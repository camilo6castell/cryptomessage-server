# Value Objects: por qué importan más de lo que parece

Es tentador ver `UserId` y pensar "esto es un `Long` con pasos extra". Este
documento es sobre por qué esos "pasos extra" no son ceremonia — son
protección contra una categoría entera de bugs que, sin ellos, el
compilador no puede detectar.

## El problema que resuelven: "primitive obsession"

Imagina esta firma de método sin value objects:

```java
public void sendMessage(Long senderId, Long chatId, Long recipientId) { ... }
```

¿Qué pasa si accidentalmente invocas `sendMessage(chatId, senderId,
recipientId)`? El código **compila perfectamente**. Los tres parámetros son
`Long` — Java no tiene forma de saber que semánticamente son cosas
distintas. El bug solo aparece en tiempo de ejecución, y con suerte con un
mensaje de error que te oriente. Con mala suerte, simplemente asigna el
mensaje al chat equivocado en silencio.

Ahora con value objects:

```java
public void sendMessage(UserId senderId, ChatId chatId, UserId recipientId) { ... }
```

Invertir `senderId` y `chatId` ahora es un **error de compilación**, no un
bug en producción. Eso es lo que compra un value object bien tipado: mueve
una categoría de errores de "runtime, con suerte los detecta un test" a
"no compila, nunca llega a ejecutarse".

## `UserId` en este proyecto

```java
// domain/shared/UserId.java
public final class UserId implements IValueObject<Long> {
    private final Long id;

    private UserId(Long id) {
        this.id = Objects.requireNonNull(id, "UserId cannot be null");
    }

    public static UserId of(Long id) {
        return new UserId(id);
    }
    // equals/hashCode basados en el valor interno
}
```

Fíjate en tres decisiones deliberadas:

1. **Constructor privado + factory estático (`of`)**. Esto no es solo estilo
   — te da un único punto de entrada donde podrías agregar validación futura
   (por ejemplo, rechazar IDs negativos) sin tener que buscar cada
   `new UserId(...)` disperso por el código.

2. **`Objects.requireNonNull`**. Un value object nunca debería poder existir
   en un estado inválido. Si `UserId` pudiera envolver un `null`, cada lugar
   que lo use tendría que volver a verificar null — el value object dejaría
   de ser una garantía.

3. **Inmutable (`final id`, sin setters)**. Un value object que cambia de
   valor después de creado deja de comportarse como un *valor* — empieza a
   comportarse como una entidad con identidad implícita, que es justo la
   distinción que quisimos evitar (ver `01-conceptos-fundamentales.md`).

## Por qué `UserId` vive en `domain/shared/` y no en `domain/chat/`

`UserId` no es un concepto exclusivo de `Chat` — es una referencia genérica
a un `AppUser`, y en teoría cualquier aggregate futuro podría necesitarla
(imagina un futuro aggregate `Contact` con reglas propias). Por eso vive en
un paquete `shared`, separado de `chat`. Esto es una señal de diseño: si un
value object solo lo usa un aggregate, vive dentro del paquete de ese
aggregate; si lo comparten varios, sube un nivel.

## El caso límite: `ChatStatus`

Vale la pena mirar la excepción deliberada a esta regla en el proyecto:
`ChatStatus` (un enum de dos valores, `PENDING`/`ACCEPTED`) **no** vive en
`domain/` — sigue viviendo en `model.entity.chat.ChatStatus`, del lado del
modelo de lectura (JPA), y el dominio lo reutiliza directamente.

Esto podría verse como una violación de la pureza del dominio (¿por qué el
dominio depende de algo del paquete `model`?). La justificación está
documentada directamente en el código:

```java
/**
 * NOTE on reusing ChatStatus: this enum still lives in
 * {@code model.entity.chat.ChatStatus} (...) instead of a new domain-owned
 * type. A "purist" hexagonal layout would duplicate it here and map between
 * the two at the boundary. That duplication buys nothing for a two-value
 * enum and only adds a mapping step that could itself drift out of sync...
 */
```

Esto es una lección de diseño real, no solo una excepción arbitraria: **la
pureza arquitectónica no es gratis, y a veces el costo de mantenerla supera
el beneficio.** Duplicar un enum de dos valores en dos capas distintas no te
protege de ningún bug real — solo te obliga a mantener sincronizados dos
lugares por una regla que, en este caso concreto, no aporta nada. Es una
decisión consciente, no un descuido — y lo notarás porque el test de
ArchUnit (`DomainIndependenceTest`) tiene una excepción explícita, nombrada,
solo para este caso — cualquier otra dependencia del dominio hacia
`model.entity` sigue prohibida y falla el build.

## Autoevaluación

1. Da un ejemplo (de cualquier dominio, no necesariamente este proyecto)
   donde usar un `String` crudo en vez de un value object podría causar un
   bug silencioso en producción.
2. `MessageId` también es una `Identity` (como `ChatId`), pero nunca se
   expone en ningún DTO ni respuesta HTTP. ¿Por qué sigue teniendo sentido
   que exista como tipo, en vez de usar directamente un `String` dentro del
   evento `MessageSent`?
3. ¿Por qué la decisión de reutilizar `ChatStatus` sin duplicarlo NO es lo
   mismo que "ser descuidado con los límites de capas"? ¿Qué la distingue de
   un descuido real?
