# Package-based vs. multi-módulo, y por qué ArchUnit cierra la brecha

## Dos formas legítimas de imponer los mismos límites

Hexagonal te dice *qué* límites debe haber entre capas. No te dice *cómo*
hacer cumplir esos límites — y ahí es donde cryptomessage y Library Provider
(el otro proyecto de este portafolio, con el mismo tipo de arquitectura)
difieren.

**Library Provider: multi-módulo Gradle.** Cada capa es literalmente un
proyecto Gradle distinto (`:model`, `:usecase`, `:mongo-repository`,
`:reactive-web`), con su propio classpath. `:model` ni siquiera *tiene* a
`:reactive-web` como dependencia — es físicamente imposible que el código de
dominio importe algo de infraestructura, porque esa clase no existe en su
classpath en absoluto. El compilador rechaza el `import` antes de que
llegues a ejecutar nada.

**cryptomessage: paquetes dentro de un único módulo.** `domain`,
`application`, `infrastructure` son paquetes Java normales, todos dentro
del mismo proyecto Gradle, todos en el mismo classpath. Nada impide,
mecánicamente, que alguien escriba `import
com.cryptomessage.server.infrastructure.algo;` dentro de una clase de
`domain` — compila sin ningún problema.

## ¿Por qué elegir la opción "más débil" para cryptomessage?

Fue una decisión de **reducir el radio de cambio de esta migración
específica**, no una afirmación de que package-based sea superior en
general. cryptomessage ya era un proyecto de un solo módulo Gradle antes de
esta migración. Convertirlo a multi-módulo *al mismo tiempo* que se
introducía DDD + Event Sourcing + Hexagonal + toda la compatibilidad con el
frontend ya existente habría aumentado el riesgo de una migración que ya
tenía mucho en juego — nada de esto debía romper lo que ya funcionaba.

Esta es una lección de juicio de ingeniería en sí misma: **la arquitectura
"más pura" no siempre es la decisión correcta en el momento correcto.** A
veces el costo de migrar hacia la versión más estricta compite
directamente con el objetivo real de la tarea (en este caso: no romper el
frontend), y hay que elegir cuál de los dos priorizar.

## El costo real de elegir package-based: se paga con disciplina, no con dinero

La garantía que pierdes al no tener módulos separados no desaparece — se
transforma. En vez de que el compilador te proteja automáticamente,
necesitas **algo más** que verifique la misma propiedad. Ese algo, en este
proyecto, es ArchUnit.

## ArchUnit: reglas de arquitectura como tests normales

ArchUnit es una librería que te deja escribir aserciones sobre la
*estructura* de tu código (qué paquete depende de cuál) de la misma forma
que escribirías cualquier test — y esos tests corren como parte normal de
`./gradlew test`, en cada build.

```java
// src/test/java/com/cryptomessage/server/architecture/DomainIndependenceTest.java
@ArchTest
static final ArchRule domain_should_not_depend_on_infrastructure_layer =
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("esto es el sentido entero de domain.chat.port: el dominio "
                        + "debe seguir siendo intercambiable sin saber qué lo implementa");
```

Si alguien (tú mismo, dentro de seis meses, con menos contexto fresco, o
cualquier otra persona que colabore en el proyecto) escribe accidentalmente
un `import` que rompe esta regla, **el build falla**, con un mensaje que
apunta exactamente a la clase y la línea del problema. No es tan fuerte como
que el compilador lo rechace directamente (el código sí compila; falla
después, al correr los tests), pero cumple el mismo propósito práctico: el
problema no llega a producción sin que alguien se entere.

## Lo que hace especial a este set de reglas: reflejan decisiones reales, no un ideal genérico

Vale la pena notar algo sobre cómo están escritas las reglas de ArchUnit en
este proyecto — no son "reglas de Hexagonal de manual" copiadas sin pensar.
Reflejan exactamente las decisiones conscientes que se tomaron:

```java
// application/ApplicationLayerTest.java — el comentario es tan importante como la regla
/**
 * NOTE lo que estas reglas deliberadamente NO prohíben: las clases de
 * application siguen dependiendo directamente de repositories.*,
 * model.entity.* y infrastructure.projection.ChatProjector para lecturas y
 * para actualizar la proyección. Esa es una decisión CQRS intencional, no
 * un descuido...
 */
```

Una regla de ArchUnit copiada de un tutorial genérico ("la capa de
aplicación nunca depende de infraestructura, punto") habría entrado en
conflicto directo con la decisión CQRS ya tomada (que las lecturas van
directo a JPA, sin pasar por un puerto). Escribir la regla real, la que
refleja lo que el proyecto decidió y por qué, es más valioso que copiar una
regla "correcta en abstracto" que no describe tu sistema.

## Autoevaluación

1. Explica la diferencia exacta entre "el compilador impide esto" y
   "ArchUnit detecta esto" — ¿en qué momento del ciclo de desarrollo se
   entera cada uno de una violación?
2. Si este proyecto creciera mucho y necesitara garantías más fuertes que
   ArchUnit, ¿qué cambio de infraestructura (no de reglas) tendrías que
   hacer para llegar al nivel de garantía que tiene Library Provider?
3. ¿Por qué sería un error copiar literalmente las reglas de ArchUnit de
   otro proyecto (por ejemplo, de un tutorial) sin adaptarlas a las
   decisiones reales que tomaste en el tuyo?
