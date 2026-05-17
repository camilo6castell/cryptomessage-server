<div align="center">

<img src="https://img.shields.io/badge/CryptoMessage-Backend-0d1117?style=for-the-badge&logo=lock&logoColor=white" alt="CryptoMessage" height="60"/>

# CryptoMessage — Backend

**End-to-end encrypted messaging. Your keys. Your conversations. Your freedom.**

[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-spring-security)
[![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white)](https://mariadb.org/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![BouncyCastle](https://img.shields.io/badge/BouncyCastle-1.79-yellow?style=flat-square)](https://www.bouncycastle.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

</div>

---

## Table of Contents

- [Overview](#-overview)
- [Philosophy](#-philosophy)
- [Architecture](#-architecture)
- [Encryption Model](#-encryption-model)
- [API Reference](#-api-reference)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [Security Design](#-security-design)
- [Data Retention Policy](#-data-retention-policy)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

---

## 📡 Overview

CryptoMessage is a **privacy-first, end-to-end encrypted messaging platform** built on the principle that private communication is a human right — not a feature. The backend exposes a stateless REST API that orchestrates secure message exchange between users, where **all encryption and decryption happens exclusively on the client**. The server is a relay: it stores ciphertext it cannot read.

This repository contains the **server-side** of the CryptoMessage ecosystem. The React/TypeScript frontend lives in a [separate repository](#).

> **Key architectural decision:** The server has zero involvement in cryptographic operations. RSA key generation, message encryption, and message decryption all happen in the browser. The backend stores, routes, and governs access — nothing more.

---

## 🧠 Philosophy

> *"Privacy is not about hiding something. It's about having the power to choose what to share and with whom."*

CryptoMessage is built on three principles:

**Sovereignty** — Users generate their own RSA key pairs on the client. The private key never travels to the server in plaintext. The passphrase used to encrypt it never leaves the device.

**Minimal trust** — The architecture assumes the server could be compromised. Even then, all stored messages are ciphertext blobs that are meaningless without client-held private keys.

**Transparency** — Open source, auditable, and built on well-established cryptographic primitives rather than proprietary black boxes.

---

## 🏛️ Architecture

CryptoMessage follows a **layered architecture** with strict separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Client                              │
│             (React/TypeScript — crypto lives here)             │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS
┌───────────────────────────────▼─────────────────────────────────┐
│                   Spring Security Filter Chain                  │
│         JWT Authentication Filter → CORS → Auth Rules          │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                        Controllers                              │
│   AuthController  ChatController  MessageController  Contact    │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                         Services                                │
│  AuthenticationService  MessageService  ChatService             │
│  ContactService  UserRegistrationService  JwtService            │
│  CurrentUserService  Scheduler                                  │
└──────────────┬────────────────────────────────┬─────────────────┘
               │                                │
┌──────────────▼──────────┐      ┌──────────────▼──────────────┐
│       Repositories      │      │    Domain Model / Entities   │
│  UserRepo  ChatRepo      │      │  AppUser  Chat  Message      │
│  MessageRepo ContactRepo │      │  Contact  ContentConverter   │
└──────────────┬──────────┘      └─────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│               MariaDB (dev) / MySQL via Aiven (prod)            │
│            users  chats  messages  contacts                     │
└─────────────────────────────────────────────────────────────────┘
```

The system is entirely **stateless** — no HTTP sessions are created. Authentication is handled exclusively through **JWT tokens** validated on every request.

---

## 🔐 Encryption Model

All cryptographic operations are performed **client-side**. The backend is cryptographically agnostic: it receives, stores, and returns opaque ciphertext without ever having access to keys or plaintext.

### Key Storage

Each user owns an RSA key pair generated entirely in the browser:

| Field | Where stored | Who can read |
|---|---|---|
| `publicKey` | Server DB (plaintext) | Anyone — used by senders |
| `encryptedPrivateKey` | Server DB (ciphertext) | Only the user, via their passphrase |
| `passphraseHash` | Server DB (bcrypt) | Nobody — hash only, used for login |

The private key is **encrypted on the client** before being sent. The server stores a blob it cannot decrypt.

### Message Encryption Flow

Messages are stored as **per-recipient ciphertext**. Each message row contains a `Map<userId, encryptedContent>` serialized to JSON via a JPA `AttributeConverter`:

```java
// ContentByUserConverter.java — transparent JPA serialization
@Converter
public class ContentByUserConverter
        implements AttributeConverter<Map<Long, String>, String> {
    // Map<userId, base64Ciphertext> ↔ JSON TEXT column
}
```

**End-to-end flow:**

```
Sender (browser)              Server                Recipient (browser)
       │                         │                          │
       │  1. Fetch pub keys ────►│                          │
       │◄─ pubKey_A, pubKey_B ── │                          │
       │                         │                          │
       │  2. encrypt(msg, pubA)  │                          │
       │     encrypt(msg, pubB)  │                          │
       │  ──────────────────────►│  store {A: ct_A, B: ct_B}│
       │                         │                          │
       │                         │◄── GET /messages ────────│
       │                         │──── {userId: ct_B} ─────►│
       │                         │                          │  3. decrypt(ct_B, privB)
```

**Domain enforcement at the entity level:**

```java
// Message.java — domain constructor enforces participant coverage
public Message(Chat chat, AppUser sender, Map<Long, String> encryptedContentByUser) {
    // Verifies ciphertext exists for EVERY participant before persisting
    Set<Long> participantIds = chat.getParticipantIds();
    if (!encryptedContentByUser.keySet().equals(participantIds)) {
        throw new IllegalArgumentException(
            "Encrypted content must exist for both chat participants"
        );
    }
    this.contentByUser = Map.copyOf(encryptedContentByUser);
}

// Access control enforced at the domain level, not just the controller
public String getContentForUser(Long userId) {
    String content = contentByUser.get(userId);
    if (content == null) throw new IllegalArgumentException("User has no access to this message");
    return content;
}
```

### BouncyCastle Provider

BouncyCastle is registered as a JCA security provider at startup, extending the JDK's default cryptographic capabilities:

```java
@Configuration
public class CryptoConfig {
    @PostConstruct
    public void registerProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
```

---

## 📋 API Reference

All endpoints are prefixed with `/api/v1`. Interactive documentation is available via **Swagger UI** at `/swagger-ui.html`.

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | ❌ Public | Register with username, bcrypt passphrase hash, and RSA key pair |
| `POST` | `/auth/login` | ❌ Public | Authenticate and receive a JWT token |
| `GET` | `/auth/verify` | ✅ JWT | Verify current token validity |

### Contacts

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/contacts` | ✅ JWT | List all contacts |
| `POST` | `/contacts` | ✅ JWT | Add a contact by username |
| `DELETE` | `/contacts` | ✅ JWT | Remove a contact |
| `GET` | `/contacts/search` | ✅ JWT | Search users by username |

### Chats

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/chats` | ✅ JWT | List all chats for the current user |
| `POST` | `/chats` | ✅ JWT | Create a new chat (starts in `PENDING` state) |
| `PATCH` | `/chats/{id}/accept` | ✅ JWT | Accept a pending chat request |

### Messages

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/messages/{chatId}` | ✅ JWT | Retrieve messages for a chat (returns per-user ciphertext slice) |
| `POST` | `/messages` | ✅ JWT | Send an encrypted message (`Map<userId, ciphertext>`) |
| `PATCH` | `/messages/{chatId}/read` | ✅ JWT | Mark messages as read |

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.2 |
| Security | Spring Security + JWT (jjwt) | 0.12.5 |
| Cryptography | BouncyCastle (`bcprov` + `bcpkix`) | 1.78.1 / 1.79 |
| Persistence | Spring Data JPA + Hibernate | — |
| Database (dev) | MariaDB | 3.4.1 driver |
| Database (prod) | MySQL via Aiven | — |
| Build Tool | Gradle | 8.5 |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| Containerization | Docker (multi-stage build) | — |
| Testing | JUnit 5 + Spring Security Test | — |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- MariaDB (dev) or MySQL instance (prod)

### Option 1 — Docker (recommended)

```bash
# Clone the repository
git clone https://github.com/your-username/cryptomessage-backend.git
cd cryptomessage-backend

# Build the image (multi-stage: Gradle build + JRE runtime)
docker build -t cryptomessage-backend .

# Run with required environment variables
docker run -p 8080:8080 \
  -e JWT_SK=<your-base64-secret> \
  -e DB_URL=jdbc:mysql://<host>:3306/<db> \
  -e DB_USER=<user> \
  -e DB_PASSWORD=<password> \
  cryptomessage-backend
```

### Option 2 — Gradle (local dev)

```bash
git clone https://github.com/your-username/cryptomessage-backend.git
cd cryptomessage-backend

# Run with dev profile (uses DB_URL_DEV, DB_USER_DEV, DB_PASSWORD_DEV)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Generate a JWT secret

```bash
# 256-bit base64-encoded secret
openssl rand -base64 32
```

---

## ⚙️ Configuration

The application supports two Spring profiles (`dev` / `prod`) via separate property files.

### `application.properties` (base — shared)

```properties
jwt.secret-key=${JWT_SK}
jwt.expiration-ms=86400000        # 24 hours

spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=update
spring.jpa.generate-ddl=true
```

### `application-dev.properties`

```properties
# MariaDB — local
spring.datasource.url=${DB_URL_DEV}
spring.datasource.username=${DB_USER_DEV}
spring.datasource.password=${DB_PASSWORD_DEV}
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

### `application-prod.properties`

```properties
# MySQL — Aiven (cloud)
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### Environment variables reference

| Variable | Profile | Description |
|---|---|---|
| `JWT_SK` | Both | Base64-encoded HMAC-SHA256 secret |
| `DB_URL_DEV` | dev | MariaDB JDBC URL |
| `DB_USER_DEV` | dev | MariaDB username |
| `DB_PASSWORD_DEV` | dev | MariaDB password |
| `DB_URL` | prod | MySQL (Aiven) JDBC URL |
| `DB_USER` | prod | MySQL username |
| `DB_PASSWORD` | prod | MySQL password |

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/cryptomessage/server/
│   │   ├── config/
│   │   │   ├── crypto/               # BouncyCastle JCA provider registration
│   │   │   ├── exceptions/           # ConflictException, ForbiddenException
│   │   │   ├── security/             # JwtAuthenticationFilter, CorsConfig, Session
│   │   │   └── SecurityConfig.java   # Security filter chain, endpoint rules
│   │   ├── controller/               # REST layer (thin — delegates to services)
│   │   │   ├── AuthenticationController.java
│   │   │   ├── ChatController.java
│   │   │   ├── ContactController.java
│   │   │   ├── MessageController.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/
│   │   │   ├── dto/                  # Request/Response DTOs per domain
│   │   │   │   ├── security/         # AuthenticationRequest, RegisterRequest, UserResponse
│   │   │   │   ├── chat/             # ChatResponse, ChatParticipantResponse, CreateChatRequest
│   │   │   │   ├── contact/          # AddContactRequest, ContactResponse, ...
│   │   │   │   └── message/          # SendMessageRequest, MessageResponse
│   │   │   ├── entity/               # JPA entities
│   │   │   │   ├── user/AppUser.java
│   │   │   │   ├── chat/Chat.java + ChatStatus.java
│   │   │   │   ├── message/Message.java + ContentByUserConverter.java
│   │   │   │   └── contact/Contact.java + ContactId.java
│   │   │   └── mapper/               # DTO ↔ Entity mapping
│   │   ├── repositories/             # Spring Data JPA repositories
│   │   ├── services/
│   │   │   ├── AuthenticationService.java
│   │   │   ├── ChatService.java
│   │   │   ├── ContactService.java
│   │   │   ├── CurrentUserService.java   # SecurityContext → AppUser helper
│   │   │   ├── JwtService.java
│   │   │   ├── MessageService.java
│   │   │   ├── Scheduler.java            # Automated data retention jobs
│   │   │   ├── UserDetailsServiceImp.java
│   │   │   └── UserRegistrationService.java
│   │   ├── SwaggerConfig.java
│   │   └── ServerApplication.java
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       └── application-prod.properties
└── test/
    └── java/com/cryptomessage/server/
        └── services/
            └── AuthenticationServiceTest.java
```

---

## 🔒 Security Design

### Authentication flow

```
1. POST /auth/login
   └─► Spring AuthenticationManager validates credentials
   └─► BCrypt comparison against stored passphraseHash
   └─► JWT generated (HMAC-SHA256, 24h expiry)

2. Subsequent requests
   └─► JwtAuthenticationFilter extracts & validates token
   └─► SecurityContextHolder populated
   └─► CurrentUserService.getCurrentUser() resolves AppUser
```

### Chat consent model

Chats follow a **two-step initiation** model to prevent unsolicited message delivery:

- A new chat starts in `PENDING` state.
- Only the initiator can send the **first message** while the chat is pending.
- The recipient must explicitly `ACCEPT` the chat before replying.
- This prevents spam and ensures recipient consent before any exchange.

### Domain-level access control

Authorization is enforced at the **entity level**, not just the controller layer:

```java
// Message.java
public String getContentForUser(Long userId) {
    String content = contentByUser.get(userId);
    if (content == null) {
        throw new IllegalArgumentException("User has no access to this message");
    }
    return content;
}
```

### What the server can and cannot do

| Capability | Server |
|---|---|
| Read message plaintext | ❌ Never — only ciphertext is stored |
| Know who communicated with whom | ✅ Yes (metadata) |
| Access user private keys | ❌ Never — stored encrypted, passphrase never sent |
| Verify user identity | ✅ Yes (bcrypt + JWT) |
| Modify stored ciphertext | ✅ Theoretically — but detectable client-side |

---

## 🗑️ Data Retention Policy

The server runs a scheduled cleanup job (`Scheduler.java`) every night at 03:00 to minimize data exposure:

| Job | Schedule | Retention |
|---|---|---|
| Delete old messages | `0 0 3 * * *` | Older than **15 days** |
| Delete empty chats | `0 15 3 * * *` | Older than **30 days** |
| Delete inactive users | `0 30 3 * * *` | Inactive for **45 days** |

This limits the blast radius of a potential server compromise and keeps the database lean.

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport
```

The test suite covers the authentication service layer including JWT generation, token validation, credential verification, and key lifecycle scenarios.

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

*Built with the conviction that private communication should be a default, not a luxury.*

</div>
