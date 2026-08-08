# DDD — Conceptos fundamentales

## La idea central, sin rodeos

Domain-Driven Design parte de una observación simple: **el software que
modela mal un dominio complejo se vuelve imposible de mantener**, sin
importar cuán "limpio" esté el código a nivel técnico. La solución que
propone no es una tecnología — es una forma de pensar el diseño donde el
*lenguaje y las reglas del negocio* mandan sobre la estructura del código, no
al revés.

Esto suena abstracto, así que vayamos directo a las piezas concretas.

## Entidad (Entity)

Una **entidad** es algo que tiene identidad propia y continuidad en el
tiempo, incluso si sus atributos cambian. Dos entidades con exactamente los
mismos datos siguen siendo *distintas* si tienen identidades distintas.

En este proyecto: un `Message` es una entidad. Dos mensajes podrían tener,
en teoría, el mismo remitente y el mismo contenido cifrado — pero siguen
siendo dos mensajes distintos porque tienen `MessageId` distintos.

```java
// domain/generic/Entity.java
public abstract class Entity<I extends Identity> {
    private final I id;
    // igualdad basada en id, no en el resto de los campos
}
```

## Value Object

Un **value object** es lo opuesto: no tiene identidad propia, solo importa
*qué valor representa*. Dos value objects con el mismo valor son
intercambiables — de hecho, deberían ser considerados iguales.

En este proyecto: `UserId` es un value object. `UserId.of(5L)` creado en dos
lugares distintos del código son "el mismo" — no hay una noción de "cuál
`UserId(5)` es el original".

```java
// domain/shared/UserId.java
public final class UserId implements IValueObject<Long> {
    private final Long id;
    // equals()/hashCode() basados en el valor, no hay concepto de "identidad propia"
}
```

**Una prueba mental útil**: si reemplazar un objeto por otro con el mismo
valor no cambia nada del significado del sistema, es un value object. Si
sí importa cuál instancia específica es (aunque los datos sean iguales), es
una entidad.

## Aggregate y Aggregate Root

Este es el concepto más importante y el más malentendido de los tres.

Un **aggregate** es un grupo de entidades y value objects que se tratan como
una sola unidad *para efectos de consistencia*. El **aggregate root** es la
única entidad del grupo a través de la cual el mundo exterior puede
interactuar con ese grupo.

La regla dura de DDD es: **nada fuera del aggregate puede modificar algo
dentro de él directamente.** Toda modificación pasa por el aggregate root,
que decide si es válida.

En este proyecto: `Chat` es el aggregate root. Conceptualmente, un mensaje
("Message") es parte del aggregate `Chat` — no tiene sentido hablar de "el
mensaje" sin el contexto de a qué chat pertenece y en qué estado estaba ese
chat cuando se envió. Por eso **no existe** un `MessageRepository` en el
dominio, ni un caso de uso "editar un mensaje directamente" — todo pasa por
`Chat.sendMessage()`.

```java
// domain/chat/Chat.java
public MessageId sendMessage(UserId senderId, Map<UserId, String> encryptedContentByUser) {
    assertParticipant(senderId);
    if (status == ChatStatus.PENDING) {
        if (!senderId.equals(initiatedBy)) {
            throw new ChatNotAcceptedException("Chat not accepted yet");
        }
        // ...
    }
    // ...
}
```

Nadie llama `new Message(...)` desde fuera del aggregate esperando que eso
"cuente" como un mensaje válido — bueno, casi nadie: hablamos de esa
excepción específica (`ChatProjector`) en `05-decisiones-de-diseno/`.

### ¿Por qué esta regla tan estricta importa?

Porque es la única forma de garantizar invariantes que involucran a *más de
un objeto a la vez*. La regla "solo se permite un mensaje mientras el chat
está PENDING" no es una propiedad de `Message` por sí solo — es una relación
entre el `Message` que se está creando y el estado actual del `Chat`. Si
`Message` pudiera crearse independientemente y luego "adjuntarse" a un chat,
no hay forma de garantizar esa regla de manera centralizada — cualquier capa
que se le olvide verificarla, la rompe.

## Invariante

Un **invariante** es una regla que debe ser verdadera siempre, en cualquier
estado válido del aggregate. No es una validación de formulario — es una
regla de negocio que, si se rompe, el sistema entró en un estado que no
debería ser posible.

Ejemplos reales en `Chat`:

- Un usuario no puede iniciar un chat consigo mismo.
- Mientras el chat está `PENDING`, solo el iniciador puede mandar mensajes,
  y solo puede mandar uno.
- Solo un participante del chat puede aceptarlo, y no puede ser quien lo
  inició.

Cada uno de estos está codificado como una verificación explícita dentro de
un método del aggregate, nunca fuera de él:

```java
public void accept(UserId acceptingUserId) {
    assertParticipant(acceptingUserId);
    if (status != ChatStatus.PENDING) {
        throw new ChatNotPendingException("Chat is not pending");
    }
    if (acceptingUserId.equals(initiatedBy)) {
        throw new SelfAcceptanceException("Initiator cannot accept their own chat");
    }
    appendEvent(new ChatAccepted());
}
```

## Ubiquitous Language (lenguaje ubicuo)

Un pilar de DDD que es fácil pasar por alto: los nombres en el código deben
ser *los mismos* nombres que usaría un experto del negocio hablando del
problema, no una traducción técnica. Si en una conversación de negocio
dirías "el chat está pendiente hasta que lo acepten", el código debería
decir literalmente `ChatStatus.PENDING` y `Chat.accept()` — no
`Chat.setFlag(true)` ni `Chat.updateState(2)`.

Esto no es estética. Cuando el código y el lenguaje del negocio divergen,
cada conversación entre alguien técnico y alguien no técnico requiere una
"traducción" mental, y esas traducciones acumulan errores con el tiempo.

## Autoevaluación

1. Explica con un ejemplo propio (no de este proyecto) la diferencia entre
   una entidad y un value object.
2. `AppUser` en este proyecto NO es un aggregate root en el sentido de DDD
   con Event Sourcing (es una entidad JPA plana). ¿Eso significa que no
   aplican conceptos de DDD sobre `AppUser` en absoluto? Justifica tu
   respuesta.
3. ¿Por qué la regla "nada fuera del aggregate puede modificarlo
   directamente" es más difícil de mantener en un ORM tradicional (donde
   cualquiera puede hacer `entity.setField(x); repository.save(entity)`) que
   en un diseño con Event Sourcing?
