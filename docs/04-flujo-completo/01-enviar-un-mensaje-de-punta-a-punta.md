# Enviar un mensaje, de punta a punta

Todos los documentos anteriores explicaron piezas por separado. Este los
ensambla, siguiendo una sola petición HTTP real —
`POST /api/v1/messages`— línea por línea, a través de las tres capas. Si
puedes explicar este flujo completo sin mirar el código, dominas los tres
patrones lo suficiente para defenderlos en una entrevista.

## El punto de partida: la petición HTTP

```json
POST /api/v1/messages
{
  "chatId": 42,
  "encryptedContentByUser": {
    "101": "<ciphertext para el usuario 101>",
    "102": "<ciphertext para el usuario 102>"
  }
}
```

## Paso 1 — El controller: la puerta de entrada (Hexagonal)

```java
// controller/MessageController.java
@PostMapping
public ResponseEntity<MessageResponse> sendMessage(@RequestBody SendMessageRequest request) {
    MessageResponse response = sendMessageUseCase.execute(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

El controller no sabe nada de Chat, de eventos, ni de proyecciones. Su único
trabajo es traducir HTTP a una llamada de aplicación, y traducir el
resultado de vuelta a HTTP. Esto es, técnicamente, un "adaptador de
entrada" en el vocabulario de Hexagonal (el mundo exterior *entrando* al
sistema), aunque en este proyecto no existe una interfaz explícita para
esto — el caso de uso mismo cumple ese rol de punto de entrada.

## Paso 2 — El caso de uso: orquesta, no decide (Hexagonal + DDD)

```java
// application/message/SendMessageUseCase.java
@Transactional
public MessageResponse execute(SendMessageRequest request) {
    AppUser sender = currentUserService.get();
    ChatId chatId = ChatId.of(String.valueOf(request.chatId()));

    Chat chat = chatAggregateRepository.findById(chatId)     // ← Paso 3
            .orElseThrow(() -> new NoSuchElementException("Chat not found"));

    Map<UserId, String> encryptedContentByUser = /* traduce el DTO a value objects */;

    chat.sendMessage(UserId.of(sender.getUserId()), encryptedContentByUser);  // ← Paso 4

    List<DomainEvent> newEvents = chat.getUncommittedChanges();
    chatAggregateRepository.save(chat);                        // ← Paso 5

    MessageSent event = (MessageSent) newEvents.get(0);
    Message message = chatProjector.projectMessageSent(request.chatId(), event);  // ← Paso 6

    // ... construir la respuesta y notificar por WebSocket — Paso 7
}
```

Fíjate en algo importante: este método no tiene ni un solo `if` sobre
reglas de negocio. No verifica "¿está el chat aceptado?" ni "¿es el
usuario un participante?" — esas preguntas las responde el aggregate. El
caso de uso solo sabe **el orden** en que deben pasar las cosas: cargar,
pedirle al dominio que actúe, guardar, proyectar, notificar. Ese es
precisamente el límite entre DDD (las reglas viven en el aggregate) y la
capa de aplicación (la orquestación vive aquí).

## Paso 3 — Reconstruir el aggregate (Event Sourcing)

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
public Optional<Chat> findById(ChatId chatId) {
    List<StoredEvent> storedEvents = storedEventRepository.findByAggregateIdOrderByVersionAsc(chatId.value());
    List<DomainEvent> events = storedEvents.stream()
            .map(stored -> /* JSON → DomainEvent */)
            .toList();
    return Optional.of(Chat.from(chatId.value(), events));
}
```

En este punto, si el chat 42 tiene 3 eventos históricos
(`ChatCreated`, `MessageSent`, `ChatAccepted`, digamos), los tres se
reproducen en orden dentro de `Chat.from(...)`, y el objeto `chat` en
memoria termina con `status = ACCEPTED`, `hasPendingMessage = true` (o lo
que corresponda), listo para recibir la siguiente acción.

## Paso 4 — El aggregate valida y emite un nuevo evento (DDD + ES)

```java
// domain/chat/Chat.java
public MessageId sendMessage(UserId senderId, Map<UserId, String> encryptedContentByUser) {
    assertParticipant(senderId);                          // invariante 1
    if (status == ChatStatus.PENDING) {
        if (!senderId.equals(initiatedBy)) { throw ... }  // invariante 2
        if (hasPendingMessage) { throw ... }               // invariante 3
    }
    // ... validar que el contenido cifrado cubre a ambos participantes
    MessageId messageId = MessageId.generate();
    appendEvent(new MessageSent(messageId.value(), senderId.value(), rawContent));
    return messageId;
}
```

Si el chat ya está `ACCEPTED` (el caso normal para un chat con conversación
en curso), ninguna de las validaciones de `PENDING` aplica — el mensaje se
acepta directamente. `appendEvent` deja el nuevo evento en la lista de "no
confirmados" del aggregate, sin tocar la base de datos todavía.

## Paso 5 — Persistir el evento, con concurrencia optimista (Event Sourcing)

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
public void save(Chat chat) {
    long nextVersion = storedEventRepository.countByAggregateId(aggregateId) + 1;
    // INSERT con version = nextVersion, protegido por el constraint único
}
```

Aquí es donde, si otra petición modificó este mismo chat entre el Paso 3 y
este momento, el `INSERT` fallaría por el constraint `(aggregate_id,
version)` y toda la transacción se revertiría — incluyendo cualquier cosa
que ya hubiera pasado en los pasos anteriores dentro de la misma
transacción de Spring. Ver `02-event-sourcing/04-concurrencia-optimista.md`
para el detalle completo de este mecanismo.

## Paso 6 — Actualizar la proyección (CQRS)

```java
// infrastructure/projection/ChatProjector.java
public Message projectMessageSent(Long chatId, MessageSent event) {
    Chat chat = findChat(chatId);          // la entidad JPA, no el aggregate
    AppUser sender = findUser(event.getSenderId());
    Message message = new Message(chat, sender, event.getContentByUser());
    chat.addMessage(message);
    return messageRepository.save(message);
}
```

Este paso ocurre **dentro de la misma transacción** que el Paso 5 — si algo
falla aquí, el evento del Paso 5 también se revierte. Esto es lo que
garantiza que el frontend nunca vea un estado a medias (un evento guardado
sin su reflejo en la tabla de lectura).

## Paso 7 — Responder y notificar (Hexagonal, puerto de salida)

```java
notificationPort.notifyUser(recipient.getUsername(), "/queue/messages", responseForRecipient);
notificationPort.notifyUser(sender.getUsername(), "/queue/messages", responseForSender);
return responseForSender;
```

`SendMessageUseCase` no sabe que esto es WebSocket por debajo — solo sabe
que existe un `NotificationPort` capaz de avisarle a alguien. El controller
recibe `responseForSender` de vuelta y lo serializa como JSON de respuesta,
exactamente el mismo formato que el frontend ya esperaba antes de esta
migración completa.

## El mapa completo, de un vistazo

```
HTTP POST /api/v1/messages
        │
        ▼
MessageController                    (Hexagonal: adaptador de entrada)
        │
        ▼
SendMessageUseCase                   (Hexagonal: orquestación, capa de aplicación)
        │
        ├─► ChatAggregateRepository.findById()   (puerto)
        │         │
        │         ▼
        │   ChatAggregateRepositoryAdapter        (Hexagonal: adaptador)
        │         │
        │         ▼
        │   Chat.from(events)                     (Event Sourcing: replay)
        │
        ├─► chat.sendMessage(...)                 (DDD: valida invariantes, emite evento)
        │
        ├─► ChatAggregateRepository.save()        (puerto)
        │         │
        │         ▼
        │   ChatAggregateRepositoryAdapter         (Event Sourcing: persiste + concurrencia)
        │
        ├─► ChatProjector.projectMessageSent()     (CQRS: actualiza la tabla de lectura)
        │
        └─► NotificationPort.notifyUser()          (puerto)
                  │
                  ▼
            StompNotificationAdapter                (Hexagonal: adaptador)
```

## Autoevaluación

1. Sin ver el diagrama, dibuja (en papel o mentalmente) este mismo flujo
   para `AcceptChatUseCase` en vez de `SendMessageUseCase`. ¿Qué pasos son
   idénticos? ¿Cuál cambia?
2. ¿En qué paso exacto se lanzaría una excepción si el usuario que intenta
   mandar el mensaje no es participante del chat? ¿Qué código HTTP recibiría
   el cliente, y por qué camino llega ese mapeo?
3. Si `chatProjector.projectMessageSent()` (Paso 6) lanzara una excepción
   inesperada, ¿qué pasa con el evento que ya se guardó en el Paso 5? Explica
   el mecanismo exacto que garantiza esa respuesta.
