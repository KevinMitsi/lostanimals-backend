# Contrato WebSocket de mensajería

## Cambio incompatible para front-end

El envío por `POST /api/v1/conversations/{conversationId}/messages` fue retirado. Desde esta versión los mensajes se envían y reciben por WebSocket. La consulta histórica por HTTP se conserva para carga inicial y recuperación después de una reconexión:

```text
GET /api/v1/conversations/{conversationId}/messages?after={cursor}&limit=50
```

No cambió la forma de los DTO existentes `SendMessageRequest` y `MessageResponse`; cambió su transporte.

## Conexión y autenticación

```text
wss://{api-host}/ws/conversations/{conversationId}?access_token={jwt}
```

- `{conversationId}` debe ser un UUID de una conversación en la que participe el usuario.
- El JWT es el mismo access token Bearer de la API REST.
- En clientes capaces de configurar encabezados puede usarse `Authorization: Bearer {jwt}` y omitirse el query parameter.
- El navegador no permite configurar `Authorization` mediante la API nativa `WebSocket`; por eso se admite `access_token` solamente bajo `/ws/conversations/**`.
- Producción debe usar siempre `wss://`. El proxy y la observabilidad deben ocultar el parámetro `access_token` de sus logs.
- El servidor rechaza un encabezado `Origin` que no esté incluido en `CORS_ALLOWED_ORIGINS`.

La conexión se cierra si el token no es válido, el usuario no pertenece a la conversación, el UUID no es válido o el origen no está permitido.

## Enviar

Cada frame de texto contiene el mismo JSON que antes recibía el endpoint HTTP:

```json
{
  "content": "La vi cerca del parque a las 16:30"
}
```

`content` es obligatorio, no puede estar en blanco, admite hasta 2000 caracteres y conserva la validación contra sintaxis SQL sospechosa. El servidor limita el frame completo a 4096 caracteres.

## Recibir

Todos los sockets autorizados conectados a esa conversación, incluido el emisor, reciben el mensaje persistido:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "senderId": "7845ee7e-f143-4e72-9873-dab8dfab86c1",
  "content": "La vi cerca del parque a las 16:30",
  "createdAt": "2026-08-25T21:30:00Z"
}
```

El front-end debe considerar este frame como confirmación de envío. No debe insertar además una copia optimista sin reconciliarla por `id`, pues el emisor también recibe la difusión.

Si un frame no puede procesarse, el socket permanece abierto y el emisor recibe:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "must not be blank"
}
```

Códigos posibles: `INVALID_PAYLOAD`, `PAYLOAD_TOO_LARGE`, `VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`, `FORBIDDEN`, `NOT_FOUND` e `INTERNAL_ERROR`.

## Reconexión recomendada

1. Cargar el historial HTTP y guardar `nextAfter`.
2. Abrir el WebSocket.
3. Al reconectar, consultar de nuevo el endpoint HTTP con el último cursor antes de continuar procesando frames en vivo.
4. Desduplicar por `MessageResponse.id` para cubrir la pequeña ventana entre la recuperación HTTP y la reapertura del socket.

El historial en PostgreSQL es la fuente de verdad. La difusión WebSocket actual es local a una instancia de la aplicación; si se despliegan varias réplicas se debe conectar el puerto `MessageEventPublisher` a un broker compartido (por ejemplo Redis o RabbitMQ) sin modificar el caso de uso.

## Ejemplo mínimo en navegador

```javascript
const url = new URL(`${API_WS_URL}/ws/conversations/${conversationId}`);
url.searchParams.set("access_token", accessToken);

const socket = new WebSocket(url);
socket.onmessage = ({ data }) => {
  const frame = JSON.parse(data);
  if (frame.code) {
    showMessageError(frame);
    return;
  }
  upsertMessage(frame);
};

socket.send(JSON.stringify({ content: "Hola" }));
```
