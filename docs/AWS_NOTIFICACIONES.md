# Notificaciones asíncronas con Amazon SQS, SES y SNS

JavaMail fue eliminado. Los casos de uso siguen dependiendo de `AccountNotificationPort` y `NotificationPort`; el adaptador activo transforma esas invocaciones en eventos JSON versionables y los publica mediante el cliente asíncrono de SQS. Ninguna clase del SDK de AWS entra al dominio o a aplicación.

## Flujo

```text
caso de uso → puertos de notificación → SQS
                                      ↓ polling largo
                         consumidor de eventos
                         ├─ SES → correo
                         └─ SNS → push por endpoint
```

Los eventos actuales son:

| Evento | SES | SNS |
|---|---:|---:|
| `EMAIL_VERIFICATION` | Sí | No |
| `PASSWORD_RESET` | Sí | No |
| `LOST_PET_REPORT_CREATED` | Sí | Sí, si existen dispositivos |

Los eventos de cuenta contienen el token de un solo uso necesario para construir el enlace. La cola debe usar cifrado SSE-KMS, TLS, acceso IAM mínimo, retención corta y una DLQ igualmente cifrada. El consumidor nunca registra el cuerpo del mensaje.

## Recursos AWS

1. Verificar en SES el dominio remitente y configurar DKIM, SPF y DMARC.
2. Solicitar salida del sandbox de SES antes de producción.
3. Crear una cola SQS y otra DLQ en la misma región.
4. Activar SSE-KMS en ambas y una política de redrive, por ejemplo después de cinco recepciones.
5. Configurar long polling en 20 segundos y un visibility timeout mayor que el peor tiempo de SES/SNS; la aplicación usa 60 segundos por defecto.
6. Crear en SNS una Platform Application para FCM o APNs y guardar su ARN.
7. Configurar alarmas de CloudWatch para mensajes visibles, edad máxima y mensajes en DLQ.

Puede utilizarse una cola Standard o FIFO. En FIFO la aplicación asigna el grupo `notifications` y usa el UUID del evento como `messageDeduplicationId`.

## Variables

```text
AWS_NOTIFICATIONS_ENABLED=true
AWS_REGION=us-east-1
AWS_NOTIFICATION_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/notifications
AWS_SES_SENDER_EMAIL=no-reply@dominio.co
AWS_SNS_PLATFORM_APPLICATION_ARN=arn:aws:sns:us-east-1:123456789012:app/GCM/animales-perdidos
FRONTEND_BASE_URL=https://app.dominio.co
AWS_NOTIFICATION_MAX_MESSAGES=10
AWS_NOTIFICATION_WAIT_SECONDS=20
AWS_NOTIFICATION_VISIBILITY_SECONDS=60
AWS_NOTIFICATION_POLL_DELAY_MS=1000
```

Con `AWS_NOTIFICATIONS_ENABLED=false`, el entorno local usa el adaptador de logging y el registro push responde que el canal no está habilitado. No existe fallback SMTP ni dependencia JavaMail.

## IAM mínimo

La identidad de la aplicación necesita:

- `sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:ChangeMessageVisibility` sobre la cola principal;
- `ses:SendEmail` sobre las identidades SES autorizadas;
- `sns:CreatePlatformEndpoint`, `sns:DeleteEndpoint`, `sns:Publish` y las operaciones mínimas necesarias sobre la Platform Application/endpoints;
- permisos KMS de cifrado/descifrado solo para las claves de las colas.

No debe usarse `Resource: *` cuando el servicio permita restringir ARN.

## Dispositivos push

El cliente obtiene el token APNs/FCM y llama:

```text
POST /api/v1/push-subscriptions
DELETE /api/v1/push-subscriptions/{subscriptionId}
```

SNS convierte el token en un endpoint ARN. PostgreSQL almacena únicamente ese ARN, no el token móvil original. La eliminación exige que la suscripción pertenezca al JWT autenticado. Si SNS indica que un endpoint está deshabilitado, se desactiva localmente.

## Reintentos e idempotencia

SQS ofrece entrega al menos una vez. `notification_delivery` registra `(event_id, channel, target)` después de cada entrega: un correo y cada dispositivo se deduplican independientemente. Si un dispositivo falla después de que otro recibió el push, el reintento solo procesa el destino pendiente.

Existe una ventana pequeña inevitable: si SES/SNS entrega y el proceso cae antes de registrar el ledger, SQS podría repetir ese destino. Los mensajes deben ser redactados para tolerar duplicados; los tokens de cuenta siguen siendo de un solo uso. Los fallos persistentes terminan en la DLQ y requieren revisión/replay controlado.
