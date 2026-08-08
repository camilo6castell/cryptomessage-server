# Mapa del proyecto: qué archivo es qué concepto

Antes de entrar en teoría, ubícate en el terreno. Esta es la traducción
directa entre "el nombre técnico del patrón" y "el archivo real que lo
implementa" en cryptomessage.

## El árbol completo, anotado

```
domain/                              ← "el dominio" en Hexagonal. Cero Spring, cero JPA.
├── generic/                         ← el "motor" de Event Sourcing, reutilizable para
│                                       cualquier aggregate futuro (no solo Chat)
│   ├── AggregateRoot.java           ← clase base de todo aggregate root (DDD + ES)
│   ├── DomainEvent.java             ← clase base de todo evento (ES)
│   ├── EventChange.java             ← registra "cuando pase X evento, haz Y" (ES)
│   ├── ChangeEventSubscriber.java   ← guarda eventos no confirmados + los aplica (ES)
│   ├── Entity.java                  ← clase base de toda entidad con identidad (DDD)
│   └── IValueObject.java            ← marca qué es un value object (DDD)
│
├── shared/
│   └── UserId.java                  ← value object (DDD) — referencia a un AppUser
│
└── chat/
    ├── Chat.java                    ← EL AGGREGATE ROOT (DDD). También es quien
    │                                   emite y aplica eventos (ES)
    ├── ChatBehavior.java            ← "qué le pasa al estado de Chat cuando llega
    │                                   cada tipo de evento" (ES)
    ├── ChatId.java, MessageId.java  ← identidades tipadas (DDD)
    ├── events/                      ← los HECHOS que le pueden pasar a un Chat (ES)
    │   ├── ChatCreated.java
    │   ├── MessageSent.java
    │   ├── ChatAccepted.java
    │   └── ChatRead.java
    ├── exceptions/                  ← invariantes violados (DDD) — no HTTP, eso es
    │                                   responsabilidad de otra capa (Hexagonal)
    └── port/                        ← LOS PUERTOS (Hexagonal) — interfaces que el
        ├── ChatAggregateRepository.java   dominio necesita, sin saber quién las implementa
        ├── ChatIdGenerator.java
        └── NotificationPort.java

application/                         ← "casos de uso" — orquestan el dominio,
│                                       no contienen reglas de negocio ellos mismos
├── chat/
│   ├── CreateChatUseCase.java
│   ├── AcceptChatUseCase.java
│   └── GetMyChatsUseCase.java
└── message/
    ├── SendMessageUseCase.java
    ├── GetMessagesByChatUseCase.java
    └── MarkChatAsReadUseCase.java

infrastructure/                      ← LOS ADAPTADORES (Hexagonal) — implementan
│                                       los puertos de domain/chat/port/
├── eventstore/                      ← el Event Store real (ES), sobre JPA/MySQL
│   ├── StoredEvent.java             ← la fila de la tabla `events`
│   ├── ChatAggregateRepositoryAdapter.java  ← implementa ChatAggregateRepository
│   ├── JpaChatIdGenerator.java      ← implementa ChatIdGenerator
│   └── EventSerializer.java         ← evento de dominio ↔ JSON
├── projection/
│   └── ChatProjector.java           ← EL LADO "Q" DE CQRS (ES) — evento → tablas
│                                       de lectura existentes (chats, messages, contacts)
└── notification/
    └── StompNotificationAdapter.java  ← implementa NotificationPort

controller/                          ← el borde HTTP (Hexagonal — un "adaptador
│                                       de entrada" en vez de salida)
├── ChatController.java              ← llama a application/chat/*, nunca al dominio
│                                       directo ni a infraestructura directo
└── MessageController.java
```

## Una forma rápida de ubicarte cuando estés leyendo código

Si estás mirando un archivo y no recuerdas de qué capa es, pregúntate:

- **¿Puedo probar esta clase sin levantar Spring ni una base de datos?**
  Si sí → `domain/`.
- **¿Esta clase orquesta (llama a varias cosas en orden) pero no decide
  reglas de negocio por sí misma?** → `application/`.
- **¿Esta clase habla con algo externo (base de datos, WebSocket, JSON)?**
  → `infrastructure/`.
- **¿Esta clase recibe HTTP y devuelve HTTP?** → `controller/`.

## Autoevaluación

1. ¿Por qué `domain/chat/exceptions/` no lanza excepciones con código HTTP
   directamente (como `ForbiddenException`)? ¿Qué capa se encarga de esa
   traducción, y dónde está ese código?
2. `ChatBehavior.java` y `Chat.java` están separados en dos archivos en vez de
   uno solo. ¿Qué responsabilidad tiene cada uno? (pista: repasa
   `01-ddd/02-el-aggregate-chat.md` si no estás seguro todavía)
3. `ChatProjector` vive en `infrastructure/`, no en `application/`, aunque los
   *use cases* lo llaman directamente. ¿Por qué esto no rompe la regla de
   Hexagonal de "application solo depende de dominio y puertos"? (pista: ver
   `apuntes/05-decisiones-de-diseno/`)
