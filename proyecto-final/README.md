# Proyecto Final - Encuestas (REST + gRPC)

Sistema de gestion de encuestas con:
- **Backend Java** (Javalin) con API REST y servidor gRPC.
- **Base de datos MongoDB**.
- **Cliente JavaFX REST**.
- **Cliente JavaFX gRPC**.

Todo el codigo de la aplicacion esta en `proyecto-final/`.

## Estructura del proyecto

```text
proyecto-final/
├─ src/main/java/.../backend      # Backend REST + gRPC
├─ src/main/proto                 # Contrato proto del servidor
├─ db/                            # Acceso a datos (MongoDB)
├─ cliente-rest/                  # Cliente JavaFX para REST
├─ cliente-grpc/                  # Cliente JavaFX para gRPC
├─ docker-compose.yml             # Mongo + app + Caddy
└─ Caddyfile                      # Proxy HTTPS para REST
```

## Requisitos

- **Java 21** (recomendado para JavaFX y build actual).
- **Gradle Wrapper** (ya incluido como `gradlew` y `gradlew.bat`).
- **MongoDB 6+** si corres local sin Docker.
- **Docker + Docker Compose** (opcional, para levantar entorno contenedorizado).

## Variables de entorno

El backend soporta estas variables:

- `MONGODB_URI` (default: `mongodb://localhost:27017`)
- `MONGODB_DB` (default: `survey_app`)
- `JWT_SECRET` (default interno en codigo; recomendado definirlo en produccion)

## Ejecutar en local (desarrollo)

Abre terminal en:

```bash
cd proyecto-final
```

### 1) Levantar MongoDB local

Ejemplo si tienes Docker:

```bash
docker run -d --name mongo-survey -p 27017:27017 mongo:6.0
```

Si ya tienes Mongo instalado local, solo asegurate de que este corriendo en `localhost:27017`.

### 2) Levantar backend (REST + gRPC)

Desde `proyecto-final`:

```bash
./gradlew run
```

En Windows PowerShell:

```powershell
.\gradlew.bat run
```

Puertos del backend:
- REST: `8080`
- gRPC: `9090`
- Route overview: `http://localhost:8080/api/routes`

### 3) Ejecutar cliente REST (JavaFX)

En otra terminal, desde `proyecto-final`:

```bash
./gradlew :cliente-rest:run
```

Windows PowerShell:

```powershell
.\gradlew.bat :cliente-rest:run
```

Al abrir la app:
- Host local: `localhost`
- Puerto local: `8080`

### 4) Ejecutar cliente gRPC (JavaFX)

En otra terminal, desde `proyecto-final`:

```bash
./gradlew :cliente-grpc:run
```

Windows PowerShell:

```powershell
.\gradlew.bat :cliente-grpc:run
```

Al abrir la app:
- Host local: `localhost`
- Puerto local: `9090`

> Nota: ambos clientes traen valores por defecto orientados a despliegue remoto (`beat04.me`), por lo que para pruebas locales debes cambiarlos manualmente.

## Ejecutar con Docker Compose

Desde `proyecto-final`:

```bash
docker compose up --build
```

Servicios definidos:
- `mongo`: base de datos.
- `app`: aplicacion Java (con `MONGODB_URI=mongodb://mongo:27017`).
- `proxy` (Caddy): expone REST por `80/443` y hace reverse proxy a `app:8080`.

Puertos expuestos:
- REST via proxy: `http://localhost` y `https://localhost` (si Caddy configura TLS localmente).
- gRPC: `9090` (mapeado por el servicio `app`).

Para bajar servicios:

```bash
docker compose down
```

## API REST principal

Rutas disponibles en backend:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/validate`
- `GET /api/auth/me` (protegida por JWT)
- `GET /api/surveys/location` (JWT)
- `GET /api/surveys/user/{userId}` (JWT)
- `POST /api/surveys` (JWT)
- `GET /api/surveys` (JWT)
- `GET /api/surveys/{id}` (JWT)
- `PUT /api/surveys/{id}` (JWT)
- `DELETE /api/surveys/{id}` (JWT)

## Servicio gRPC

Definido en `src/main/proto/encuesta.proto`:
- `ListarFormularios`
- `CrearFormulario`
- `ListarTodos`
- `ActualizarFormulario`
- `EliminarFormulario`

