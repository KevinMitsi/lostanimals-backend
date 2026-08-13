# Protección con Cloudflare

La integración tiene dos capas distintas. Turnstile se valida desde la aplicación para reducir registros y accesos automatizados. La mitigación DDoS, WAF y rate limiting se ejecuta antes de que una solicitud llegue al backend y debe configurarse en Cloudflare.

## 1. Turnstile para registro y login

1. Crear un widget Turnstile en Cloudflare y restringirlo a los hostnames de cada ambiente.
2. En el frontend, renderizar el widget con `action: "register"` para registro y `action: "login"` para login.
3. Enviar el token generado en `turnstileToken`. Es de un solo uso y expira aproximadamente en cinco minutos.
4. Configurar el backend:

```text
CLOUDFLARE_TURNSTILE_ENABLED=true
CLOUDFLARE_TURNSTILE_SECRET=<secret del widget; nunca enviarlo al navegador>
CLOUDFLARE_EXPECTED_HOSTNAME=app.example.com
```

El adaptador llama de forma no bloqueante a `POST https://challenges.cloudflare.com/turnstile/v0/siteverify` y comprueba `success`, `action` y `hostname`. Si Cloudflare no responde, el acceso falla de forma cerrada.

Para desarrollo local Turnstile está deshabilitado. Puede usarse el sitekey y secret de prueba oficiales si se desea probar el flujo completo.

## 2. DDoS, WAF y protección del origen

Estas medidas se aplican en el panel o mediante infraestructura como código:

1. Mantener el registro DNS de la aplicación como **Proxied** (nube naranja).
2. Activar los managed rulesets de DDoS y WAF.
3. Añadir rate limiting estricto para:
   - `POST /api/v1/auth/login`;
   - `POST /api/v1/auth/register`;
   - endpoints de publicación y subida de imágenes.
4. Bloquear el acceso público directo al origen. Aceptar únicamente rangos IP de Cloudflare o, preferiblemente, usar Cloudflare Tunnel.
5. Habilitar Full (strict) TLS y Authenticated Origin Pulls con certificado propio cuando la plataforma de alojamiento lo permita.
6. No publicar la IP del origen en registros DNS-only, históricos, repositorios ni mensajes de error.

Solo después de bloquear el acceso directo al origen debe habilitarse:

```text
CLOUDFLARE_TRUST_CONNECTING_IP=true
```

Esto permite usar `CF-Connecting-IP` como IP real del visitante para Turnstile. Si el origen continúa accesible públicamente, ese encabezado puede ser falsificado y debe permanecer en `false`.

## 3. Secretos y rotación

- Guardar `JWT_SECRET`, `CLOUDFLARE_TURNSTILE_SECRET`, credenciales AWS y contraseña de BD en el gestor de secretos del proveedor de despliegue.
- Usar secretos diferentes en desarrollo, pruebas y producción.
- `JWT_SECRET` debe tener al menos 32 bytes aleatorios. Rotarlo invalida los JWT existentes con el diseño simétrico actual.
- Nunca incluir secretos en el frontend, logs, imágenes de contenedor o repositorio.
