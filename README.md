# 📄 Contrato Frontend ↔ Backend — Chat App

Este documento define **el contrato exacto** entre el frontend y el backend del sistema de mensajería.

Su objetivo es:
- Evitar ambigüedades
- Permitir desarrollo frontend sin depender del backend
- Servir como base estable para futuras extensiones (WebSockets, notificaciones, etc.)

---

## 🔐 Autenticación

Todos los endpoints requieren autenticación vía JWT.

**Header obligatorio**:
```
Authorization: Bearer <jwt>
```

### Errores estándar
| Código | Significado |
|------|------------|
| 401 | Token inválido o ausente |
| 403 | Token válido pero sin permisos |

---

## 👥 Chats

### Crear chat (usuario no-contacto)

```
POST /api/v1/chats
```

**Request body**
```json
{
  "username": "targetUser"
}
```

**Response — 201 CREATED**
```json
{
  "chatId": 123,
  "status": "PENDING",
  "initiatedBy": 7,
  "participants": [7, 42]
}
```

**Errores posibles**
| Código | Motivo |
|------|-------|
| 404 | Usuario no existe |
| 409 | Chat ya existe |

---

### Listar mis chats

```
GET /api/v1/chats
```

**Query params opcionales**
- `status=PENDING|ACTIVE|BLOCKED`

**Response — 200 OK**
```json
[
  {
    "chatId": 123,
    "status": "PENDING",
    "lastMessageAt": "2026-01-30T18:22:00Z",
    "unreadCount": 1
  }
]
```

---

### Aceptar chat

```
POST /api/v1/chats/{chatId}/accept
```

**Response — 200 OK**
```json
{
  "chatId": 123,
  "status": "ACTIVE"
}
```

**Errores**
| Código | Motivo |
|------|-------|
| 403 | Usuario no es receptor |
| 409 | Chat no está en estado PENDING |

---

### Bloquear chat

```
POST /api/v1/chats/{chatId}/block
```

**Response — 200 OK** (sin body)

---

## 💬 Mensajes

### Enviar mensaje

```
POST /api/v1/messages
```

**Request body**
```json
{
  "chatId": 123,
  "encryptedContentByUser": {
    "7": "cipher_for_7",
    "42": "cipher_for_42"
  }
}
```

**Response — 201 CREATED**
```json
{
  "messageId": 555,
  "chatId": 123,
  "senderId": 7,
  "encryptedContent": "cipher_for_7",
  "read": false,
  "sentAt": "2026-01-30T18:23:11Z"
}
```

**Reglas de envío**
- Chat `BLOCKED`: nadie puede enviar
- Chat `PENDING`:
  - solo el iniciador puede enviar
  - **solo un mensaje permitido** hasta aceptación
- Chat `ACTIVE`: libre

**Errores**
| Código | Motivo |
|------|-------|
| 403 | Envío no permitido por estado |
| 404 | Chat no existe |

---

### Obtener mensajes de un chat

```
GET /api/v1/messages/chat/{chatId}
```

**Response — 200 OK**
```json
[
  {
    "messageId": 555,
    "chatId": 123,
    "senderId": 7,
    "encryptedContent": "cipher_for_7",
    "read": false,
    "sentAt": "2026-01-30T18:23:11Z"
  }
]
```

---

## 🔄 Estados del chat

```
PENDING → ACTIVE → BLOCKED
```

### Reglas
- `PENDING`
  - Solo el iniciador puede escribir
  - Máximo 1 mensaje
- `ACTIVE`
  - Ambos usuarios pueden escribir
- `BLOCKED`
  - Nadie puede escribir

---

## 🚫 Responsabilidades del Frontend

El frontend **NO debe**:
- Asumir que un chat existe
- Reintentar mensajes sin confirmación
- Ignorar estados del chat

El frontend **DEBE**:
- Respetar códigos HTTP
- Confiar únicamente en respuestas del backend
- Refrescar estado tras errores

---

## 🔮 Extensión futura: WebSockets (no implementado)

Los WebSockets reutilizarán los mismos DTOs definidos aquí.

Ejemplo:
```
/topic/chats/{chatId}
```

Payload = `MessageResponse`

No se romperá este contrato.

---

📌 Este documento es la fuente de verdad del sistema.

