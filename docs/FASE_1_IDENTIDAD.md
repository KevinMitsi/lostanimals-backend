# Fase 1 — Identidad segura

## Alcance completado

- El registro crea una cuenta no verificada y un token de verificación con vigencia configurable.
- El login exige contraseña correcta y correo verificado.
- El login devuelve un JWT de corta duración y un refresh token opaco.
- Cada refresh rota de forma atómica: el token anterior queda revocado y solo se almacena SHA-256 del nuevo.
- Logout revoca la sesión refresh indicada.
- La recuperación de contraseña no revela si el correo existe, usa token de un solo uso y revoca todas las sesiones del usuario al cambiar la clave.
- Reenvío de verificación y recuperación están protegidos por acciones Turnstile independientes.
- Los fallos del transporte de correo no alteran la respuesta pública ni permiten enumerar cuentas. El token queda disponible para reintento mediante un futuro mecanismo outbox.

## Endpoints públicos

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Crea la cuenta y solicita verificación |
| `POST` | `/api/v1/auth/verify-email` | Consume el token y verifica el correo |
| `POST` | `/api/v1/auth/resend-verification` | Genera un nuevo token; siempre responde `202` |
| `POST` | `/api/v1/auth/login` | Emite JWT y refresh token |
| `POST` | `/api/v1/auth/refresh` | Rota el refresh token y emite otro par |
| `POST` | `/api/v1/auth/logout` | Revoca el refresh token |
| `POST` | `/api/v1/auth/forgot-password` | Solicita recuperación; siempre responde `202` |
| `POST` | `/api/v1/auth/reset-password` | Cambia la clave y revoca todas las sesiones |

Todos los requests tienen Jakarta Validation y los contratos están publicados en OpenAPI.

## Tokens y almacenamiento

- Los valores de verificación, recuperación y refresh tienen 256 bits generados con `SecureRandom`.
- La base de datos almacena únicamente SHA-256 hexadecimal, nunca el token utilizable.
- Verificación y recuperación son de un solo uso mediante un `UPDATE ... RETURNING` condicionado por expiración y consumo.
- La rotación refresh usa un único CTE PostgreSQL para revocar e insertar, cerrando carreras concurrentes.
- TTL predeterminados: JWT 1 hora, refresh 30 días, verificación 24 horas, recuperación 30 minutos.

## Configuración

```text
JWT_TTL=PT1H
REFRESH_TOKEN_TTL=P30D
EMAIL_VERIFICATION_TTL=PT24H
PASSWORD_RESET_TTL=PT30M
FRONTEND_BASE_URL=https://app.example.com
```

Turnstile debe emitir las acciones `resend-verification` y `password-recovery`, además de `register` y `login`.

## Estrategia de pruebas

Los servicios con dependencias inyectadas usan JUnit 5 y Mockito. Los puertos son `@Mock`; `ArgumentCaptor` verifica los modelos enviados a persistencia y notificaciones. Se usan spies solo si una prueba necesita conservar comportamiento real parcial; actualmente no existe ese caso en los servicios de la fase.
