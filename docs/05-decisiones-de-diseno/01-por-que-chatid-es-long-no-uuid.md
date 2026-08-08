# Por qué `ChatId` es un `Long`, no un UUID

## La tensión: "pureza" del patrón vs. compatibilidad real

Si buscas cualquier tutorial de Event Sourcing, casi todos generan el id del
aggregate con un UUID aleatorio, en memoria, sin tocar la base de datos:

```java
// El enfoque "de manual" — lo que NO se hizo aquí
public static Chat create(...) {
    Chat chat = new Chat(ChatId.generate());  // UUID.randomUUID(), sin I/O
    // ...
}
```

Es simple, no requiere coordinación con nada externo, y es el ejemplo que
vas a ver en el 90% del material introductorio sobre el patrón.

El problema: el frontend de cryptomessage ya existía antes de esta
migración, y su contrato con el backend usa `Long` para `chatId` — en el
JSON de las respuestas (`ChatResponse.chatId`) y en las rutas
(`/api/v1/chats/{chatId}`). Cambiar eso a un `String` con formato UUID
habría sido un cambio de contrato visible para el frontend — exactamente lo
que la migración completa se propuso evitar desde el principio.

## La solución: separar "identidad del dominio" de "cómo se genera"

La respuesta no fue "renunciar a Event Sourcing por compatibilidad", sino
notar algo importante: **nada en el patrón exige que el id sea un UUID.**
Un UUID es solo la opción más simple, no un requisito. Lo único que Event
Sourcing necesita de verdad es que el id sea único de forma confiable.

Entonces: en vez de generar el id en memoria sin coordinación (lo que un
UUID te permite hacer, porque su unicidad es probabilística y no requiere
consultar nada), se decidió que generar el id **sí requiere una consulta a
base de datos** — usando el propio mecanismo de `AUTO_INCREMENT` de MySQL,
a través de una tabla dedicada:

```java
// infrastructure/eventstore/ChatIdSequence.java
@Entity
@Table(name = "chat_id_sequences")
public class ChatIdSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // deliberadamente sin ningún otro campo — su único trabajo es "dame
    // un número único que nunca se repite"
}
```

```java
// infrastructure/eventstore/JpaChatIdGenerator.java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public ChatId nextId() {
    ChatIdSequence saved = sequenceRepository.save(new ChatIdSequence());
    return ChatId.of(String.valueOf(saved.getId()));
}
```

## Por qué esto es una operación de infraestructura, no de dominio (repaso de Hexagonal)

Esta es la razón por la que `Chat.create()` **no** genera su propio id — lo
recibe ya generado:

```java
public static Chat create(ChatId chatId, UserId requesterId, UserId otherUserId, UserId initiatedBy) {
    // el id ya viene minteado desde afuera
}
```

Generar un `UUID.randomUUID()` es una operación pura — no necesita hablar
con nada externo, así que sí podría vivir dentro del dominio sin
contradecir Hexagonal. Pero generar un id consultando una tabla de base de
datos **sí es I/O**, y el dominio, por definición, no hace I/O directamente
— eso es exactamente lo que los puertos existen para separar. Por eso
`ChatIdGenerator` es un puerto (`domain/chat/port/ChatIdGenerator.java`), y
el caso de uso es quien lo consulta antes de llamar a `Chat.create(...)`.

```java
// application/chat/CreateChatUseCase.java
ChatId chatId = chatIdGenerator.nextId();          // I/O, vive en application
Chat chat = Chat.create(chatId, ...);               // puro, vive en domain
```

## `@Transactional(propagation = REQUIRES_NEW)`: el detalle que casi se pasa por alto

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public ChatId nextId() {
```

Esto merece explicación aparte. `REQUIRES_NEW` significa que este método
**siempre abre su propia transacción**, independiente de la transacción más
grande de `CreateChatUseCase.execute()` (que probablemente ya está en
curso). ¿Por qué importa esto?

Imagina que no fuera así, y `nextId()` compartiera la transacción del caso
de uso. Si, más adelante en ese mismo flujo, la proyección falla (por
ejemplo, porque ya existe un chat entre esos dos usuarios — un conflicto de
negocio real) y toda la transacción se revierte, el `INSERT` en
`chat_id_sequences` **también se revertiría**. Eso significaría que ese
valor de `AUTO_INCREMENT` podría, en teoría, quedar disponible para
reusarse — rompiendo la garantía de unicidad que toda esta tabla existe
para dar.

Con `REQUIRES_NEW`, el `INSERT` en `chat_id_sequences` se confirma de
inmediato, sin importar qué pase después en el resto del flujo. Si el resto
falla, ese número queda "quemado" para siempre — exactamente el mismo
comportamiento que tendría cualquier columna `AUTO_INCREMENT` normal frente
a una transacción revertida (un salto en la secuencia, inofensivo, nunca
una colisión).

## Autoevaluación

1. Explica por qué generar un UUID en memoria es una operación "pura" (en el
   sentido de Hexagonal/DDD) pero consultar una tabla `AUTO_INCREMENT` no lo
   es.
2. ¿Qué problema concreto podría ocurrir si `nextId()` no usara
   `REQUIRES_NEW` y compartiera la transacción del caso de uso que la
   invoca?
3. Si cryptomessage no tuviera un frontend ya construido con `Long` como
   tipo de `chatId`, ¿elegirías la misma solución (tabla de secuencia
   dedicada) o usarías el enfoque más simple de UUID? Justifica tu
   respuesta.
