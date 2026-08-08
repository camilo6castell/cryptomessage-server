# Glosario

Definiciones cortas, para consulta rápida. Cada una apunta al documento
donde se explica con profundidad si necesitas el contexto completo.

**Adaptador (Adapter)** — Implementación concreta de un puerto, que sí
depende de una tecnología específica (JPA, WebSocket, etc.). Vive en
`infrastructure/`. → `03-hexagonal/01-conceptos-fundamentales.md`

**Aggregate / Aggregate Root** — Un grupo de objetos tratados como una sola
unidad de consistencia; el aggregate root es el único punto de entrada para
modificarlos. En este proyecto: `Chat`. → `01-ddd/01-conceptos-fundamentales.md`

**ArchUnit** — Librería que permite escribir tests sobre la estructura del
código (qué paquete depende de cuál), corriendo como parte normal de la
suite de tests. → `03-hexagonal/03-package-based-vs-modular-y-archunit.md`

**Concurrencia optimista** — Estrategia que permite que varias operaciones
lean y procesen libremente, verificando conflictos solo al momento de
escribir (en vez de bloquear el recurso de antemano). →
`02-event-sourcing/04-concurrencia-optimista.md`

**CQRS (Command Query Responsibility Segregation)** — Separar el modelo
usado para escribir del modelo usado para leer. En este proyecto: el
aggregate `Chat` (escritura) vs. las tablas `chats`/`messages`/`contacts`
(lectura). → `02-event-sourcing/03-cqrs-y-la-proyeccion.md`

**DDD (Domain-Driven Design)** — Enfoque de diseño donde el lenguaje y las
reglas del negocio determinan la estructura del código, en vez de al revés.
→ `01-ddd/01-conceptos-fundamentales.md`

**DomainEvent** — Clase base de todo evento de dominio; representa un hecho
inmutable que ya ocurrió. → `02-event-sourcing/01-conceptos-fundamentales.md`

**Entidad (Entity)** — Objeto con identidad propia que persiste a través de
cambios en sus atributos. → `01-ddd/01-conceptos-fundamentales.md`

**Event Sourcing** — Persistir la secuencia de hechos que le pasaron a un
aggregate, en vez de su estado final; el estado se deriva reproduciendo esos
hechos. → `02-event-sourcing/01-conceptos-fundamentales.md`

**Event Store** — El almacenamiento (en este proyecto, la tabla `events`)
donde vive la secuencia inmutable de eventos de todos los aggregates.
→ `02-event-sourcing/02-el-event-store-de-cryptomessage.md`

**Hexagonal Architecture (Ports and Adapters)** — Patrón que aísla la
lógica de negocio de la tecnología que la rodea, a través de interfaces
(puertos) que la infraestructura implementa (adaptadores).
→ `03-hexagonal/01-conceptos-fundamentales.md`

**Invariante** — Regla de negocio que debe cumplirse siempre en cualquier
estado válido de un aggregate. → `01-ddd/01-conceptos-fundamentales.md`

**Proyección (Projection / Read Model)** — Vista derivada de los eventos,
optimizada para lecturas rápidas. En este proyecto: las tablas
`chats`/`messages`/`contacts`, mantenidas por `ChatProjector`.
→ `02-event-sourcing/03-cqrs-y-la-proyeccion.md`

**Puerto (Port)** — Interfaz que el dominio necesita, definida en su propio
vocabulario, sin mencionar tecnología concreta. → `03-hexagonal/01-conceptos-fundamentales.md`

**Replay** — El proceso de reconstruir el estado de un aggregate
reproduciendo, en orden, todos sus eventos históricos.
→ `02-event-sourcing/01-conceptos-fundamentales.md`

**Value Object** — Objeto sin identidad propia, donde solo importa el valor
que representa; dos value objects con el mismo valor son intercambiables.
→ `01-ddd/03-value-objects-y-por-que-importan.md`

**Versión (de un evento)** — Posición de un evento dentro de la secuencia
de su aggregate (1, 2, 3...), usada para garantizar orden y para detectar
conflictos de concurrencia. → `02-event-sourcing/04-concurrencia-optimista.md`
