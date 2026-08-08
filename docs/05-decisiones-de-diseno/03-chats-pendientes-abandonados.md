# Event Sourcing no es blockchain, pero tu intuición no estaba del todo mal

Este documento nace de una pregunta real, no de un ejercicio inventado: si
los eventos "están encadenados" como en blockchain, ¿se puede borrar uno
del medio sin romper todo? La respuesta corta es "no están encadenados así,
pero sí encontramos un caso real donde borrar sin pensar rompía algo" — y
vale la pena entender ambas partes.

## Por qué NO es blockchain

En blockchain, cada bloque contiene el **hash del bloque anterior**. Esa es
una garantía criptográfica: si alteras o borras un bloque, el hash que el
siguiente bloque guarda deja de coincidir, y la ruptura es matemáticamente
detectable.

Mira la tabla `events` de este proyecto:

​```java
// infrastructure/eventstore/StoredEvent.java
@Column(name = "aggregate_id")   private String aggregateId;
@Column(name = "version")        private long version;
@Column(name = "payload")        private String payload;
​```

No hay ningún campo tipo `previousEventHash`. El orden es simplemente un
número entero (`version`), verificado con un `ORDER BY version ASC` al
leer. No existe ningún mecanismo que detecte o impida borrar la fila
`version = 7` sin tocar las filas `8`, `9`, `10` — de hecho, exactamente
eso es lo que hace el `Scheduler` todos los días con los eventos
`MessageSent` viejos.

## Pero la intuición apuntaba a algo real: dependencia semántica, no criptográfica

Aunque no hay una cadena de hashes, sí existe una dependencia de
**significado**: reproducir eventos (`Chat.from(...)`) asume que tienes la
secuencia completa que el aggregate necesita para llegar a un estado
correcto. Borrar un evento sin pensar en qué información aportaba puede
producir un estado incorrecto — no porque el sistema te lo impida, sino
porque nadie te avisa, y el resultado queda mal en silencio.

## El caso real que encontramos al hacer esta pregunta

Repasa el invariante de `Chat.sendMessage()`: mientras el chat está
`PENDING`, solo puede existir un mensaje (el del iniciador). Ese hecho se
rastrea con un solo booleano en el aggregate:

​```java
// domain/chat/ChatBehavior.java
addSubscriber(MessageSent.class, event -> {
    if (chat.getStatus() == ChatStatus.PENDING) {
        chat.markPendingMessageSent();
    }
});
​```

El `Scheduler` original purgaba los eventos `MessageSent` con más de 15
días, **sin importar el estado del chat**. Esto es seguro para chats
`ACCEPTED` (revisa `02-event-sourcing/05-el-problema-del-borrado.md` para
el porqué). Pero para un chat que se quedó `PENDING` — nadie lo aceptó — y
cuyo único mensaje cumple 15 días, purgar ese evento dejaba al chat en un
estado inconsistente: seguía existiendo, seguía `PENDING`, pero al
reconstruirse desde sus eventos restantes (solo `ChatCreated`), el
aggregate ya no tenía forma de saber que el iniciador ya había mandado su
mensaje. `hasPendingMessage` volvía a `false` — y el iniciador podría
enviar un segundo "primer mensaje", saltándose el invariante en la
práctica.

La ventana real del bug: entre el día 15 (se purga el evento) y el día 30
(`deleteEmptyChats` eventualmente elimina el chat completo, porque para
entonces su único mensaje ya fue borrado también de la tabla de lectura y
el chat queda "vacío"). Durante esos 15 días intermedios, el chat quedaba
en un limbo inconsistente.

## La corrección

En vez de esperar a que `deleteEmptyChats` (30 días) eventualmente
resolviera el problema por accidente, se adelantó la limpieza: cualquier
chat `PENDING` cuyo mensaje está a punto de purgarse se trata **ya** como
un chat abandonado, y se borra por completo en ese mismo momento — fila de
lectura (con cascada a su mensaje) y stream de eventos completo.

​```java
// repositories/ChatRepository.java — nueva consulta
@Query("""
SELECT DISTINCT c FROM Chat c
JOIN c.messages m
WHERE c.status = :status
AND m.sentAt < :limit
""")
List<Chat> findChatsByStatusWithMessagesOlderThan(
        @Param("status") ChatStatus status,
        @Param("limit") LocalDateTime limit
);
​```

​```java
// services/Scheduler.java — deleteOldMessages(), fragmento
List<Chat> stuckPendingChats = chatRepository.findChatsByStatusWithMessagesOlderThan(
        ChatStatus.PENDING, limit
);
List<String> stuckPendingChatIds = stuckPendingChats.stream()
        .map(chat -> String.valueOf(chat.getChatId()))
        .toList();

chatRepository.deleteAll(stuckPendingChats);   // cascade elimina también su mensaje
stuckPendingChatIds.forEach(storedEventRepository::deleteByAggregateId);
​```

Un detalle técnico que vale la pena notar: se usa `chatRepository.deleteAll(...)`
(borrado basado en la entidad, uno por uno) y no un `@Modifying @Query
DELETE ...` en bloque. La razón es que los `DELETE` en bloque de JPQL
**no disparan cascada ni `orphanRemoval`** — pasarían por alto la
`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` que `Chat`
tiene sobre sus mensajes. `deleteAll` con las entidades ya cargadas sí
respeta esa cascada, y por eso no hace falta borrar el mensaje por
separado.

## La lección real de este ejercicio

No fue "encontramos un bug porque el código estaba mal escrito" — fue que
una pregunta conceptual bien hecha ("¿esto se parece a blockchain?") llevó
a examinar con más cuidado una interacción entre dos partes del sistema
(el invariante del aggregate + la política de retención) que, por
separado, cada una parecía correcta. Ese tipo de pregunta — "¿qué pasa en
el borde entre estas dos reglas que diseñé por separado?" — es exactamente
el tipo de pensamiento que distingue una revisión de código superficial de
una que encuentra bugs reales antes de que lleguen a producción.

## Autoevaluación

1. Explica, sin usar la palabra "hash", por qué Event Sourcing no ofrece la
   misma garantía de inmutabilidad que blockchain.
2. ¿Por qué el bug descrito aquí solo afectaba a chats `PENDING`, y nunca a
   chats `ACCEPTED`? Repasa `01-ddd/02-el-aggregate-chat.md` si necesitas
   refrescar por qué el aggregate solo rastrea ese booleano.
3. ¿Por qué `chatRepository.deleteAll(stuckPendingChats)` es la elección
   correcta aquí, y no un `@Modifying @Query("DELETE FROM Chat c WHERE ...")`
   como el que sí se usa en `deleteEmptyChatsOlderThan`?
