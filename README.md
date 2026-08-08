<div align="center">

<img src="https://img.shields.io/badge/CryptoMessage-Server-0d1117?style=for-the-badge&logo=springboot&logoColor=white" alt="CryptoMessage" height="60"/>

# CryptoMessage — Server

**End-to-end encrypted messaging. The server never sees a plaintext message, passphrase, or private key.**

[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate-6DB33F?style=flat-square&logo=hibernate&logoColor=white)](https://spring.io/projects/spring-data-jpa)
[![WebSocket](https://img.shields.io/badge/STOMP_over_WebSocket-Realtime-2ea44f?style=flat-square)](https://docs.spring.io/spring-framework/reference/web/websocket.html)
[![Gradle](https://img.shields.io/badge/Gradle_8.14-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

</div>

---

## Table of Contents

- [Overview](#-overview)
- [Philosophy](#-philosophy)
- [How Encryption Works](#-how-encryption-works)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Domain Model](#-domain-model)
- [API Reference](#-api-reference)
- [Realtime](#-realtime)
- [Security](#-security)
- [Data Retention](#-data-retention)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Docker](#-docker)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

---

## 📡 Overview

CryptoMessage Server is the backend for a **privacy-first, end-to-end encrypted messaging application**. Built with Java 17 and Spring Boot 3, it is intentionally *message-blind*: it authenticates, stores, routes, and expires data, but it never has access to plaintext content. All cryptographic operations — key generation, message encryption, message decryption — happen exclusively on the client, using the browser's native Web Crypto API.

This repository contains the **backend** of the CryptoMessage ecosystem. The React/TypeScript frontend lives in a [separate repository](#).

---

## 🔒 Philosophy

CryptoMessage is designed around a simple premise: **the server should be the least trusted party in the system.**

In most messaging platforms, the operator is an implicit third party — they can read messages, retain them indefinitely, and comply with third-party data requests. CryptoMessage takes the opposite stance. The server is a dumb relay: it holds ciphertext it cannot decrypt, and it forgets data on a fixed schedule regardless of user action.

This puts the responsibility — and the control — back in the hands of the user. Privacy depends on the user's passphrase and device, not on trusting this server.

> **Note on design evolution:** the first version of this project performed RSA and AES encryption on the server side. As the architecture matured, it became clear that server-side encryption fundamentally contradicts a zero-trust model — the server knowing the plaintext even momentarily defeats the purpose. Encryption was migrated entirely to the client, reducing the server's role to authenticated storage and delivery of opaque data.

---

## 🔐 How Encryption Works

Encryption is handled entirely on the client. The server's role starts *after* content is already ciphertext.

```
┌──────────────────────────────────────────────────────────────────────┐
│  CLIENT                                                              │
│                                                                       │
│  1. Key generation (RSA-OAEP 2048 + AES-GCM)                         │
│     ├── Public key  ─────────────────────────► stored on server (plaintext)  │
│     └── Private key ─► encrypted with passphrase ► stored on server (ciphertext) │
│                                                                       │
│  2. Sending a message                                                │
│     └── content encrypted once per participant                      │
│         ├── { userId_A: encrypt(content, pubKey_A) }                 │
│         └── { userId_B: encrypt(content, pubKey_B) }                 │
│                                          │                            │
└──────────────────────────────────────────┼────────────────────────────┘
                                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│  SERVER                                                               │
│                                                                        │
│  Receives: Map<userId, encryptedContent>                              │
│  Stores:   opaque ciphertext blobs, routed by userId                  │
│  Returns:  the blob addressed to the requesting user                  │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

Each participant receives only their own encrypted copy. The server cannot reconstruct the original message even if it tried — it holds two unrelated ciphertexts, and nothing on this side of the wire ever calls a decrypt function.

See the frontend README's "Cryptography Architecture" section for the client-side primitives (RSA-OAEP, AES-GCM, PBKDF2) that produce these blobs.

---

## 🛠️ Tech Stack

| Layer         | Technology                              | Version                  |
|---------------|------------------------------------------|---------------------------|
| Language      | Java                                     | 17                        |
| Framework     | Spring Boot                              | 3.3.2                     |
| Security      | Spring Security · JWT (JJWT)             | 0.12.5                    |
| Persistence   | Spring Data JPA · Hibernate              | —                         |
| Database      | MariaDB (dev) · MySQL (prod)             | —                         |
| Realtime      | Spring WebSocket · STOMP                 | —                         |
| API Docs      | SpringDoc OpenAPI (Swagger UI)           | 2.5.0                     |
| Build         | Gradle                                   | 8.14.4                    |
| Container     | Docker (`eclipse-temurin:17-jdk`)        | —                         |
| Architecture tests | ArchUnit                            | 1.5.0                     |

---

## 🏛️ Architecture

The `Chat` slice (chats + messages — the part of this app with real audit/history
value) is built on **Domain-Driven Design**, **Event Sourcing**, and
**Hexagonal Architecture**. `AppUser`, `Contact`, and authentication
deliberately stay plain layered/JPA — not everything benefits from these
patterns, and forcing them onto user profile data would be complexity with no
payoff.

- **DDD**: `domain.chat.Chat` is the aggregate root — every chat/message
  invariant (who can message whom, when, how many messages before a chat is
  accepted) lives there and nowhere else.
- **Event Sourcing**: `Chat`'s state isn't stored directly. It's derived by
  replaying its own event stream (`ChatCreated`, `MessageSent`, `ChatAccepted`,
  `ChatRead`), persisted in an append-only `events` table.
- **Hexagonal**: the domain depends only on **ports**
  (`domain.chat.port.*`) — `infrastructure.*` provides the adapters (JPA event
  store, STOMP notifications) that implement them. The domain has zero
  dependency on Spring or JPA — see `docs/MIGRATION_NOTES.md`.
- **CQRS, deliberately partial**: the existing `chats`/`messages`/`contacts`
  tables became the *read model*, kept in sync **synchronously** (same
  transaction as the event append — this app has no message queue, and
  eventual consistency in a chat's own message list would be user-visible).

This is a **package-based** implementation of Hexagonal (not multi-module
Gradle) — the boundaries above are enforced by ArchUnit tests
(`src/test/.../architecture/`) rather than by the compiler. See
`docs/MIGRATION_NOTES.md` for the full reasoning behind every compatibility
decision made while introducing this (why `ChatId` is a `Long` and not a UUID,
why the retention scheduler also purges the event store, etc.), and
**`apuntes/`** for a from-scratch, project-specific explanation of DDD, Event
Sourcing, and Hexagonal Architecture as study material.

---

## 📁 Project Structure

```
src/main/java/com/cryptomessage/server/
├── ServerApplication.java
├── controller/
│   ├── AuthenticationController.java   # /api/v1/auth        — unchanged
│   ├── ChatController.java             # /api/v1/chats       — delegates to application.chat.*
│   ├── ContactController.java          # /api/v1/contacts    — unchanged
│   ├── MessageController.java          # /api/v1/messages    — delegates to application.message.*
│   └── GlobalExceptionHandler.java     # Centralized error mapping → ApiError
│
├── domain/                             # ── Pure Java. No Spring, no JPA. ──
│   ├── generic/                        # AggregateRoot, DomainEvent, EventChange... (ES framework)
│   ├── shared/UserId.java
│   └── chat/
│       ├── Chat.java, ChatId.java, MessageId.java, ChatBehavior.java
│       ├── events/                     # ChatCreated, MessageSent, ChatAccepted, ChatRead
│       ├── exceptions/                 # Chat invariant violations
│       └── port/                       # ChatAggregateRepository, ChatIdGenerator, NotificationPort
│
├── application/                        # Use cases — orchestrate the domain, one per action
│   ├── chat/          # CreateChatUseCase, AcceptChatUseCase, GetMyChatsUseCase
│   └── message/        # SendMessageUseCase, GetMessagesByChatUseCase, MarkChatAsReadUseCase
│
├── infrastructure/                     # Adapters — implement the domain's ports
│   ├── eventstore/     # StoredEvent (JPA), EventSerializer, ChatAggregateRepositoryAdapter, JpaChatIdGenerator
│   ├── projection/      # ChatProjector — turns domain events into read-model updates
│   └── notification/    # StompNotificationAdapter
│
├── services/
│   ├── AuthenticationService.java      # Login + token verification
│   ├── UserRegistrationService.java    # Registration + key material storage
│   ├── JwtService.java                 # Token issuance / validation (HMAC-SHA256)
│   ├── CurrentUserService.java         # Resolves the authenticated AppUser
│   ├── ContactService.java             # Contact search / add / remove
│   ├── UserDetailsServiceImpl.java     # Spring Security UserDetailsService
│   └── Scheduler.java                  # Nightly data-retention jobs — also purges the event store
├── repositories/                       # Spring Data JPA interfaces — now the CQRS *read* side for Chat
│   ├── UserRepository.java
│   ├── ChatRepository.java
│   ├── MessageRepository.java
│   └── ContactRepository.java
├── model/
│   ├── entity/                         # Read-model JPA entities (see Architecture)
│   │   ├── user/AppUser.java
│   │   ├── chat/Chat.java, ChatStatus.java
│   │   ├── message/Message.java, ContentByUserConverter.java
│   │   └── contact/Contact.java, ContactId.java
│   ├── dto/                            # Request/response records, grouped by domain — unchanged
│   └── mapper/                         # Entity → DTO mapping (ChatMapper, MessageMapper...) — unchanged
└── config/
    ├── SecurityConfig.java             # Filter chain, stateless sessions, route rules
    ├── SwaggerConfig.java              # OpenAPI metadata
    ├── security/
    │   ├── AuthConfig.java             # PasswordEncoder + AuthenticationManager beans
    │   ├── CorsConfig.java             # CORS origins from env
    │   └── JwtAuthenticationFilter.java
    ├── websocket/
    │   ├── WebSocketConfig.java        # STOMP broker + endpoint registration
    │   └── StompAuthChannelInterceptor.java  # Authenticates the STOMP CONNECT frame
    └── exceptions/
        ├── ConflictException.java      # → 409
        └── ForbiddenException.java     # → 403

src/test/java/com/cryptomessage/server/
├── services/                           # AuthenticationServiceTest
├── architecture/                       # ArchUnit — enforces the layering above
└── ServerApplicationTests.java

docs/
├── MIGRATION_NOTES.md                  # Every compatibility decision, explained
└── schema-additions.sql                # events / chat_id_sequences DDL for ddl-auto=validate environments

apuntes/                                # Study material — DDD / Event Sourcing / Hexagonal, from scratch
```

Note: `ChatService.java` and `MessageService.java` no longer exist — their
responsibilities moved into `domain.chat.Chat` (the invariants) and
`application.chat` / `application.message` (the orchestration). See
`docs/MIGRATION_NOTES.md`.

---

## 🗃️ Domain Model

| Entity | Key fields | Notes |
|---|---|---|
| `AppUser` | `username` (unique), `passphraseHash` (BCrypt), `publicKey`, `encryptedPrivateKey`, `lastSeen` | `recordActivity()` bumps `lastSeen` on every authenticated action — this is what keeps the account out of the inactivity-cleanup job. Plain JPA entity, not event-sourced — see [Architecture](#-architecture). |
| `Chat` (write side — `domain.chat.Chat`) | `user1Id`, `user2Id`, `initiatedBy`, `status`, `hasPendingMessage` | Event-sourced aggregate. Holds the *minimum* state needed to validate its own invariants, not the message history — see `apuntes/` for why. |
| `Chat` (read side — `model.entity.chat.Chat`) | Same shape as before this migration | The projection `ChatController`/`MessageController` actually read from. Unique constraint on `(user1_id, user2_id)` still lives here. |
| `Message` | `chat`, `sender`, `contentByUser` (`Map<userId, ciphertext>`), `isRead` | Read-model row, written by `ChatProjector` in reaction to a `MessageSent` event. `contentByUser` is persisted via a custom `AttributeConverter` (`ContentByUserConverter`) — the column itself is opaque `TEXT`, the server never parses the ciphertext values, only the map's keys. |
| `Contact` / `ContactId` | Composite key `(ownerId, contactId)` | Created bidirectionally when `ChatProjector` reacts to `ChatAccepted`. Deliberately *not* its own event-sourced aggregate — see [Architecture](#-architecture). |

---

## 📖 API Reference

All endpoints except `/api/v1/auth/**` require a `Bearer` token in the `Authorization` header. Errors are returned as a consistent shape from `GlobalExceptionHandler`:

```json
{
  "error": "FORBIDDEN",
  "message": "You are not a participant of this chat",
  "timestamp": "2026-07-17T03:00:00Z"
}
```

### Authentication — `/api/v1/auth`

| Method | Endpoint    | Description                                                | Auth |
|--------|-------------|--------------------------------------------------------------|:----:|
| POST   | `/register` | Create account (username + passphrase + public key + client-encrypted private key) | ✗ |
| POST   | `/login`    | Authenticate, receive JWT + key material                    | ✗ |
| GET    | `/verify`   | Validate the current token, refresh user data                | ✓ |

**Register / Login payload note:** the client generates the RSA key pair locally before registering. The server stores the public key in plaintext and the private key already encrypted by the client (AES-GCM) — it never touches the raw private key or the plaintext passphrase.

### Chats — `/api/v1/chats`

| Method | Endpoint           | Description                                              |
|--------|--------------------|------------------------------------------------------------|
| POST   | `/`                | Initiate a chat with another user                          |
| GET    | `/`                | List your chats (optional `?status=PENDING\|ACCEPTED`)     |
| POST   | `/{chatId}/accept` | Accept a pending chat (creates contacts bidirectionally)   |

Chats start as `PENDING`. The initiator can send one message before the other party accepts, which prevents unsolicited message spam while still allowing a first contact.

### Messages — `/api/v1/messages`

| Method | Endpoint                  | Description                          |
|--------|----------------------------|----------------------------------------|
| POST   | `/`                        | Send an encrypted message              |
| GET    | `/chat/{chatId}`           | Retrieve messages for a chat           |
| PATCH  | `/chat/{chatId}/read`      | Mark all messages in a chat as read    |

Request body for sending a message:

```json
{
  "chatId": 1,
  "encryptedContentByUser": {
    "101": "<ciphertext for user 101>",
    "102": "<ciphertext for user 102>"
  }
}
```

Both participants must be present as keys — the server validates this and rejects incomplete payloads before persisting anything.

### Contacts — `/api/v1/contacts`

| Method | Endpoint       | Description                              |
|--------|----------------|---------------------------------------------|
| POST   | `/search`      | Look up a user by username                  |
| GET    | `/`            | List your contacts                          |
| POST   | `/`            | Add a contact (requires an accepted chat)   |
| DELETE | `/{contactId}` | Remove a contact                            |

Interactive docs are served by Swagger UI at `/swagger-ui/index.html` once the app is running.

---

## 🔌 Realtime

New messages and chat status changes are pushed live over STOMP-over-WebSocket instead of requiring a client reload.

- **Endpoint:** `ws(s)://<host>/ws` — raw WebSocket, no SockJS fallback. Modern browsers and `@stomp/stompjs` support native WebSocket directly, so the extra long-polling complexity SockJS brings isn't needed for a private app with no legacy-browser requirement.
- **Auth model:** the WebSocket *handshake* itself is anonymous (`permitAll` in `SecurityConfig`) because SockJS/raw-WS handshakes can't reliably carry custom headers from the browser. Real authentication happens on the STOMP `CONNECT` frame, intercepted by `StompAuthChannelInterceptor`, which mirrors `JwtAuthenticationFilter`'s validation so there's a single source of truth for what makes a token valid. A missing or invalid token closes the connection before any subscription is accepted.
- **Subscriptions** (per-user, via Spring's `/user` prefix):

  | Destination             | Payload            | Fired on                                                                 |
    |--------------------------|---------------------|-----------------------------------------------------------------------------|
  | `/user/queue/messages`   | `MessageResponse`  | A message was sent in one of your chats (echoed to your own other sessions too) |
  | `/user/queue/chats`      | `ChatResponse`     | A chat was created (to the recipient) or accepted (to both participants)   |

  Both payloads are the same DTOs the REST endpoints already return — the server stays message-blind here too, relaying the same opaque ciphertext it stores, nothing more is decrypted or inspected to make this work.

---

## 🛡️ Security

- **Passwords** are hashed with BCrypt before storage. The server never stores or transmits plaintext passphrases.
- **Private keys** reach the server already encrypted by the client (AES-GCM, key derived from the user's passphrase). The server stores them to allow multi-device login — the client re-derives the decryption key locally.
- **JWT** is stateless (HMAC-SHA256, `SessionCreationPolicy.STATELESS`). Token expiration is set via `jwt.expiration-ms` (default: 24 hours).
- **CORS** origins are configured via the `CORS_ALLOWED_ORIGINS` environment variable (comma-separated for multiple origins); falls back to a wildcard pattern in dev.
- **Message validation** enforces that every outgoing message includes a ciphertext entry for each chat participant, preventing partial or malformed payloads from reaching the database.
- **Route rules:** only `/api/v1/auth/**` and the WebSocket handshake at `/ws/**` are public; everything else requires a valid bearer token.

---

## 🧹 Data Retention

A scheduler runs nightly (starting 03:00 server time) and permanently deletes data on a fixed schedule. There is no opt-out and no manual override — this is by design.

| Job                     | Data               | Deleted after           | Cron              |
|--------------------------|--------------------|----------------------------|--------------------|
| `deleteOldMessages`      | Messages (+ their `MessageSent` events) | 15 days | `0 0 3 * * *`      |
| `deleteEmptyChats`       | Empty chats (+ their full event stream) | 30 days | `0 15 3 * * *`     |
| `deleteInactiveUsers`    | Inactive accounts   | 45 days without `recordActivity()` | `0 30 3 * * *` |

Deletion is hard — no soft-delete, no archive, no audit log. Once the window
passes, the data is gone — from the read-model tables **and** from the event
store. Event Sourcing's usual "never delete anything" default would otherwise
quietly undermine this policy; see `docs/MIGRATION_NOTES.md`.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- MariaDB running locally
- A base64-encoded 256-bit secret for JWT

### Environment Variables

Set the following (e.g. via a `.env` file loaded by your IDE, or exported in your shell):

```bash
# Database (dev profile)
DB_URL_DEV=jdbc:mariadb://localhost:3306/cryptomessage
DB_USER_DEV=your_user
DB_PASSWORD_DEV=your_password

# JWT
JWT_SK=<base64-encoded 256-bit secret>
# Generate one with: openssl rand -base64 32

# Optional — comma-separated list; falls back to wildcard in dev if unset
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Run

```bash
# Activate the dev profile (MariaDB, show-sql enabled, ddl-auto=update)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Swagger UI will be available at `http://localhost:8080/swagger-ui/index.html`.

| Variable | Profile | Description |
|---|---|---|
| `DB_URL_DEV` / `DB_USER_DEV` / `DB_PASSWORD_DEV` | dev | MariaDB connection |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | prod | MySQL connection |
| `JWT_SK` | both | Base64-encoded 256-bit HMAC secret |
| `CORS_ALLOWED_ORIGINS` | both | Comma-separated allowed origins for REST + WebSocket |

---

## 🐳 Docker

```bash
docker build -t cryptomessage-server .

docker run -p 8080:8080 \
  -e JWT_SK=<your-secret> \
  -e DB_URL=<jdbc-url> \
  -e DB_USER=<user> \
  -e DB_PASSWORD=<password> \
  -e CORS_ALLOWED_ORIGINS=https://your-frontend.example.com \
  cryptomessage-server
```

The production profile is active by default in the container image (MySQL driver, `ddl-auto=validate`, `show-sql=false`). The `Dockerfile` uses a two-stage build — `gradle:8.14.4-jdk17` to compile, `eclipse-temurin:17-jdk` to run — so the shipped image doesn't carry the Gradle toolchain.

> **New tables since the DDD/Event Sourcing migration:** `events` and
> `chat_id_sequences`. With `ddl-auto=validate`, run `docs/schema-additions.sql`
> against the prod database once, manually. (If you're bootstrapping a fresh
> environment with no data yet, temporarily setting `ddl-auto=update` to let
> Hibernate create everything works too — just switch back to `validate`, or
> introduce a real migration tool, once there's real data to protect.)

---

## ✅ Testing

```bash
./gradlew test
```

JUnit 5 is wired in via `spring-boot-starter-test` and `spring-security-test`. Test coverage currently focuses on the authentication and chat-creation service logic — see `src/test/java/com/cryptomessage/server/services/`.

`src/test/java/com/cryptomessage/server/architecture/` additionally runs as
part of the same `./gradlew test` — **ArchUnit** rules that enforce the
domain/application/infrastructure layering described in
[Architecture](#-architecture) at build time, not just by convention.

---

## 🤝 Contributing

Contributions are welcome. Please open an issue before submitting a pull request to discuss the proposed change.

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/): `git commit -m 'feat: add your feature'`
4. Push and open a pull request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](./LICENSE) file for details.

Copyright © 2025 Camilo Andres Castellanos Herrera

---

<div align="center">

*The server holds ciphertext it cannot read.*

</div>