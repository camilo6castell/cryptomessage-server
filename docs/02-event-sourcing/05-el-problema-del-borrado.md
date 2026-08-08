# El problema del borrado: cuando Event Sourcing choca con la privacidad

Este es, probablemente, el tema más "de entrevista senior" de todo este
proyecto — porque no tiene una respuesta de libro de texto limpia, y
cryptomessage tuvo que resolverlo de verdad.

## La promesa (y el problema) de "nunca borres nada"

Event Sourcing, en su forma más pura, parte de la idea de que el event log
es un registro histórico inmutable — no editas eventos pasados, no los
borras, punto. Esa inmutabilidad es justamente lo que te da auditoría
confiable: si pudieras borrar eventos, ya no podrías confiar en que el
historial es completo.

Pero cryptomessage tiene una política de retención real, ya existente antes
de esta migración, con una razón de negocio legítima (privacidad — es una
app de mensajería cifrada, donde "nosotros no guardamos tus datos para
siempre" es parte de la propuesta de valor):

```java
// services/Scheduler.java (antes de esta migración)
@Scheduled(cron = "0 0 3 * * *")
public void deleteOldMessages() {
    LocalDateTime limit = LocalDateTime.now().minusDays(15);
    messageRepository.deleteOlderThan(limit);
}
```

Estas dos cosas están en conflicto directo. Si aplicaras Event Sourcing "a
la letra", el event store guardaría los mensajes cifrados **para siempre**,
sin importar que la tabla de lectura (`messages`) sí los borre a los 15
días. El dato "desaparecido" seguiría viviendo, indefinidamente, dentro de
los eventos — contradiciendo exactamente la promesa de privacidad que el
producto hace.

## Por qué esto no es un caso raro — es un problema conocido

Esto tiene nombre en la industria: es una tensión bien documentada entre
Event Sourcing y regulaciones de protección de datos (como el "derecho al
olvido" de GDPR en Europa). Hay dos estrategias típicas para resolverlo:

1. **Crypto-shredding**: en vez de borrar el evento, borras solo la clave
   de cifrado que lo hacía legible. El evento sigue existiendo
   estructuralmente (preservando el conteo, el orden, la auditoría de "algo
   pasó aquí"), pero su contenido queda irrecuperable para siempre. Es una
   técnica elegante para cuando SÍ necesitas mantener el hecho de que algo
   ocurrió, pero no su contenido.

2. **Borrado físico selectivo**: aceptas romper la pureza del patrón y
   borras las filas del event store directamente, igual que harías con
   cualquier tabla — sacrificando parte del ideal de inmutabilidad a cambio
   de cumplir con la política real del producto.

## La decisión que se tomó en cryptomessage, y por qué

Se eligió la segunda opción, pero de forma quirúrgica, no en bloque:

```java
// services/Scheduler.java (después de la migración)
@Scheduled(cron = "0 0 3 * * *")
@Transactional
public void deleteOldMessages() {
    LocalDateTime limit = LocalDateTime.now().minusDays(15);
    messageRepository.deleteOlderThan(limit);

    storedEventRepository.deleteByEventTypeAndOccurredOnBefore(
        MessageSent.class.getName(), limit
    );
}
```

Fíjate en el detalle importante: **solo se borran eventos `MessageSent`**,
no `ChatCreated` ni `ChatAccepted` del mismo chat. Esto respeta exactamente
la semántica que ya tenía `deleteOldMessages()` desde antes — borra
mensajes viejos, no borra el chat en sí. El chat sigue existiendo, sigue
siendo válido, solo que sus mensajes más antiguos ya no están ni en la
tabla de lectura ni en el event store.

Para el caso de chats vacíos (30 días), la lógica es un poco distinta
porque ahí sí se borra el chat completo:

```java
public void deleteEmptyChats() {
    LocalDateTime limit = LocalDateTime.now().minusDays(30);

    var emptyChatIds = chatRepository.findEmptyChatsOlderThan(limit)
            .stream()
            .map(chat -> String.valueOf(chat.getChatId()))
            .toList();

    chatRepository.deleteEmptyChatsOlderThan(limit);

    emptyChatIds.forEach(storedEventRepository::deleteByAggregateId);
}
```

Aquí sí se borra el stream de eventos completo (`deleteByAggregateId`) —
pero solo para chats que nunca llegaron a tener ni un solo mensaje, así que
no hay ninguna tensión adicional: no hay "historia de negocio" real que
proteger en un chat vacío.

## Lo que esto te enseña sobre aplicar patrones en el mundo real

La lección de fondo, la que vale la pena que puedas articular en una
entrevista: **ningún patrón de arquitectura es más importante que los
requisitos reales del producto que estás construyendo.** Aplicar Event
Sourcing "de manual" (nunca borrar nada) habría sido, en este caso
concreto, una regresión de producto — habría roto silenciosamente una
promesa de privacidad que ya existía. Reconocer esa tensión, y resolverla
de forma explícita y documentada (en vez de ignorarla o aplicar el patrón
ciegamente), es exactamente el tipo de juicio que distingue a alguien que
entiende un patrón de alguien que solo lo sigue al pie de la letra.

## Un caso borde que esto no cubre (y cómo se resolvió)

Todo lo anterior asume que el chat sigue existiendo y solo pierde mensajes
viejos. Hay un caso borde real donde purgar el único mensaje de un chat
`PENDING` dejaba al aggregate en un estado inconsistente — ver
`05-decisiones-de-diseno/03-chats-pendientes-abandonados.md` para el
detalle completo y la corrección aplicada.

## Autoevaluación

1. Explica, sin ver el código, la diferencia entre crypto-shredding y
   borrado físico como estrategias para resolver la tensión entre Event
   Sourcing y políticas de retención de datos. ¿Cuándo elegirías una sobre
   la otra?
2. ¿Por qué `deleteOldMessages()` borra solo eventos `MessageSent`, y no el
   stream completo del chat al que pertenecen esos mensajes?
3. Si cryptomessage necesitara en el futuro una auditoría regulatoria real
   (por ejemplo, "demostrar cuántos mensajes se enviaron por día, sin
   revelar su contenido"), ¿cómo cambiaría esto tu respuesta sobre qué
   estrategia de borrado usar?
