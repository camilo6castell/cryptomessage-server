# Concurrencia optimista: qué pasa si dos peticiones chocan

## El escenario que hay que poder responder en una entrevista

Dos peticiones llegan casi al mismo tiempo, sobre el mismo chat: Alice
manda un mensaje justo cuando Bob acepta el chat. Ambas leen el mismo
estado del aggregate (el chat todavía `PENDING`, sin el evento del otro
todavía). ¿Qué evita que una de las dos escrituras "desaparezca" o corrompa
el estado del chat?

## Optimista vs. pesimista: la diferencia real

**Concurrencia pesimista** significa bloquear el recurso mientras lo estás
usando — el equivalente a un `SELECT ... FOR UPDATE`. Nadie más puede leer
o escribir ese chat hasta que termines. Es segura, pero cara: cada operación
tiene que esperar en fila, incluso cuando la mayoría de las veces no habría
colisión real.

**Concurrencia optimista** apuesta a que las colisiones son raras: deja que
ambas peticiones lean y procesen libremente, y solo verifica al momento de
*escribir* si algo cambió mientras tanto. Si nadie más escribió, tu cambio
se guarda normal. Si alguien más ya escribió, tu intento falla y tienes que
reintentar (o, en este caso, el cliente HTTP recibe un error y decide si
reintenta).

cryptomessage usa la segunda, y el mecanismo que la hace posible ya lo viste
de pasada en el documento anterior: **el constraint único
`(aggregate_id, version)`**.

## El mecanismo, paso a paso

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
@Override
@Transactional
public void save(Chat chat) {
    List<DomainEvent> uncommitted = chat.getUncommittedChanges();
    String aggregateId = chat.identity().value();
    long nextVersion = storedEventRepository.countByAggregateId(aggregateId) + 1;

    try {
        for (DomainEvent event : uncommitted) {
            StoredEvent storedEvent = new StoredEvent(
                aggregateId, AGGREGATE_TYPE, event.getClass().getName(),
                nextVersion, event.getOccurredOn(), eventSerializer.toJson(event)
            );
            storedEventRepository.save(storedEvent);
            nextVersion++;
        }
    } catch (DataIntegrityViolationException e) {
        throw new ConflictException("This chat was modified concurrently — please retry");
    }

    chat.markChangesAsCommitted();
}
```

Sigamos el escenario de Alice y Bob con esto en mente. Supón que el chat 42
ya tiene 3 eventos guardados (versiones 1, 2, 3).

1. **Alice** carga el chat (`findById`) → reconstruye `Chat` desde los 3
   eventos existentes → llama `chat.sendMessage(...)` → esto genera un
   `MessageSent` en memoria, sin versión asignada todavía.
2. **Bob**, casi al mismo tiempo, también carga el chat (los mismos 3
   eventos) → llama `chat.accept(...)` → genera un `ChatAccepted` en
   memoria.
3. Ambos llaman a `save()`. Ambos calculan `nextVersion = COUNT(*) + 1 = 4`,
   porque en el momento en que cada uno hizo ese `COUNT`, todavía solo
   existían 3 eventos guardados — **ninguno de los dos sabe todavía del
   otro**.
4. Una de las dos transacciones llega primero a la base de datos e inserta
   exitosamente `(aggregate_id=42, version=4)`.
5. La segunda transacción intenta insertar `(aggregate_id=42, version=4)`
   también — y la base de datos rechaza el `INSERT` por el constraint único.
   Spring traduce esa violación SQL en un `DataIntegrityViolationException`.
6. El `catch` lo convierte en `ConflictException`, que
   `GlobalExceptionHandler` mapea a un `409 Conflict` para el cliente HTTP.

**La base de datos, no el código Java, es quien decide quién ganó.** Esto es
importante: no hay una condición de carrera posible en el código Java
mismo, porque la garantía real la da el motor de base de datos al momento
de aplicar el constraint — algo atómico por construcción, sin necesidad de
locks explícitos en la aplicación.

## Por qué NO se verifica la versión "a mano" antes de escribir

Un diseño más ingenuo podría verse así (y de hecho, es un error común):

```java
// ❌ NO es lo que hace este proyecto — ejemplo de lo que NO hacer
long currentVersion = storedEventRepository.countByAggregateId(aggregateId);
if (currentVersion != expectedVersion) {
    throw new ConflictException("...");
}
// ... insertar eventos
```

El problema: entre el `if` que verifica la versión y el `INSERT` real,
sigue existiendo una ventana de tiempo donde otra transacción podría colarse
— la verificación "a mano" en código Java **no es atómica** respecto a la
escritura. El único lugar donde esa garantía es realmente atómica es dentro
del motor de base de datos, en el momento exacto del `INSERT`. Por eso el
diseño real de cryptomessage no verifica nada de antemano — simplemente
intenta escribir, y deja que el constraint único haga el trabajo de
detectar el conflicto de forma infalible.

## El bug que esto corrige (y por qué vale la pena entenderlo)

Este mecanismo con el constraint en la base de datos existe, en parte,
porque el framework genérico original (heredado de otro proyecto,
Library Provider) tenía un defecto sutil: el contador de versión vivía
**dentro del aggregate en memoria**, y nunca se resincronizaba al
reconstruir un aggregate desde su historial — solo se actualizaba cuando se
llamaba `appendEvent()`, nunca durante el replay de eventos históricos.

Eso significa que dos instancias distintas de `Chat`, reconstruidas de forma
independiente, podían calcular "la siguiente versión es 1" cada una,
aunque en la base de datos ya hubiera decenas de eventos reales guardados
— porque ninguna de las dos había estado presente en tiempo real, y el
contador en memoria de un objeto recién reconstruido simplemente arranca en
cero. El control de concurrencia habría fallado en silencio.

La lección detrás de esto es más importante que el bug en sí: **la fuente
de verdad de "cuántos eventos existen" tiene que vivir donde vive la fuente
de verdad de los eventos mismos** — la base de datos — no en un contador
derivado que un objeto en memoria intenta llevar por su cuenta.

## Autoevaluación

1. Explica por qué verificar la versión con un `SELECT` y luego decidir si
   insertar, en dos pasos separados desde código Java, no es una garantía
   real contra condiciones de carrera.
2. ¿Qué debería hacer el cliente (frontend o quien sea que reciba un 409 de
   este endpoint) cuando recibe este error? ¿Es razonable simplemente
   reintentar automáticamente, o depende del contexto?
3. Describe, con tus propias palabras y sin ver el código, por qué un
   contador de versión que vive dentro del aggregate en memoria (en vez de
   en el adaptador de persistencia) es una fuente de bugs sutiles en
   sistemas donde los aggregates se reconstruyen constantemente desde cero.
