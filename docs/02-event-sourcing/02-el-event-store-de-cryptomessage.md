# El Event Store de cryptomessage, pieza por pieza

Este documento recorre el camino completo de un evento: desde que
`Chat.sendMessage()` lo crea en memoria, hasta que queda guardado en la
tabla `events`, y de regreso cuando alguien necesita reconstruir el
aggregate.

## Paso 1 — El evento nace, pero no se guarda todavía

```java
// domain/generic/ChangeEventSubscriber.java
public DomainEvent appendEvent(DomainEvent domainEvent) {
    domainEvent.setOccurredOn(LocalDateTime.now());
    domainEvents.add(domainEvent);   // ← lista de "no confirmados", en memoria
    return domainEvent;
}
```

Cuando `Chat.sendMessage()` llama a `appendEvent(new MessageSent(...))`, el
evento se marca con la hora actual y se guarda en una lista **en memoria**,
no en la base de datos. Esto es clave: el aggregate puede generar varios
eventos durante una sola operación de negocio sin que cada uno dispare una
escritura a la base de datos por separado.

```java
public List<DomainEvent> getUncommittedChanges() {
    return List.copyOf(changeEventSubscriber.events());
}
```

El caso de uso (`SendMessageUseCase`) lee esta lista después de invocar al
aggregate, y es responsable de persistirla.

## Paso 2 — Asignar la versión (y por qué NO lo hace el aggregate)

Aquí hay una decisión de diseño importante, documentada explícitamente en
el código porque corrige un bug real que encontramos al portar el framework
desde otro proyecto (Library Provider):

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
long nextVersion = storedEventRepository.countByAggregateId(aggregateId) + 1;

for (DomainEvent event : uncommitted) {
    StoredEvent storedEvent = new StoredEvent(
        aggregateId, AGGREGATE_TYPE, event.getClass().getName(),
        nextVersion, event.getOccurredOn(), eventSerializer.toJson(event)
    );
    storedEventRepository.save(storedEvent);
    nextVersion++;
}
```

**La versión la calcula el adaptador de persistencia**, contando cuántos
eventos ya existen para ese `aggregateId` en la base de datos — no un
contador interno del objeto `Chat` en memoria.

¿Por qué importa esta distinción? Porque un aggregate reconstruido con
`Chat.from(...)` (reproduciendo eventos históricos) nunca vuelve a tocar ese
contador — solo se usa cuando se llama `appendEvent`, nunca durante el
replay. Si el aggregate mismo llevara la cuenta de versión, dos instancias
distintas de `Chat` (una reconstruida con 50 eventos históricos, otra recién
creada) podrían calcular la "siguiente versión" de forma incorrecta y
colisionar. Delegar esta responsabilidad al adaptador, que siempre consulta
la fuente de verdad real (la base de datos), elimina esa clase de bug de
raíz.

## Paso 3 — Serialización: objeto Java → JSON

```java
// infrastructure/eventstore/EventSerializer.java
public String toJson(DomainEvent event) {
    return objectMapper.writeValueAsString(event);
}
```

Cada evento se convierte a JSON antes de guardarse en la columna `payload`
(tipo `TEXT`). Esto es lo que hace posible que la tabla `events` sea
genérica — no necesita una columna por cada tipo de evento, solo necesita
saber *qué clase* deserializar (columna `event_type`, el nombre completo de
la clase Java) y el JSON crudo.

## Paso 4 — El constraint que hace posible la concurrencia optimista

```java
@Table(name = "events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"aggregate_id", "version"})
})
```

Esta única línea es la que convierte "asignar versiones" en una garantía
real, no solo una convención. Si dos peticiones concurrentes intentan
modificar el mismo chat al mismo tiempo, ambas calcularán "la siguiente
versión es 5" (porque ambas leyeron el mismo estado antes de escribir) — pero
solo **una** logrará insertar la fila con `version = 5`; la base de datos
rechazará la segunda por violar el constraint único. Ver el documento
`04-concurrencia-optimista.md` para el flujo completo de este escenario.

## Paso 5 — Reconstrucción: JSON → objeto Java, de vuelta

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
List<DomainEvent> events = storedEvents.stream()
    .map(stored -> {
        DomainEvent event = eventSerializer.fromJson(stored.getPayload(), stored.getEventType());
        event.setOccurredOn(stored.getOccurredOn());
        event.setAggregateRootId(stored.getAggregateId());
        return event;
    })
    .toList();

return Optional.of(Chat.from(chatId.value(), events));
```

Nota algo sutil aquí: `occurredOn` y `aggregateRootId` **no** se restauran
leyéndolos del JSON — se restauran directamente desde las columnas propias
de `StoredEvent`. La razón es simple: esas columnas ya existen de forma
confiable en la fila, así que no hay necesidad de depender de que el JSON
las traiga también (y de hecho, así evitamos un problema real de
deserialización con Jackson — el constructor de cada evento, anotado con
`@JsonCreator`, solo declara los campos propios del evento, no los
heredados de `DomainEvent`).

## El árbol completo, de un vistazo

```
Chat.sendMessage()
  └─ appendEvent(new MessageSent(...))          ← en memoria, sin versión aún
       │
SendMessageUseCase.execute()
  └─ chatAggregateRepository.save(chat)
       │
ChatAggregateRepositoryAdapter.save()
  ├─ nextVersion = COUNT(*) events WHERE aggregate_id = ?
  ├─ eventSerializer.toJson(event)               ← objeto Java → JSON
  └─ storedEventRepository.save(new StoredEvent(...))   ← INSERT real

  [ más tarde, en otra petición ]

ChatAggregateRepositoryAdapter.findById()
  ├─ SELECT * FROM events WHERE aggregate_id = ? ORDER BY version
  ├─ eventSerializer.fromJson(...)                ← JSON → objeto Java
  └─ Chat.from(chatId, events)                     ← replay completo
```

## Autoevaluación

1. Explica, con tus propias palabras, por qué asignar la versión en el
   adaptador (y no en el aggregate) es lo que hace que el control de
   concurrencia optimista funcione de manera confiable.
2. ¿Qué pasaría si `occurredOn` y `aggregateRootId` se leyeran del JSON en
   vez de restaurarse desde las columnas de `StoredEvent`? (pista: piensa en
   qué necesita el `@JsonCreator` de cada evento para deserializar
   correctamente)
3. Si tuvieras que agregar un nuevo tipo de evento a `Chat` (por ejemplo,
   `ChatArchived`), enumera todos los archivos que tendrías que tocar,
   siguiendo el camino de este documento.
