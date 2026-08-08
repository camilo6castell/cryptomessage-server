# El aggregate `Chat`, en detalle

Este documento diseca `domain/chat/Chat.java` pieza por pieza. Si
`01-conceptos-fundamentales.md` te dio el vocabulario, este te muestra el
razonamiento detrás de cada decisión concreta de diseño.

## La decisión menos obvia: qué NO guarda el aggregate

Lo primero que salta a la vista si vienes de un ORM tradicional: `Chat` no
tiene una `List<Message>`. Ningún campo del aggregate contiene la lista de
mensajes.

```java
public class Chat extends AggregateRoot<ChatId> {
    private UserId user1Id;
    private UserId user2Id;
    private UserId initiatedBy;
    private ChatStatus status;
    private boolean hasPendingMessage;   // ← esto, no una lista de mensajes
    // ...
}
```

Esto es intencional, y es la decisión de diseño más importante de este
aggregate. La razón está atada a Event Sourcing (lo verás con más detalle en
`02-event-sourcing/`), pero el razonamiento de DDD puro ya la justifica por
sí solo: **el aggregate solo necesita guardar el estado mínimo necesario
para verificar sus propios invariantes** — no toda la información que
alguna vez existió sobre él.

Pregúntate: ¿qué invariante de `Chat` depende del historial completo de
mensajes? Solo una: "mientras está `PENDING`, solo puede haber un mensaje
enviado". Para verificar eso, no necesitas la lista completa — necesitas un
solo booleano: ¿ya se envió ese mensaje o no?

```java
addSubscriber(MessageSent.class, event -> {
    if (chat.getStatus() == ChatStatus.PENDING) {
        chat.markPendingMessageSent();
    }
});
```

Si mañana apareciera una regla nueva como "no se pueden enviar más de 100
mensajes por hora", eso SÍ obligaría al aggregate a rastrear más estado (un
conteo con ventana de tiempo, por ejemplo) — pero seguiría sin necesitar el
*contenido* de esos mensajes. El aggregate modela invariantes, no reportes.

## `Chat` como aggregate root de `Message`

Aunque `Chat` no guarda la lista de mensajes en memoria, sigue siendo el
único punto de entrada para crear uno. No existe (ni debería existir nunca)
un `MessageAggregateRoot` independiente. `Message` conceptualmente vive
*dentro* del límite de consistencia de `Chat` — su validez depende
completamente del estado de su chat en el momento de crearse.

```java
public MessageId sendMessage(UserId senderId, Map<UserId, String> encryptedContentByUser) {
    assertParticipant(senderId);
    // ... valida contra el estado actual del Chat, no del Message
    MessageId messageId = MessageId.generate();
    // ...
    appendEvent(new MessageSent(messageId.value(), senderId.value(), rawContent));
    return messageId;
}
```

Fíjate que el método devuelve un `MessageId`, no un objeto `Message`
completo — porque, otra vez, el aggregate no modela "un Message como cosa
persistente en memoria", solo modela "el hecho de que se envió un mensaje" y
delega guardar los detalles a otra parte del sistema (la proyección — ver
`02-event-sourcing/03-cqrs-y-la-proyeccion.md`).

## Los métodos son el único contrato público

Mira los métodos públicos de `Chat`: `create`, `from`, `sendMessage`,
`accept`, `markAsRead`, `getOtherParticipant`, y los getters. Eso es todo.
No hay setters. No hay forma de poner el aggregate en un estado inválido
desde afuera — cada transición pasa por un método con nombre de negocio
(`accept()`, no `setStatus(ACCEPTED)`) que valida antes de actuar.

Esto es DDD aplicado literalmente: **el código público de la clase debe
leerse como el vocabulario del negocio**, no como operaciones CRUD
genéricas.

## `assertParticipant`: el patrón de invariante compartido

```java
private void assertParticipant(UserId userId) {
    if (!userId.equals(user1Id) && !userId.equals(user2Id)) {
        throw new NotParticipantException("User " + userId.value() + " is not part of this chat");
    }
}
```

Tres métodos distintos (`sendMessage`, `accept`, `markAsRead`,
`getOtherParticipant`) empiezan verificando esto. Es un invariante que
aplica a *cualquier* interacción con el chat, no a una operación específica
— por eso vive como un método privado reutilizable en vez de repetirse (o,
peor, verificarse de forma distinta) en cada lugar.

## `ChatId` vs value object cualquiera: por qué es una `Identity`

```java
public final class ChatId extends Identity {
```

`ChatId` no implementa `IValueObject` directamente — extiende `Identity`,
que sí lo implementa pero le añade semántica de identidad tipada (dos
`ChatId` con el mismo string valen igual; un `ChatId` y un `MessageId` con
el mismo string NO valen igual, aunque ambos "son" strings por dentro). Esto
evita una clase entera de bugs sutiles: pasar un `MessageId` donde se
esperaba un `ChatId` no compila, mientras que si ambos fueran `String`
crudo, ese error solo aparecería en tiempo de ejecución (o nunca, si el bug
queda enmascarado por casualidad).

## Autoevaluación

1. Si tuvieras que agregar la regla "un chat no puede tener más de 500
   mensajes en total", ¿qué campo(s) tendrías que agregar a `Chat`? ¿Seguiría
   siendo cierto que el aggregate no necesita la lista completa de mensajes?
2. `getOtherParticipant(UserId userId)` llama a `assertParticipant` antes de
   devolver el otro participante. ¿Por qué tiene sentido que ESTE método
   también valide, si solo está leyendo datos, no mutando estado?
3. Explica por qué `sendMessage` devuelve un `MessageId` (un value object
   pequeño) en vez de un objeto `Message` completo con todos sus campos.
