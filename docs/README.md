# Apuntes: DDD, Event Sourcing y Hexagonal en cryptomessage

Esta carpeta es material de estudio, no documentación del proyecto (para eso
está `docs/MIGRATION_NOTES.md`). El objetivo es que puedas repasar estos tres
temas hasta defenderlos con seguridad en una entrevista técnica, usando tu
propio proyecto como caso de estudio en vez de ejemplos genéricos de un
tutorial.

Asumo que ya tienes bagaje de programación — no voy a diluir los conceptos
difíciles. Cuando algo es genuinamente sutil (por ejemplo, por qué el
versionado de eventos no puede vivir dentro del aggregate, o por qué Event
Sourcing choca con el derecho al olvido), lo digo así, sin suavizarlo, y te
doy el razonamiento completo, no solo la conclusión.

## Orden sugerido de lectura

No es obligatorio seguir el orden exacto, pero está pensado como progresión:

1. **`00-vision-general/`** — por qué estos tres patrones aparecen juntos tan
   seguido, y un mapa de qué archivo del proyecto corresponde a qué concepto.
2. **`01-ddd/`** — Domain-Driven Design: agregados, invariantes, value objects.
   Es la base conceptual que los otros dos patrones dan por sentada.
3. **`02-event-sourcing/`** — el cambio de mentalidad de guardar *estado* a
   guardar *hechos*, y todo lo que eso trae consigo (proyecciones,
   concurrencia, el problema del borrado).
4. **`03-hexagonal/`** — puertos y adaptadores, y la decisión concreta que
   tomamos de implementarlo por paquetes en vez de módulos separados.
5. **`04-flujo-completo/`** — sigue una sola petición HTTP (enviar un mensaje)
   línea por línea a través de las tres capas, para que veas cómo se
   ensamblan en la práctica, no solo en teoría.
6. **`05-decisiones-de-diseno/`** — el porqué de cada decisión no obvia que
   tomamos para no romper el frontend, explicado con más profundidad de la
   que cabía en los comentarios del código.

Al final de `01-ddd/`, `02-event-sourcing/` y `03-hexagonal/` hay una sección
corta de **"cuándo NO usar esto"** — deliberadamente, porque saber cuándo un
patrón no aplica es tan importante para una entrevista como saber aplicarlo.

## Cómo usar esto para repasar

Cada documento termina con 2-3 preguntas que deberías poder responder sin
volver a leer el texto. Si te trabas en alguna, esa es la señal de qué
releer. Hay una batería más larga de preguntas de autoevaluación mezclando
los tres temas al final de todo, pensada para simular cómo te podrían
preguntar esto en una entrevista técnica real.
