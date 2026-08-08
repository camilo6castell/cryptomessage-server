# Event Sourcing — Conceptos fundamentales

## El cambio de mentalidad, en una frase

En persistencia tradicional guardas **dónde quedaron las cosas**. En Event
Sourcing guardas **lo que pasó**, y calculas dónde quedaron las cosas cuando
alguien lo necesita.

Suena a diferencia técnica menor. No lo es — cambia qué preguntas puedes
responder, qué garantías puedes dar, y qué complejidad nueva heredas a
cambio.

## Comparación directa

**Persistencia tradicional** (lo que tenía cryptomessage antes de esta
migración, y lo que sigue teniendo `AppUser` hoy):

```sql
UPDATE chats SET status = 'ACCEPTED' WHERE chat_id = 42;
```

Después de ejecutar esto, la fila dice la verdad sobre el *ahora*. No dice
nada sobre el *antes*: no sabes quién lo aceptó, cuándo exactamente, si hubo
intentos previos rechazados, ni en qué orden pasaron las cosas si hay varias
columnas que cambiaron.

**Event Sourcing**:

```java
appendEvent(new ChatAccepted());
```

Esto no sobrescribe nada. Agrega un hecho nuevo, con marca de tiempo, al
final de una secuencia inmutable. El estado "chat aceptado" no se *guarda*
— se *deriva*, reproduciendo la secuencia completa de hechos que le pasaron
a ese chat.

## El event store: una tabla, muy distinta a las demás

```java
// infrastructure/eventstore/StoredEvent.java
@Entity
@Table(name = "events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"aggregate_id", "version"})
})
public class StoredEvent {
    private String aggregateId;    // ¿de qué Chat es este evento?
    private String aggregateType;  // "chat"
    private String eventType;      // com.cryptomessage.server.domain.chat.events.MessageSent
    private long version;          // posición en la secuencia: 1, 2, 3...
    private LocalDateTime occurredOn;
    private String payload;        // el evento serializado como JSON
}
```

Compárala mentalmente con la tabla `chats` de siempre: ahí cada fila
representa *un chat*, y se actualiza in-place. Aquí, cada fila representa
*un hecho que le pasó a un chat*, nunca se actualiza, y un solo chat puede
tener muchas filas (una por cada evento en su historia).

## Reconstrucción: cómo vuelves del event log al objeto en memoria

```java
// domain/chat/Chat.java
public static Chat from(String chatId, List<DomainEvent> events) {
    if (events.isEmpty()) {
        throw new ChatDomainException("No events found for chat " + chatId);
    }
    Chat chat = new Chat(ChatId.of(chatId));
    events.forEach(chat::applyEvent);
    return chat;
}
```

Este es el corazón del patrón. Para saber "en qué estado está el chat 42
ahora mismo", el sistema:

1. Trae **todos** los eventos guardados con `aggregate_id = 42`, en orden.
2. Crea un `Chat` vacío.
3. Reproduce cada evento uno por uno, dejando que cada uno mute el estado en
   memoria según la lógica de `ChatBehavior`.
4. El objeto resultante es exactamente el mismo estado que tendrías con
   persistencia tradicional — solo que llegaste ahí reproduciendo la
   historia, no leyendo un snapshot.

```java
// domain/chat/ChatBehavior.java
addSubscriber(ChatCreated.class, event -> {
    chat.setUser1Id(UserId.of(event.getUser1Id()));
    // ...
    chat.setStatus(ChatStatus.PENDING);
});

addSubscriber(ChatAccepted.class, event -> chat.setStatus(ChatStatus.ACCEPTED));
```

Nota algo importante: `ChatBehavior` es exactamente el mismo código sin
importar si el evento es "nuevo" (se acaba de generar) o "histórico" (se
está reproduciendo desde la base de datos). Eso es deliberado — es lo que
garantiza que reconstruir un aggregate produce *exactamente* el mismo
resultado que si hubieras estado presente viendo pasar los eventos en
tiempo real.

## ¿Por qué esto es más que una curiosidad técnica?

Tres beneficios reales, no teóricos:

1. **Auditoría gratis.** Con persistencia tradicional, "quién aceptó este
   chat y cuándo" requeriría una tabla de auditoría paralela, mantenida a
   mano. Con Event Sourcing, ya está ahí — es literalmente lo que
   guardaste.

2. **Depuración de producción distinta.** Si un chat termina en un estado
   raro, con persistencia tradicional solo ves la fila final — no sabes
   cómo llegó ahí. Con Event Sourcing puedes reproducir exactamente la
   secuencia de eventos y ver dónde algo se desvió.

3. **Nuevas vistas del pasado, sin re-arquitectura.** Si mañana el negocio
   pide "¿cuántos chats se aceptaron en menos de 1 hora desde que se
   crearon?", con persistencia tradicional esa pregunta es imposible de
   responder retroactivamente (nunca guardaste esa relación). Con Event
   Sourcing, los eventos ya tienen las marcas de tiempo — solo necesitas
   una proyección nueva que los lea.

## El costo real (nadie regala nada)

Vale la pena decir esto sin rodeos, porque es justo lo que separa a alguien
que entendió el patrón de alguien que solo lo memorizó:

- **Leer es más caro por defecto.** Reconstruir un aggregate reproduciendo
  eventos es más trabajo que un `SELECT * FROM chats WHERE id = ?`. En este
  proyecto se resuelve con una **proyección** (ver
  `03-cqrs-y-la-proyeccion.md`) — las lecturas normales ni siquiera tocan el
  event store.
- **Las consultas ad-hoc son más difíciles.** "Dame todos los chats con
  status ACCEPTED" no es un `WHERE` trivial sobre el event store — necesitas
  una vista materializada (otra vez, la proyección).
- **El esquema de un evento es casi para siempre.** Una vez que guardaste
  `MessageSent` con esa forma exacta, cambiarla retroactivamente sobre
  millones de eventos ya guardados es un problema real de versionado de
  esquemas — algo que la persistencia tradicional no tiene en la misma
  magnitud (un `ALTER TABLE` es, comparativamente, sencillo).

## Autoevaluación

1. Explica, sin ver el código, por qué `ChatBehavior` debe comportarse
   *exactamente igual* al aplicar un evento nuevo que al reproducir uno
   histórico. ¿Qué se rompería si no fuera así?
2. ¿Qué información puede responder Event Sourcing que una tabla
   tradicional con un campo `updated_at` no puede?
3. Nombra dos costos reales de Event Sourcing que casi nunca se mencionan en
   los tutoriales que solo muestran el "happy path".
