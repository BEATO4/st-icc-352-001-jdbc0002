# EventPass - Gestion de Eventos Academicos

Aplicacion web full-stack para gestionar eventos, inscripciones y control de asistencia por QR.

Stack principal:
- Backend: Java 21, Javalin 7, Hibernate ORM
- DB: H2 (file-based)
- Frontend: HTML/CSS/JS vanilla
- Build: Gradle

---

## Caracteristicas

- Registro e inicio de sesion con roles:
  - `PARTICIPANT`
  - `ORGANIZER`
  - `ADMIN`
- CRUD de eventos para organizadores/admin.
- Publicar, pausar y cancelar eventos.
- Inscripcion/cancelacion de participantes.
- Generacion de token y QR por inscripcion.
- Escaneo de QR por camara o ingreso manual.
- Registro de asistencia (idempotente: escaneo repetido no duplica asistencia).
- Panel de estadisticas por evento.
- Panel administrativo de usuarios y eventos.

---

## Estructura del proyecto

```text
src/main/java/edu/pucmm/icc352
  Main.java
  config/
  controllers/
  models/
  repositories/
  services/
  utils/

src/main/resources
  hibernate.cfg.xml
  public/
    *.html
    css/main.css
    js/api.js
```

---

## Requisitos

- Java 21+
- Gradle Wrapper (ya incluido)

En Windows usa `gradlew.bat`; en Linux/macOS usa `./gradlew`.

---

## Ejecucion local

### 1) Compilar y correr

```powershell
cd C:\Users\rafav\IdeaProjects\parcial21
.\gradlew.bat run
```

La app inicia en:
- `http://localhost:8080`

### 2) Ejecutar pruebas

```powershell
cd C:\Users\rafav\IdeaProjects\parcial21
.\gradlew.bat test
```

### 3) Build de JAR

```powershell
cd C:\Users\rafav\IdeaProjects\parcial21
.\gradlew.bat build
```

---

## Base de datos

Se usa H2 en archivo, configurada en `src/main/resources/hibernate.cfg.xml`.

- URL: `jdbc:h2:file:./data/eventpass`
- Modo schema: `hibernate.hbm2ddl.auto=update`
- La data persiste entre reinicios en la carpeta `data/` del proyecto.

---

## Usuario admin por defecto (seed)

Al iniciar la app, `SeedData` crea un admin si no existe:

- Email: `admin@eventos.com`
- Password: `Admin1234!`

Archivo relevante: `src/main/java/edu/pucmm/icc352/utils/SeedData.java`.

---

## Flujo QR (como funciona)

1. Un participante se inscribe en un evento.
2. Se crea `Registration` con `qrToken` unico (`eventId:userId:uuid`).
3. El participante ve su QR en `my-registrations.html`.
4. Organizador/admin escanea en `scan.html` (camara o ingreso manual).
5. Frontend llama `POST /api/registrations/scan` con `{ "qrToken": "..." }`.
6. Backend valida token y guarda `Attendance`.

Rutas clave:
- `GET /api/registrations/qr/{token}/image`
- `POST /api/registrations/scan`

---

## Fecha de inicio y fin de eventos

Los eventos manejan:
- `dateTime` (inicio)
- `endDateTime` (termino)

Reglas actuales:
- Inicio debe ser futuro al crear evento.
- Fin debe ser posterior al inicio.
- Si no se envia `endDateTime`, backend usa inicio + 2 horas.

Archivos relevantes:
- `src/main/java/edu/pucmm/icc352/models/Event.java`
- `src/main/java/edu/pucmm/icc352/services/EventService.java`
- `src/main/java/edu/pucmm/icc352/controllers/EventController.java`
- `src/main/resources/public/my-events.html`

---

## API resumida

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Events
- `GET /api/events` (publicados)
- `GET /api/events/mine` (organizador autenticado)
- `GET /api/events/{id}`
- `POST /api/events` (organizador/admin)
- `PUT /api/events/{id}` (organizador/admin)
- `PATCH /api/events/{id}/publish` (organizador/admin)
- `PATCH /api/events/{id}/cancel` (organizador/admin)

### Registrations / Attendance
- `GET /api/registrations/my`
- `GET /api/registrations/event/{eventId}` (organizador/admin)
- `POST /api/registrations/{eventId}`
- `DELETE /api/registrations/{eventId}`
- `GET /api/registrations/qr/{token}/image`
- `POST /api/registrations/scan` (organizador/admin)

### Stats
- `GET /api/stats/{eventId}` (organizador/admin)

### Admin
- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/block`
- `PATCH /api/admin/users/{id}/role`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/events`
- `DELETE /api/admin/events/{id}`

---

## Frontend principal

- `index.html`: portada
- `login.html`: login/registro
- `dashboard.html`: resumen
- `events.html`: listado publico de eventos
- `my-events.html`: gestion de eventos del organizador
- `my-registrations.html`: QR del participante
- `scan.html`: escaneo QR
- `admin.html`: gestion administrativa

Todos en: `src/main/resources/public/`

---

## Manejo de errores HTTP

Configurado en `Main.java`:
- `IllegalArgumentException` -> `400`
- `IllegalStateException` -> `409`
- `SecurityException` -> `403`
- `Exception` -> `500`

---

## Creadores

- Jostin Beato
- Rafael Ramirez

---

## Solucion de problemas

- Error de stats en cero:
  - Verifica que la respuesta de `GET /api/stats/{eventId}` incluya `totalRegistrations`, `totalAttendances`, `attendanceRate`, `absentees`.
- Error al escanear QR con mensaje `For input string: "scan"`:
  - Ocurre por colision de rutas (`/scan` vs `/{eventId}`) si el orden de rutas no es correcto.
- No se marca asistencia en lista de inscritos:
  - Verifica que `Registration` serialice `attended/scannedAt` y que exista fila en `attendance`.

---

## Licencia

No definida aun en este repositorio.

