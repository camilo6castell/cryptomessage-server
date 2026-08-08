# Autoevaluación final — simulando preguntas de entrevista

Estas preguntas mezclan los tres temas a propósito, como probablemente
aparecerían en una conversación técnica real, no organizadas prolijamente
por patrón. Intenta responderlas en voz alta, como si estuvieras en la
entrevista — no solo pensarlas. La diferencia entre "sé la respuesta" y
"puedo explicarla con claridad bajo presión" es real, y vale la pena
practicarla.

No hay respuestas escritas aquí a propósito. Si te trabas, el documento
correspondiente de las carpetas anteriores tiene todo lo que necesitas —
identificar *cuál* releer ya es parte útil del ejercicio.

## Nivel 1 — Conceptos base

1. Explica Domain-Driven Design, Event Sourcing y Hexagonal Architecture a
   alguien que sabe programar pero nunca ha oído estos términos. Usa
   ejemplos de tu propio proyecto.
2. ¿Por qué estos tres patrones aparecen juntos tan seguido? ¿Podrías usar
   uno sin los otros dos?
3. ¿Cuál es la diferencia entre una entidad y un value object? Da un
   ejemplo de cada uno en cryptomessage.

## Nivel 2 — Aplicación al proyecto

4. Camina paso a paso qué pasa, técnicamente, desde que llega
   `POST /api/v1/chats/{chatId}/accept` hasta que el frontend recibe la
   respuesta.
5. ¿Por qué el aggregate `Chat` no guarda la lista completa de mensajes en
   memoria? ¿Qué SÍ necesita guardar, y por qué exactamente eso y no más?
6. Explica cómo funciona el control de concurrencia optimista en este
   proyecto, sin usar la palabra "lock".
7. ¿Qué es una proyección, y por qué las tablas `chats`/`messages` que ya
   existían antes de esta migración terminaron cumpliendo ese rol?

## Nivel 3 — Decisiones y trade-offs

8. Este proyecto NO aplicó DDD/Event Sourcing a `AppUser` ni a `Contact`.
   Justifica esa decisión con el mismo criterio que usarías para decidirlo
   en un proyecto nuevo.
9. `ChatId` es un `Long`, no un UUID — ¿por qué, y qué tuvo que construirse
   de más para lograrlo?
10. `ChatProjector` es una dependencia directa de la capa de aplicación,
    técnicamente "prohibida" por Hexagonal estricto. Defiende por qué esa
    excepción no compromete el propósito real de la regla.
11. Event Sourcing dice "nunca borres nada". Este proyecto sí borra datos.
    Explica la tensión y cómo se resolvió, con el detalle suficiente para
    que quede claro que no es una contradicción ignorada, sino una decisión
    consciente.

## Nivel 4 — Preguntas de "y si..." (para pensar más allá del código actual)

12. Si tuvieras que agregar la funcionalidad de **editar un mensaje ya
    enviado**, ¿qué evento nuevo agregarías? ¿Qué le pasaría al invariante
    de "un mensaje mientras el chat está pendiente"? ¿Tendrías que cambiar
    algo del lado de la proyección?
13. Si el negocio pidiera "quiero saber cuántos chats se crean por hora, en
    tiempo real, para un dashboard interno" — ¿cómo usarías lo que ya existe
    (el event store) para construir eso, sin tocar el aggregate `Chat` en
    absoluto?
14. Si mañana tuvieras que migrar el event store de JPA/MySQL a otra
    tecnología (por ejemplo, un event store especializado como
    EventStoreDB), ¿qué archivos tendrías que tocar? ¿Qué archivos
    garantiza Hexagonal que NO tendrías que tocar?
15. Explica por qué la arquitectura package-based de este proyecto no sería
    automáticamente "incorrecta" si decidieras nunca convertirla a
    multi-módulo — ¿qué tendría que ser cierto para que package-based +
    ArchUnit siga siendo una elección defendible a largo plazo, y qué
    señales te dirían que ya es momento de migrar a multi-módulo?

## Cómo usar esta lista para practicar de verdad

Una técnica que funciona mejor que simplemente leer las preguntas: elige
tres al azar, ponte un temporizador de 3 minutos por pregunta, y
respóndelas en voz alta sin mirar el código ni los apuntes. Grábate si
puedes — es incómodo la primera vez, pero es la forma más parecida a una
entrevista real de detectar en qué parte tu explicación se vuelve confusa
o insegura, que es exactamente donde vale la pena reforzar.
