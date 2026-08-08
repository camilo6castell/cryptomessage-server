# Los límites de DDD en este proyecto (y en general)

Saber cuándo un patrón NO aplica es, en una entrevista, tan valioso como
saber aplicarlo — demuestra que lo entendiste de verdad y no lo estás
usando por moda. Este proyecto es un buen caso de estudio porque
**deliberadamente no aplicamos DDD/ES a todo**, y la razón de esa decisión
es más instructiva que la implementación misma.

## `AppUser`: por qué se quedó fuera

`AppUser` sigue siendo una entidad JPA plana — sin aggregate root propio en
el sentido de Event Sourcing, sin eventos de dominio. Esto fue una decisión
explícita, no un olvido.

La pregunta que hay que hacerse para decidir si algo merece el tratamiento
completo de DDD (con Event Sourcing) es: **¿el historial de cómo llegó a su
estado actual tiene valor real, más allá del estado actual mismo?**

Para `AppUser`: el negocio no necesita saber "el usuario cambió su
`publicKey` tres veces antes de la actual" ni "hubo dos intentos fallidos de
registro con este username". Solo importa el estado actual: ¿cuál es la
clave pública ahora? ¿está el username disponible? Modelarlo con Event
Sourcing agregaría la complejidad completa del patrón (reconstrucción por
replay, versionado, un event store) sin ganar nada a cambio.

Compáralo con `Chat`: ahí SÍ importa el historial — "¿cuándo se aceptó este
chat?", "¿quién lo inició?" son preguntas de negocio reales, y la secuencia
de eventos (`ChatCreated` → `MessageSent` → `ChatAccepted`) es información
que un simple "estado actual" no puede reconstruir.

## `Contact`: el caso más sutil

Este es más interesante que `AppUser` porque a primera vista *sí* parece un
candidato razonable — al final y al cabo, se crea como reacción a un evento
de dominio (`ChatAccepted`).

Pero fíjate en la naturaleza de `Contact`: no tiene invariantes propios que
proteger, ni transiciones de estado propias. Es puramente un **hecho
derivado**: "estos dos usuarios tienen un chat aceptado entre ellos". No
existe una operación de negocio "aceptar un contacto" o "rechazar un
contacto" con reglas propias — su ciclo de vida entero está subordinado al
de `Chat`.

Cuando un concepto no tiene invariantes propios que solo él puede proteger,
convertirlo en su propio aggregate (con su propio event stream) es
sobre-ingeniería: agregas la maquinaria completa de DDD/ES para modelar algo
que, en esencia, es una tabla derivada — que es exactamente lo que sigue
siendo hoy (una fila JPA creada por `ChatProjector` como reacción a
`ChatAccepted`).

## La pregunta que deberías hacerte siempre

Antes de convertir algo en un aggregate con Event Sourcing, pregúntate:

1. **¿Tiene invariantes propios** que ninguna otra parte del sistema podría
   proteger igual de bien?
2. **¿El historial de cómo cambió importa** para el negocio, no solo el
   estado actual?
3. **¿Hay concurrencia real** sobre este concepto que necesite protegerse
   (dos procesos modificándolo al mismo tiempo)?

Si la respuesta a las tres es "no", probablemente no necesita ni DDD táctico
completo ni Event Sourcing — una entidad JPA sencilla, bien diseñada, es
suficiente y más barata de mantener.

## Una trampa común: aplicar DDD "porque sí"

Es fácil, después de aprender estos patrones, querer aplicarlos a todo el
sistema por consistencia estética. Resístete a esa tentación. Un sistema
donde CADA entidad es un aggregate con su propio event stream no es "más
profesional" — es, en la mayoría de los casos, sobreingeniería que ralentiza
el desarrollo sin proteger nada que valiera la pena proteger.

La señal de madurez real, la que un entrevistador senior reconoce, no es
"apliqué DDD a todo mi proyecto" — es "apliqué DDD donde el dominio lo pedía,
y puedo explicar por qué no lo apliqué en el resto".

## Autoevaluación

1. Si el negocio de cryptomessage cambiara y necesitara "auditoría completa
   de todos los cambios de contraseña de un usuario, con fecha, para
   cumplimiento regulatorio" — ¿cambiaría tu respuesta sobre si `AppUser`
   debería ser event-sourced? ¿Por qué sí o por qué no?
2. Explica con tus propias palabras por qué `Contact` "no tener invariantes
   propios" es una razón más fuerte para no convertirlo en aggregate que
   simplemente "es más simple así".
3. Piensa en un proyecto tuyo (o hipotético) donde aplicar DDD/ES a todo
   sería sobreingeniería. ¿Qué parte SÍ lo ameritaría, si alguna?
