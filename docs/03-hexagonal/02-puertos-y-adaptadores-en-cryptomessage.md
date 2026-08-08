# Puertos y adaptadores en cryptomessage: los tres casos reales

Este documento recorre cada uno de los tres puertos de salida del proyecto,
con su adaptador correspondiente, y explica por qué cada uno se decidió
así — porque las tres decisiones no son idénticas entre sí, y esa variedad
es justo lo interesante.

## 1. `ChatAggregateRepository` — el puerto "canónico"

```java
public interface ChatAggregateRepository {
    Optional<Chat> findById(ChatId chatId);
    void save(Chat chat);
}
```

Este es el ejemplo de libro de texto: el dominio necesita persistir y
recuperar su propio aggregate, sin saber cómo. Su único adaptador,
`ChatAggregateRepositoryAdapter`, traduce esas dos operaciones a un event
store real sobre JPA (ver `02-event-sourcing/02-el-event-store-de-cryptomessage.md`
para el detalle completo).

**Por qué necesita un puerto de verdad, no solo una clase concreta**: porque
la tecnología detrás es genuinamente reemplazable — mañana podría ser
MongoDB, EventStoreDB, o cualquier otra cosa, sin que ningún caso de uso
necesite cambiar una línea.

## 2. `NotificationPort` — el puerto más simple, con la lección más clara

```java
public interface NotificationPort {
    void notifyUser(String username, String destination, Object payload);
}
```

Su único adaptador:

```java
@Component
public class StompNotificationAdapter implements NotificationPort {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }
}
```

Este puerto es casi trivial — literalmente envuelve una sola llamada a
`SimpMessagingTemplate`. Vale la pena preguntarse: ¿por qué molestarse en
crear una interfaz para esto?

La respuesta tiene dos partes:

1. **Testabilidad real.** Sin el puerto, cualquier test de `SendMessageUseCase`
   necesitaría un `SimpMessagingTemplate` real (o un mock elaborado de una
   clase de Spring, con toda su complejidad). Con el puerto, un test puede
   pasar una implementación falsa de `NotificationPort` en dos líneas.
2. **El dominio nunca debería saber que WebSocket existe.** Aunque hoy
   `notifyUser` es una envoltura simple, el punto es que "cómo se entera el
   usuario de algo" es una decisión de *entrega*, no una decisión de
   *negocio*. `SendMessageUseCase` no debería tener que saber si la
   notificación llega por WebSocket, push notification, o un correo — solo
   le importa que "el destinatario se entere".

## 3. `ChatIdGenerator` — el puerto más inusual, y el que enseña más

```java
public interface ChatIdGenerator {
    ChatId nextId();
}
```

Este puerto existe por una razón muy específica de este proyecto, no por
seguir la receta al pie de la letra. Repasa por qué en
`05-decisiones-de-diseno/01-por-que-chatid-es-long-no-uuid.md` si no lo
tienes fresco — en resumen: el frontend espera `chatId` como `Long`, así que
generar un id nuevo requiere una consulta a base de datos (una tabla
`chat_id_sequences` dedicada, solo para "quemar" un valor único de
`AUTO_INCREMENT`).

```java
@Component
public class JpaChatIdGenerator implements ChatIdGenerator {
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatId nextId() {
        ChatIdSequence saved = sequenceRepository.save(new ChatIdSequence());
        return ChatId.of(String.valueOf(saved.getId()));
    }
}
```

La lección aquí: **generar un identificador, cuando requiere I/O (una
consulta a base de datos), es una operación de infraestructura, no una
operación pura de dominio.** Por eso `Chat.create()` no genera su propio id
internamente (como sí hacía una versión anterior de este diseño, con
`ChatId.generate()` produciendo un UUID aleatorio en memoria, sin ningún
I/O) — recibe el id ya minteado desde afuera, por el caso de uso, que a su
vez se lo pidió a este puerto.

## Patrón que se repite en los tres: el dominio pide, nunca busca

En los tres casos, fíjate en la dirección de la llamada: es siempre el
**caso de uso** (en `application/`) quien pide algo al puerto, nunca el
aggregate pidiéndoselo directamente a sí mismo desde dentro. `Chat.java` no
tiene ninguna referencia a `ChatAggregateRepository`, `NotificationPort` ni
`ChatIdGenerator` — esos tres viven exclusivamente en las firmas de los
métodos de `application/`.

Esto es deliberado y es una regla más estricta que "solo no depender de
infraestructura": ni siquiera los puertos (que son parte del dominio)
deberían inyectarse *dentro* del aggregate. El aggregate se mantiene puro
en el sentido más fuerte posible — ni siquiera conoce la existencia de
mecanismos externos, aunque sean abstractos.

## Autoevaluación

1. ¿Por qué `NotificationPort` vale la pena aunque su único adaptador sea
   una sola línea de código? Da la razón sin usar la palabra "testeable".
2. Explica la diferencia entre "el dominio depende de una abstracción
   (puerto)" y "el dominio depende de una implementación concreta (adaptador)"
   usando `ChatIdGenerator` como ejemplo específico.
3. ¿Por qué el aggregate `Chat` nunca recibe ninguno de estos tres puertos
   inyectado directamente, aunque técnicamente podría (son interfaces del
   propio paquete `domain`)?
