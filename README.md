# CryptoMessage — Server

**Backend for a privacy-first, end-to-end encrypted messaging application.**

Built with Java 17 and Spring Boot 3, this server is intentionally *message-blind*: it stores, routes, and expires data, but never has access to plaintext content. All cryptographic operations happen exclusively on the client.

---

## Philosophy

CryptoMessage is designed around a simple premise: **the server should be the least trusted party in the system.**

In most messaging platforms, the operator is an implicit third party — they can read your messages, retain them indefinitely, and comply with third-party data requests. CryptoMessage takes the opposite stance. The server is a dumb relay: it holds ciphertext it cannot decrypt, and it forgets data on a fixed schedule regardless of user action.

This puts the responsibility — and the control — back in the hands of the user. Your privacy depends on your passphrase and your device, not on trusting this server.

---

## How Encryption Works

Encryption is handled entirely on the client (frontend). The server never sees a plaintext message, passphrase, or private key.

```
┌─────────────────────────────────────────────────────────────────┐
│  CLIENT                                                         │
│                                                                 │
│  1. Key generation (RSA-OAEP + AES-GCM)                        │
│     ├── Public key  ──────────────────────────────► stored on server (plaintext) │
│     └── Private key ──► encrypted with passphrase ► stored on server (ciphertext) │
│                                                                 │
│  2. Sending a message                                           │
│     └── content encrypted once per participant                  │
│         ├── { userId_A: encrypt(content, pubKey_A) }           │
│         └── { userId_B: encrypt(content, pubKey_B) }           │
│                                          │                      │
└──────────────────────────────────────────┼──────────────────────┘
                                           ▼
┌─────────────────────────────────────────────────────────────────┐
│  SERVER                                                         │
│                                                                 │
│  Receives: Map<userId, encryptedContent>                        │
│  Stores:   opaque ciphertext blobs, routed by userId            │
│  Returns:  the blob addressed to the requesting user            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Each participant receives only their own encrypted copy. The server cannot reconstruct the original message even if it tried — it holds two unrelated ciphertexts.

> **Note on design evolution:** The first version of this project performed RSA and AES encryption on the server side. As the architecture matured, it became clear that server-side encryption fundamentally contradicts a zero-trust model — the server knowing the plaintext even momentarily defeats the purpose. Encryption was migrated entirely to the client, reducing the server's role to authenticated storage and delivery of opaque data.

---

## Data Retention

A scheduler runs nightly (03:00 server time) and permanently deletes data on a fixed schedule. There is no opt-out and no manual override — this is by design.

| Data              | Deleted after   |
|-------------------|-----------------|
| Messages          | 15 days         |
| Empty chats       | 30 days         |
| Inactive accounts | 45 days of inactivity |

Deletion is hard — no soft-delete, no archive, no audit log. Once the window passes, the data is gone.

---

## Tech Stack

| Layer         | Technology                                      |
|---------------|-------------------------------------------------|
| Language      | Java 17                                         |
| Framework     | Spring Boot 3.3.2                               |
| Security      | Spring Security · JWT (JJWT 0.12.5)            |
| Persistence   | Spring Data JPA · Hibernate                     |
| Database      | MariaDB (dev) · MySQL (prod)                    |
| API Docs      | SpringDoc OpenAPI (Swagger UI)                  |
| Build         | Gradle 8.14.4                                   |
| Container     | Docker (eclipse-temurin:17-jdk)                 |

---

## Project Structure

```
src/main/java/com/cryptomessage/server/
│
├── controller/          # REST endpoints + GlobalExceptionHandler
├── services/            # Business logic, JWT, scheduler
├── repositories/        # Spring Data JPA interfaces
├── model/
│   ├── entity/          # JPA entities (AppUser, Chat, Message, Contact)
│   ├── dto/             # Request/response records
│   └── mapper/          # Entity → DTO mapping
└── config/
    ├── security/        # JWT filter, CORS, AuthConfig
    └── exceptions/      # Custom exception types
```

---

## API Reference

All endpoints (except auth) require a `Bearer` token in the `Authorization` header.

### Authentication — `/api/v1/auth`

| Method | Endpoint    | Description                              | Auth |
|--------|-------------|------------------------------------------|------|
| POST   | `/register` | Create account (sends public key + encrypted private key) | ✗ |
| POST   | `/login`    | Authenticate, receive JWT + key material | ✗ |
| GET    | `/verify`   | Validate token, refresh user data        | ✓ |

**Register / Login payload note:** The client generates the key pair locally before registering. The server stores the public key in plaintext and the private key already encrypted by the client — it never touches the raw private key.

### Chats — `/api/v1/chats`

| Method | Endpoint           | Description                                              |
|--------|--------------------|----------------------------------------------------------|
| POST   | `/`                | Initiate a chat with another user                        |
| GET    | `/`                | List your chats (optional `?status=PENDING\|ACCEPTED`)   |
| POST   | `/{chatId}/accept` | Accept a pending chat (creates contacts bidirectionally) |

Chats start as `PENDING`. The initiator can send one message before the other party accepts. This prevents unsolicited message spam while allowing a first contact.

### Messages — `/api/v1/messages`

| Method | Endpoint                  | Description                                 |
|--------|---------------------------|---------------------------------------------|
| POST   | `/`                       | Send an encrypted message                   |
| GET    | `/chat/{chatId}`          | Retrieve messages for a chat                |
| PATCH  | `/chat/{chatId}/read`     | Mark all messages in a chat as read         |

The request body for sending a message:
```json
{
  "chatId": 1,
  "encryptedContentByUser": {
    "101": "<ciphertext for user 101>",
    "102": "<ciphertext for user 102>"
  }
}
```
Both participants must be present as keys. The server validates this and rejects incomplete payloads.

### Contacts — `/api/v1/contacts`

| Method | Endpoint          | Description                              |
|--------|-------------------|------------------------------------------|
| POST   | `/search`         | Look up a user by username               |
| GET    | `/`               | List your contacts                       |
| POST   | `/`               | Add a contact (requires accepted chat)   |
| DELETE | `/{contactId}`    | Remove a contact                         |

### Realtime — `/ws` (STOMP over WebSocket)

New messages and chat status changes are pushed live instead of requiring a reload.

- **Endpoint:** `ws(s)://<host>/ws` — raw WebSocket (no SockJS fallback).
- **Auth:** the handshake itself is anonymous; the STOMP `CONNECT` frame must carry a
  native `Authorization: Bearer <jwt>` header. A missing or invalid token closes the
  connection before any subscription is accepted.
- **Subscriptions** (per-user, via Spring's `/user` prefix):

  | Destination            | Payload         | Fired on                          |
  |-------------------------|-----------------|------------------------------------|
  | `/user/queue/messages`  | `MessageResponse` | A message was sent in one of your chats (echoed to your own other sessions too) |
  | `/user/queue/chats`     | `ChatResponse`    | A chat was created (to the recipient) or accepted (to both participants) |

  Both payloads are the same DTOs the REST endpoints already return — the server relays
  the same opaque ciphertext it stores, nothing more is decrypted or exposed to make
  this work.

---

## Running Locally

### Prerequisites

- Java 17+
- MariaDB running locally
- A base64-encoded 256-bit secret for JWT

### Environment Variables

```bash
# Database (dev profile)
DB_URL_DEV=jdbc:mariadb://localhost:3306/cryptomessage
DB_USER_DEV=your_user
DB_PASSWORD_DEV=your_password

# JWT
JWT_SK=<base64-encoded 256-bit secret>
# Generate one with: openssl rand -base64 32
```

### Start

```bash
# Activate the dev profile (uses MariaDB, enables show-sql, ddl-auto=update)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Swagger UI will be available at `http://localhost:8080/swagger-ui/index.html`.

---

## Docker

```bash
docker build -t cryptomessage-server .

docker run -p 8080:8080 \
  -e JWT_SK=<your-secret> \
  -e DB_URL=<jdbc-url> \
  -e DB_USER=<user> \
  -e DB_PASSWORD=<password> \
  cryptomessage-server
```

The production profile is active by default in the container (MySQL driver, `ddl-auto=validate`, `show-sql=false`).

---

## Security Notes

- **Passwords** are hashed with BCrypt before storage. The server never stores or transmits plaintext passphrases.
- **Private keys** reach the server already encrypted by the client (AES-GCM, key derived from the user's passphrase). The server stores them to allow multi-device login — the client re-derives the decryption key locally.
- **JWT** is stateless (HMAC-SHA256). Token expiration is set via `jwt.expiration-ms` (default: 24 hours).
- **CORS** origins are configured via the `CORS_ALLOWED_ORIGINS` environment variable. Falls back to wildcard in dev.
- **Message validation** enforces that every outgoing message includes a ciphertext entry for each chat participant, preventing partial or malformed payloads from reaching the database.

---

## License

MIT
