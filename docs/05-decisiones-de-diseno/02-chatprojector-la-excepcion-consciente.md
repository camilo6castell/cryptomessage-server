# `ChatProjector`: la excepción consciente a "application solo depende de dominio"

Ya viste esto de pasada en varios documentos anteriores. Este lo trata en
profundidad porque es, probablemente, la decisión más "debatible" de todo
el proyecto — la que más fácil sería criticar sin entender el razonamiento
completo detrás. Vale la pena que puedas defenderla con seguridad.

## La regla que "debería" existir, según Hexagonal estricto

En una lectura purista de Hexagonal, la capa de aplicación (los casos de
uso) solo debería depender de: el dominio, y los puertos del dominio. Nunca
de una clase concreta de infraestructura.

Pero mira `SendMessageUseCase`:

```java
private final ChatProjector chatProjector;   // ← infrastructure.projection.ChatProjector

// ...
Message message = chatProjector.projectMessageSent(request.chatId(), event);
```

`ChatProjector` vive en `infrastructure/projection/` y es una clase
concreta, no una interfaz. `SendMessageUseCase` la inyecta y la llama
directamente — exactamente el tipo de dependencia que un Hexagonal
"de manual" prohibiría.

## Por qué no se convirtió en un puerto más

La pregunta correcta no es "¿esto viola la regla?" (sí, la viola,
literalmente). La pregunta correcta es "¿el motivo por el que existen los
puertos aplica aquí?"

Los puertos existen para proteger contra **cambio de tecnología real**: la
razón de ser de `ChatAggregateRepository` es que mañana el event store
podría dejar de ser JPA/MySQL y pasar a ser otra cosa, sin que
`SendMessageUseCase` tenga que cambiar. Esa es una posibilidad real y
razonable.

Ahora pregúntate lo mismo sobre `ChatProjector`: ¿hay algún escenario
razonable donde mañana necesites una implementación *distinta* de "actualizar
las tablas `chats`/`messages`/`contacts` en reacción a un evento"? La
respuesta es no, por una razón estructural, no por pereza: **`ChatProjector`
existe únicamente para mantener sincronizadas dos tablas que viven en la
misma base de datos.** No hay una segunda implementación razonable de eso —
o mantienes esas tablas sincronizadas con el event store, o el sistema deja
de funcionar correctamente. No es una pieza intercambiable; es una
consecuencia directa, casi mecánica, de la decisión de reutilizar las
tablas existentes como modelo de lectura (ver
`02-event-sourcing/03-cqrs-y-la-proyeccion.md`).

Introducir una interfaz `ReadModelProjector` con una sola implementación
posible para siempre no te protegería de nada real — solo agregaría una
capa de indirección que alguien tendría que atravesar mentalmente cada vez
que lea el código, sin ganar la flexibilidad que las interfaces normalmente
compran.

## Esto tiene nombre: sobreingeniería por consistencia estética

Hay una tentación común, sobre todo después de aprender un patrón nuevo, de
aplicarlo *en todos lados* por coherencia visual del código — "todo lo que
toca infraestructura debería pasar por un puerto, siempre, sin excepción".
Esa tentación ignora que el propósito de un puerto no es "verse bien" o
"seguir la regla" — es dar una ventaja real (poder cambiar de tecnología sin
tocar el dominio). Cuando esa ventaja no existe, forzar el patrón de todas
formas es puro costo, sin beneficio.

## Cómo se documentó esta decisión, en vez de esconderla

Lo importante no es solo tomar esta decisión bien razonada — es dejarla
visible para quien lea el código después (incluido tú mismo, en seis
meses). Por eso está explícita en tres lugares distintos, cada uno
apropiado para su audiencia:

1. **En el código mismo**, como comentario en `ChatProjector` y en los casos
   de uso que lo usan.
2. **En el test de ArchUnit** (`ApplicationLayerTest`), que verifica que
   *esta* excepción es la única permitida — cualquier otra dependencia
   directa de `application` hacia `infrastructure` (por ejemplo, hacia
   `ChatAggregateRepositoryAdapter` en vez del puerto) sigue prohibida y
   rompe el build.
3. **Aquí, en los apuntes**, con el razonamiento completo — porque el
   comentario en el código puede decir *qué* se decidió, pero un documento
   de estudio puede darse el lujo de explicar *por qué*, con calma.

## La lección más grande de este documento

Un entrevistador senior no está buscando que sigas reglas de arquitectura
al pie de la letra sin excepción — eso, de hecho, suele ser señal de
alguien que memorizó un patrón sin entenderlo. Lo que sí impresiona es
poder decir: "aquí rompí la regla general, y esta es la razón específica,
concreta y limitada por la que romperla en este caso no compromete lo que
la regla protege". Eso es exactamente lo que este documento — y el código
que describe — te da para poder decir en una entrevista.

## Autoevaluación

1. Formula, con tus propias palabras y sin ver este documento, la pregunta
   que hay que hacerse para decidir si una dependencia "prohibida" merece
   una excepción documentada o si en realidad es un descuido real.
2. ¿Qué pasaría si mañana el proyecto SÍ necesitara una segunda forma de
   actualizar la proyección (por ejemplo, un job batch que reconstruye toda
   la tabla `messages` desde cero, reproduciendo todo el event store)?
   ¿Cambiaría eso tu respuesta sobre si `ChatProjector` merece convertirse
   en un puerto?
3. Piensa en un caso, de cualquier proyecto (no necesariamente este), donde
   introducir una interfaz "por si acaso" resultó ser complejidad sin
   beneficio real. ¿Cómo lo reconocerías la próxima vez, antes de escribir
   el código?
