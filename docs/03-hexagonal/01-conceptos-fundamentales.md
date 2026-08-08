# Arquitectura Hexagonal — Conceptos fundamentales

## El nombre es la peor parte de este patrón

"Hexagonal" no significa nada especial sobre hexágonos — el nombre viene de
cómo Alistair Cockburn dibujó el diagrama original (un hexágono, elegido
solo porque daba espacio visual para dibujar varios lados/puertos
alrededor, no porque el número seis importe). El nombre técnicamente más
preciso, y el que vas a ver usado indistintamente, es **Ports and Adapters**
(Puertos y Adaptadores) — ese sí describe la idea real.

## La pregunta que responde

¿Qué pasa si mañana cambias de base de datos? ¿O si quieres probar tu lógica
de negocio sin levantar Spring Boot completo? ¿O si necesitas exponer la
misma lógica por HTTP *y* por un consumidor de mensajería, sin duplicar
código?

Si tu lógica de negocio llama directamente a `JpaRepository`, a
`SimpMessagingTemplate`, o depende de anotaciones de Spring para funcionar,
la respuesta a todas esas preguntas es "con dificultad, y tocando el código
del dominio". Hexagonal existe para que la respuesta sea "cambias un
adaptador, el dominio ni se entera".

## Puerto: el contrato, no la implementación

Un **puerto** es una interfaz que el dominio *necesita*, definida en
términos del dominio — no en términos de la tecnología que eventualmente la
va a resolver.

```java
// domain/chat/port/ChatAggregateRepository.java
public interface ChatAggregateRepository {
    Optional<Chat> findById(ChatId chatId);
    void save(Chat chat);
}
```

Fíjate en lo que esta interfaz **no** dice: no menciona JPA, no menciona SQL,
no menciona "tabla `events`". Solo dice, en el vocabulario del dominio,
"necesito poder buscar un Chat por su id, y necesito poder guardarlo". Eso
es exactamente lo que hace posible que el dominio (`domain/`) no dependa de
Spring ni de JPA en absoluto — nunca importa una clase concreta de
infraestructura, solo esta interfaz.

Hay dos tipos de puertos, y vale la pena distinguirlos:

- **Puertos de entrada** (*driving/primary ports*): cómo el mundo exterior
  le pide cosas al dominio. En este proyecto no hay una interfaz explícita
  para esto — los casos de uso (`application/`) cumplen ese rol
  directamente, siendo ellos mismos el punto de entrada al dominio.
- **Puertos de salida** (*driven/secondary ports*): lo que el dominio
  necesita del mundo exterior para hacer su trabajo. `ChatAggregateRepository`,
  `ChatIdGenerator` y `NotificationPort` son los tres puertos de salida de
  este proyecto.

## Adaptador: quien de verdad sabe cómo hacerlo

Un **adaptador** es una implementación concreta de un puerto, que sí sabe
(y depende) de la tecnología real.

```java
// infrastructure/eventstore/ChatAggregateRepositoryAdapter.java
@Component
public class ChatAggregateRepositoryAdapter implements ChatAggregateRepository {
    private final StoredEventRepository storedEventRepository;  // JPA, sin vergüenza
    private final EventSerializer eventSerializer;               // Jackson, sin vergüenza

    @Override
    public Optional<Chat> findById(ChatId chatId) {
        // aquí SÍ hay SQL, JPA, Jackson — todo lo que el puerto ocultaba
    }
}
```

Este archivo puede importar lo que quiera de Spring/JPA/Jackson — está en
`infrastructure/`, cuyo trabajo es precisamente saber cómo hablar con el
mundo real. La regla de dependencia es unidireccional: `infrastructure`
puede depender de `domain` (implementa sus interfaces), pero `domain` nunca
puede depender de `infrastructure`.

## El punto que más se malentiende: la inyección de dependencias invierte quién depende de quién

Aquí está el truco conceptual central de Hexagonal, el que le da sentido a
todo lo demás: **en tiempo de compilación, el dominio no sabe nada del
adaptador. En tiempo de ejecución, Spring conecta ambos.**

```java
// application/chat/SendMessageUseCase.java (simplificado)
public class SendMessageUseCase {
    private final ChatAggregateRepository chatAggregateRepository; // ← el PUERTO

    public SendMessageUseCase(ChatAggregateRepository chatAggregateRepository) {
        this.chatAggregateRepository = chatAggregateRepository;
    }
    // este código nunca menciona "ChatAggregateRepositoryAdapter"
}
```

`SendMessageUseCase` recibe un `ChatAggregateRepository` por el
constructor — no le importa, ni puede saber, si por debajo hay JPA, MongoDB,
o un mapa en memoria para un test. Spring, al arrancar la aplicación, ve que
`ChatAggregateRepositoryAdapter` es un `@Component` que implementa esa
interfaz, y lo inyecta automáticamente. Esa es la "inversión de
dependencias" (la D de SOLID) funcionando en la práctica: el módulo de alto
nivel (el caso de uso) no depende del módulo de bajo nivel (el adaptador
JPA) — ambos dependen de una abstracción (el puerto).

## Autoevaluación

1. Explica con tus propias palabras por qué el nombre "Puertos y
   Adaptadores" describe mejor el patrón que "Hexagonal".
2. `NotificationPort` es un puerto de salida. Si mañana cryptomessage
   quisiera enviar notificaciones push además de (o en vez de) WebSocket,
   ¿qué archivos tendrías que crear, y qué archivos NO tendrías que tocar?
3. ¿Por qué decimos que `application/` "sabe" del puerto pero no del
   adaptador, si en tiempo de ejecución igual termina usando el adaptador
   real? ¿Dónde ocurre esa conexión, si no es en el código del caso de uso?
