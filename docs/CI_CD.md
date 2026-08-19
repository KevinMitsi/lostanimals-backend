# CI/CD del backend

El workflow `.github/workflows/backend-cd.yml` se ejecuta al enviar cambios a `master` o manualmente.

## Flujo

1. Ejecuta los tests con Java 21 y Gradle. Un fallo detiene el pipeline.
2. Asume un rol de AWS mediante GitHub OIDC, sin access keys permanentes.
3. Construye la imagen y publica dos tags en ECR: el SHA inmutable del commit y `latest`.
4. Comprueba que la instancia EC2 esté `Online` en Systems Manager.
5. En `/opt/animales-perdidos`, descarga la imagen antes de detener el servicio `backend`.
6. Recrea únicamente ese servicio y espera hasta 60 segundos por `/actuator/health`.
7. Si el health check local falla, vuelve a etiquetar la imagen anterior y recrea el contenedor.
8. Comprueba finalmente `https://api.animales-perdidos.com/actuator/health` a través de Cloudflare.

## Variables del environment `production` en GitHub

Crear el environment en **Settings > Environments > production** y agregar:

| Variable | Ejemplo                                               |
|---|-------------------------------------------------------|
| `AWS_REGION` | `us-east-2`                                           |
| `AWS_ROLE_ARN` | `arn:aws:iam::1228025:role/lostanimals-github-deploy` |
| `ECR_REPOSITORY` | `animales-backend`                                    |
| `EC2_INSTANCE_ID` | `i-012345789adef0`                                    |
| `DEPLOY_PATH` | `/path/path/animales-perdidos`                        |
| `COMPOSE_SERVICE` | `backend-animals`                                     |
| `HEALTHCHECK_URL` | `https://api.animales-perdidos.com/actuator/health`   |

Opcionalmente, proteger el environment con aprobación manual. No se necesitan secretos AWS en GitHub.

## Proveedor OIDC y rol de GitHub

En IAM debe existir el proveedor:

- URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

La trust policy del rol debe limitarse a este repositorio, la rama `master` y el environment `production`. Para repositorios sin subject claims inmutables habilitados, el `sub` esperado es:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::512624878025:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:KevinMitsi/lostanimals-backend:environment:production"
        }
      }
    }
  ]
}
```

Verificar el subject real si el repositorio activó los subject claims inmutables introducidos por GitHub en 2026.

## Permisos mínimos del rol de GitHub

Reemplazar el ID de instancia y el nombre del repositorio ECR cuando corresponda:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuthorization",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "PushBackendImage",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:us-east-1:512624878025:repository/animales-perdidos-backend"
    },
    {
      "Sid": "InspectManagedInstance",
      "Effect": "Allow",
      "Action": "ssm:DescribeInstanceInformation",
      "Resource": "*"
    },
    {
      "Sid": "RunDeployment",
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:us-east-1:512624878025:instance/i-REEMPLAZAR",
        "arn:aws:ssm:us-east-1::document/AWS-RunShellScript"
      ]
    },
    {
      "Sid": "ReadDeploymentResult",
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }
  ]
}
```

## Requisitos de la EC2

- SSM Agent activo y la instancia visible como `Online`.
- Rol de instancia con `AmazonSSMManagedInstanceCore` y permisos de lectura sobre ECR.
- AWS CLI, Docker, Docker Compose y `curl` instalados.
- `/opt/animales-perdidos/compose.yml` existente.
- El servicio de Compose debe llamarse `backend` o coincidir con `COMPOSE_SERVICE`.
- La imagen del servicio debe usar el tag `latest` publicado por el workflow.
- Las variables y secretos de ejecución deben permanecer en `/opt/animales-perdidos/.env`, nunca en GitHub Actions.

Ejemplo del fragmento requerido:

```yaml
services:
  backend:
    image: 512624878025.dkr.ecr.us-east-1.amazonaws.com/animales-perdidos-backend:latest
    env_file:
      - .env
    restart: unless-stopped
    ports:
      - "8080:8080"
```

El rol de la EC2 necesita como mínimo `ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer` y `ecr:BatchCheckLayerAvailability` sobre el repositorio correspondiente.
