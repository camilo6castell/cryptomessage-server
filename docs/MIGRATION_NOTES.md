# DDD + Event Sourcing + Hexagonal — Migration Notes

This document explains every non-obvious decision made while introducing these
patterns into cryptomessage, specifically to keep the **existing frontend
working unmodified**. Read this before extending the pattern to `AppUser` or
`Contact`, or before an interview where you need to defend these choices.

## What changed, in one sentence

`Chat` is now event-sourced (write side); the existing `chats`/`messages`/
`contacts` tables became a CQRS read-model projection, kept in sync
**synchronously, in the same transaction** as every write. No endpoint, no
request/response shape, and no WebSocket destination changed.

## What did NOT change

- `AppUser`, authentication, JWT, `SecurityConfig`, `WebSocketConfig` — untouched.
  User registration/profile has no audit-history value that would justify ES.
- `Contact` — stays a plain JPA-managed side effect of `ChatAccepted`, not its
  own event-sourced aggregate. It's a derived fact ("this chat was accepted"),
  not an independent business concept with its own lifecycle.
- Every DTO, every REST path, every WebSocket destination
  (`/user/queue/messages`, `/user/queue/chats`).

## Key compatibility decisions

1. **`ChatId` is a Long, not a UUID.** The frontend's `ChatResponse#chatId` and
   every path variable are typed as `Long`. A "purist" aggregate mints its own
   UUID; this one gets a pre-minted `Long` from `ChatIdGenerator` (backed by a
   tiny `chat_id_sequences` table) so the event-sourced aggregate's identity and
   the read-model row's primary key are the exact same value.

2. **`ChatStatus` stays in `model.entity.chat`, reused by the domain layer.**
   Duplicating a two-value enum across layers just to satisfy a "purist"
   hexagonal boundary would add a mapping step with no real benefit — and a
   second place for it to drift out of sync.

3. **Projection runs synchronously, same transaction as the event append.**
   This app has no outbox/message queue. Eventual consistency between "message
   sent" and "recipient's chat list updates" would be a user-visible
   regression in a chat app. If throughput ever demands it, introduce an
   outbox + async projection then — not preemptively.

4. **Retention scheduler purges the event store too.** Pure Event Sourcing says
   "never delete." This app deletes old messages/empty chats/inactive users by
   design (privacy). The scheduler now purges the matching `events` rows in the
   same jobs, so "deleted" data doesn't quietly survive forever in the event
   log. See `Scheduler.java`.

5. **Domain exceptions map to the exact same HTTP status codes as before.**
   `GlobalExceptionHandler` gained new `@ExceptionHandler`s for the Chat
   aggregate's exception types, each documented with which original
   service-layer exception it replaces and why it maps to that status.

## A bug fixed while porting the generic ES framework

The generic framework (`AggregateRoot`, `ChangeEventSubscriber`, ...) is
adapted from the Library Provider project. That version never re-synced its
in-memory version counter when an aggregate was reconstructed from history —
replaying events never touched it, so a reconstructed aggregate's next
`appendEvent()` would compute a version that could collide with real history.
Here, **the persistence adapter** assigns event versions (based on
`COUNT(*) events WHERE aggregate_id = ?`), not the aggregate itself. Combined
with a unique constraint on `(aggregate_id, version)`, this is what makes
optimistic concurrency control actually work — see
`ChatAggregateRepositoryAdapter`.

## Required manual step before deploying

`application-prod.properties` uses `spring.jpa.hibernate.ddl-auto=validate` —
Hibernate won't create the two new tables (`events`, `chat_id_sequences`) or
apply the `chats.chat_id` column change in production. Run
**`docs/schema-additions.sql`** against the prod database first. Dev
(`ddl-auto=update`) gets all of this for free.

## Architecture enforced by tests, not by the compiler

Package-based Hexagonal (as opposed to Library Provider's multi-module Gradle)
means nothing stops someone from writing `import ...infrastructure...;` inside
`domain.chat.Chat` and having it compile fine. `src/test/java/.../architecture/`
now enforces the same boundaries with **ArchUnit**, run as a normal part of
`./gradlew test`:

- `DomainIndependenceTest` — domain depends on nothing but the JDK, plus the
  one documented exception (`ChatStatus`).
- `ApplicationLayerTest` — use cases depend on domain **ports**, never on the
  concrete adapters that implement them (except the CQRS read/projection side
  — deliberately allowed, see the class javadoc for why).
- `MigratedControllersTest` — scoped only to `ChatController`/`MessageController`
  (the two this migration touched); doesn't make claims about the untouched
  auth/contact controllers.
- `GeneralCodeQualityTest` — constructor injection only (no `@Autowired`
  fields), no package cycles inside `domain.chat`.

If you extend this pattern to `AppUser`/`Contact` later, these tests are what
will catch it if the boundary gets blurred by accident.

## About `ddl-auto`

`docs/schema-additions.sql` assumes `ddl-auto=validate` in prod (manual schema
management, matching what was already there). Since there's no production
data yet, temporarily switching prod to `ddl-auto=update` to have Hibernate
create everything (including the two new tables) is a reasonable shortcut —
just remember to switch it back to `validate` (or introduce a real migration
tool like Flyway) once there's real data you can't afford to have Hibernate
guess about.

## Deliberately left for a future step

- `AppUser` and `Contact` were intentionally not converted — see point 2 in
  "What did NOT change" and the earlier discussion about which aggregates
  actually benefit from Event Sourcing.
- No snapshotting. Not needed yet — `Chat`'s write-side state is already
  minimal (a few fields, not the full message history), so replay stays cheap
  regardless of message volume. Revisit only if a single chat's event count
  (not message count — `ChatCreated`/`ChatAccepted`/`ChatRead` too) grows large
  enough to matter.
- No async projection / outbox. See point 3 above.
