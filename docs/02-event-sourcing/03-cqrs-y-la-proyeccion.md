# CQRS y la proyección: por qué el frontend nunca toca el Event Store

## El problema que resuelve CQRS

Ya viste en el documento anterior que reconstruir un `Chat` significa
reproducir *todos* sus eventos. Para un chat con pocos mensajes, no importa.
Para un chat activo con miles de mensajes, hacer eso **cada vez que alguien
abre la conversación** sería lento de una forma que se nota.

CQRS (Command Query Responsibility Segregation) es la respuesta a este
problema, y su idea central es simple de enunciar aunque tenga
implicaciones profundas: **separa el modelo que usas para escribir
(comandos) del modelo que usas para leer (consultas)**. No tienen que ser
la misma estructura de datos, ni siquiera la misma tecnología.

## Cómo se ve esto en cryptomessage, concretamente

| | Lado de escritura ("C") | Lado de lectura ("Q") |
|---|---|---|
| **Qué es** | El aggregate `Chat`, reconstruido desde eventos | Las tablas `chats`, `messages`, `contacts` (las mismas que ya existían antes de esta migración) |
| **Quién lo usa** | `CreateChatUseCase`, `AcceptChatUseCase`, `SendMessageUseCase`, `MarkChatAsReadUseCase` | `GetMyChatsUseCase`, `GetMessagesByChatUseCase`, y los controllers que responden HTTP |
| **Cómo se actualiza** | Reproduciendo/añadiendo eventos | `ChatProjector`, reaccionando a esos mismos eventos |
| **Optimizado para** | Validar invariantes correctamente | Consultarse rápido, con el formato exacto que el frontend espera |

Este es el punto crucial que quizás no sea obvio a primera vista: **las
tablas que ya tenías (`chats`, `messages`, `contacts`) no desaparecieron ni
se reemplazaron**. Pasaron a cumplir un rol distinto — dejaron de ser "la
fuente de verdad" y pasaron a ser una *proyección*, una vista derivada,
mantenida al día por reacción a los eventos reales.

## El proyector: el puente entre los dos mundos

```java
// infrastructure/projection/ChatProjector.java
@Transactional
public void project(Long chatId, List<DomainEvent> events) {
    for (DomainEvent event : events) {
        if (event instanceof ChatCreated e) {
            projectChatCreated(chatId, e);
        } else if (event instanceof MessageSent e) {
            projectMessageSent(chatId, e);
        } else if (event instanceof ChatAccepted ignored) {
            projectChatAccepted(chatId);
        } else if (event instanceof ChatRead e) {
            projectChatRead(chatId, e);
        }
    }
}
```

Por cada tipo de evento, el proyector traduce "esto pasó" en "así se ve
ahora la tabla de lectura":

```java
private void projectMessageSent(Long chatId, MessageSent event) {
    Chat chat = findChat(chatId);              // ← la entidad JPA, no el aggregate
    AppUser sender = findUser(event.getSenderId());
    Message message = new Message(chat, sender, event.getContentByUser());
    chat.addMessage(message);
    messageRepository.save(message);
}
```

Fíjate que esto **reutiliza la entidad JPA `Message` que ya existía**, con
sus propios constructores y validaciones. No se inventó una tabla nueva de
lectura — se le dio un nuevo origen (reaccionar a eventos) a la misma tabla
de siempre. Esa es una decisión pragmática importante que verás con más
detalle en `05-decisiones-de-diseno/`: en vez de "purismo CQRS" con tablas
de lectura completamente nuevas, se reutilizó lo que ya funcionaba y ya
tenía el formato exacto que el frontend espera.

## Sincronía deliberada: sin cola de mensajes

Un detalle importante que distingue esta implementación de un CQRS "de
libro de texto": la proyección se actualiza **en la misma transacción** que
el evento se guarda, no de forma asíncrona a través de una cola de mensajes.

```java
// application/message/SendMessageUseCase.java
@Transactional
public MessageResponse execute(SendMessageRequest request) {
    // ...
    chat.sendMessage(...)
    chatAggregateRepository.save(chat);           // ← guarda el evento
    Message message = chatProjector.projectMessageSent(...);  // ← actualiza la proyección
    // ... misma transacción, todo o nada
}
```

En sistemas CQRS a gran escala es común que la proyección se actualice de
forma asíncrona (el evento se publica en una cola, un worker separado lo
consume y actualiza la vista de lectura más tarde). Eso trae *consistencia
eventual*: hay una ventana de tiempo, aunque sea de milisegundos, donde el
evento ya existe pero la proyección todavía no lo refleja.

Para una app de chat, esa ventana sería un problema real y visible: "envié
un mensaje pero no aparece en mi lista de chats todavía" es exactamente el
tipo de bug que un usuario nota de inmediato. Por eso la decisión aquí fue
sacrificar el desacople total (que sí tendría sentido si esto necesitara
escalar horizontalmente con un sistema de mensajería) a cambio de
consistencia inmediata garantizada por una sola transacción de base de
datos.

## ¿Qué gana el frontend con todo esto?

Nada — y esa es la idea. El frontend sigue pegándole a
`GET /api/v1/messages/chat/{chatId}`, recibe exactamente el mismo JSON de
siempre, con la misma latencia de siempre (de hecho, mejor: nunca reproduce
eventos para responder una lectura). Todo el cambio de arquitectura ocurrió
completamente del lado del servidor.

```java
// application/message/GetMessagesByChatUseCase.java — lectura pura
@Transactional(readOnly = true)
public List<MessageResponse> execute(Long chatId) {
    Chat chat = chatReadRepository.findById(chatId)...  // ← tabla de lectura, no event store
    return messageRepository.findByChatOrderBySentAtAsc(chat)...
}
```

Este caso de uso **nunca toca `ChatAggregateRepository`**. Ni falta que le
hace — es una lectura pura, no valida ni cambia ninguna regla de negocio,
así que no necesita el aggregate en absoluto.

## Autoevaluación

1. Explica con tus propias palabras por qué "el aggregate y la proyección
   pueden estar desincronizados por un instante" sería aceptable en algunos
   sistemas pero no en cryptomessage.
2. ¿Por qué `GetMessagesByChatUseCase` no depende de `ChatAggregateRepository`
   en absoluto? ¿Qué tendría que cambiar en los requisitos del negocio para
   que sí necesitara depender de él?
3. Si mañana cryptomessage necesitara escalar a millones de mensajes por
   segundo, ¿qué parte de este diseño (sincronía vs. asincronía) sería la
   primera candidata a reconsiderar? ¿Qué tendrías que introducir para
   hacerlo?
