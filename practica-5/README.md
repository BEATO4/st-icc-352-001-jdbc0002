# Practica 5 - AJAX + WebSocket

Base: blog de la practica anterior (Javalin + JPA + Thymeleaf).

## Funcionalidades implementadas

- Paginacion de articulos con `Fetch API` sin recargar la pagina completa.
- Chat para visitantes/autenticados en:
  - Pagina principal.
  - Vista de detalle de articulo.
- Panel de administracion de chats para `admin` y `autor`:
  - Ver conversaciones activas/recientes.
  - Responder varias conversaciones en paralelo.
  - Cerrar conversaciones.
- Persistencia de conversaciones y mensajes en BD.
- Ejecucion con Docker y Docker Compose.

## Ejecutar local (sin Docker)

```powershell
cd practica-5
.\gradlew.bat run
```

Abrir: `http://localhost:7000`

Usuarios iniciales (si la BD esta vacia):

- `admin / admin`
- `autor / 1234`

## Ejecutar con Docker Compose

```powershell
cd practica-5
docker compose up --build
```

Abrir: `http://localhost:7000`

## Endpoints agregados

- `GET /api/articulos?pagina=N`
- `POST /api/chat/sesiones`
- `GET /api/chat/sesiones/{id}/mensajes?token=...`
- `GET /api/admin/chats/sesiones`
- `GET /api/admin/chats/sesiones/{id}/mensajes`
- `POST /api/admin/chats/sesiones/{id}/cerrar`
- `WS /ws/chat`

