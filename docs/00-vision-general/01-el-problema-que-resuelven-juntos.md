# ¿Por qué DDD, Event Sourcing y Hexagonal aparecen juntos tan seguido?

No es casualidad ni moda. Cada uno resuelve un problema distinto, pero los
tres problemas suelen aparecer en el mismo tipo de sistema — y por eso, en la
práctica, casi nunca ves uno sin los otros dos cerca.

## Tres preguntas distintas

Piensa en estas como tres preguntas independientes que cualquier sistema
backend con lógica de negocio no trivial tiene que responder:

1. **¿Dónde vive la verdad sobre las reglas de negocio?**
   Esto es lo que responde **DDD**. Sin él, las reglas ("solo el iniciador
   puede mandar el primer mensaje", "no puedes aceptar tu propio chat")
   terminan dispersas entre el controller, el service, y a veces hasta en el
   frontend — cada quien reimplementándolas un poco distinto. DDD dice: las
   reglas viven en un solo lugar, el *aggregate*, y todo lo demás le
   pregunta a él, nunca las reimplementa.

2. **¿Cómo guardo lo que pasó, no solo dónde quedó todo?**
   Esto es lo que responde **Event Sourcing**. Un `UPDATE chats SET status =
   'ACCEPTED'` es honesto sobre el *ahora*, pero mentiroso sobre el *cómo
   llegamos aquí* — la fila no dice quién lo aceptó, cuándo, ni si hubo un
   intento fallido antes. Event Sourcing dice: no guardes el resultado final,
   guarda la secuencia de hechos que lo produjo, y calcula el resultado final
   cuando lo necesites.

3. **¿Cómo evito que mi lógica de negocio dependa de la tecnología que la
   rodea?**
   Esto es lo que responde **Hexagonal**. Si `Chat.sendMessage()` llama
   directamente a un `JpaRepository`, entonces tu regla de negocio ("no más
   de un mensaje mientras el chat está pendiente") queda atada a Spring Data
   JPA — no puedes probarla sin levantar una base de datos, ni cambiar de
   persistencia sin tocar la regla. Hexagonal dice: el dominio solo conoce
   *interfaces* (puertos); la tecnología concreta vive afuera, detrás de un
   adaptador.

## Por qué se necesitan mutuamente

Aquí está la parte que casi nadie explica bien: **Event Sourcing sin DDD no
tiene mucho sentido**, y viceversa.

Si no tienes un aggregate bien definido (DDD), ¿de quién es el evento? ¿Quién
decide cuándo se genera uno? Sin un límite de consistencia claro, "Event
Sourcing" degenera en simplemente loguear cosas que pasaron, sin ninguna
garantía de que sean válidas.

Y si tienes un aggregate (DDD) pero guardas su estado directo en una tabla
(sin ES), pierdes exactamente la ventaja que DDD te prometía: tener un lugar
único que controla las transiciones válidas dejó de importar en el momento
en que otro proceso puede hacer un `UPDATE` directo a la tabla y saltarse
esas reglas por completo.

Hexagonal, por su parte, es casi un *requisito técnico* para que Event
Sourcing funcione bien: tu aggregate necesita reconstruirse desde eventos sin
saber nada de cómo esos eventos llegan a memoria (¿Mongo? ¿Postgres?
¿archivos?) — eso es exactamente lo que un puerto (`ChatAggregateRepository`
en este proyecto) te da.

## En cryptomessage, concretamente

| Pregunta | Respuesta en este proyecto | Dónde está |
|---|---|---|
| ¿Dónde vive la verdad del negocio? | El aggregate `Chat` | `domain/chat/Chat.java` |
| ¿Cómo se guarda lo que pasó? | Como una secuencia de eventos inmutables | `domain/chat/events/`, tabla `events` |
| ¿Cómo se aísla el dominio de la tecnología? | A través de puertos que la infraestructura implementa | `domain/chat/port/`, `infrastructure/` |

Una intuición que te va a servir mucho: **DDD te dice QUÉ proteger. Event
Sourcing te dice CÓMO guardarlo sin perder historia. Hexagonal te dice DÓNDE
trazar la línea para que lo anterior no dependa de framework.**

## Autoevaluación

1. Si solo implementaras DDD sin Event Sourcing ni Hexagonal en un proyecto
   nuevo, ¿qué seguirías ganando? ¿Qué perderías respecto a tenerlos los tres?
2. ¿Por qué decimos que Event Sourcing "necesita" un límite de consistencia
   claro (un aggregate) para tener sentido?
3. Explica con tus propias palabras por qué Hexagonal es casi un
   prerrequisito técnico, no solo estético, para que Event Sourcing funcione
   bien.
