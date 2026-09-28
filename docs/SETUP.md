# Setup Guide

This guide walks through running the Roommate Management System locally in VS Code:
MySQL → Spring Boot backend → Angular frontend.

> **A note on this build**: the code in this repository was written in a sandboxed
> environment with no access to Maven Central or the npm registry, so `mvn clean install`
> and `npm install` could not be executed here to verify a clean build. Every file was
> written and manually cross-checked for consistency (entity fields vs. DTOs vs.
> repository methods vs. schema columns, Angular imports vs. exports, etc.), but you
> should run the builds yourself in VS Code — where normal internet access is available —
> as the first step, and report back any compiler error so it can be fixed quickly.

## 1. Prerequisites

Install the following and make sure each is on your `PATH`:

| Tool | Version | Check with |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | 18+ (LTS) | `node -version` |
| npm | 9+ | `npm -version` |
| Angular CLI | 17.x | `ng version` (install with `npm install -g @angular/cli@17`) |
| MySQL Server | 8.0+ | `mysql --version` |
| MySQL Workbench | latest | — |

Recommended VS Code extensions: **Extension Pack for Java**, **Spring Boot Extension Pack**,
**Angular Language Service**, **MySQL** (by Weijan Chen) or use MySQL Workbench directly.

## 2. Clone / open the project

Open the `roommate-management/` folder in VS Code. It contains:

```
roommate-management/
├── backend/     Spring Boot API (Java 17, Maven)
├── frontend/    Angular 17 SPA
├── database/    schema.sql + seed-data.sql
├── docs/        API.md, DATABASE.md, SETUP.md (this file)
└── README.md
```

## 3. Set up MySQL

1. Start your local MySQL server.
2. Open MySQL Workbench (or the `mysql` CLI) and connect as `root` (or any admin user).
3. Run the schema script, then the seed script, in order:

   ```sql
   SOURCE /absolute/path/to/roommate-management/database/schema.sql;
   SOURCE /absolute/path/to/roommate-management/database/seed-data.sql;
   ```

   Or from a terminal:

   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed-data.sql
   ```

   `schema.sql` creates the `roommate_management` database and all 25 tables with keys,
   constraints and indexes. `seed-data.sql` inserts a demo room with 4 members and sample
   data across every module (see `docs/DATABASE.md` for the full table list and the
   default login credentials at the bottom of `seed-data.sql`).

4. (Optional) Create a dedicated MySQL user instead of using `root`:

   ```sql
   CREATE USER 'roommate_app'@'localhost' IDENTIFIED BY 'a-strong-password';
   GRANT ALL PRIVILEGES ON roommate_management.* TO 'roommate_app'@'localhost';
   FLUSH PRIVILEGES;
   ```

## 4. Run the backend (Spring Boot)

The backend reads its MySQL credentials and JWT secret from environment variables, all
with sensible local defaults baked into `backend/src/main/resources/application.yml`:

| Variable | Default | Purpose |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | (dev key baked in) | HMAC signing key for JWTs — **override in any real deployment** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d) | Reserved for future refresh-token support |
| `FILE_STORAGE_DIR` | `uploads` | Where uploaded photos/receipts/ID proofs are written (relative to the backend's working directory) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origins allowed to call the API |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | empty | Only needed if you wire up real forgot-password emails; the API works without them (see note in API.md) |

If your MySQL username/password differ from `root`/`root`, set them before starting the app:

```bash
cd backend

# macOS / Linux
export DB_USERNAME=root
export DB_PASSWORD=your_password

# Windows (PowerShell)
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"

mvn clean install
mvn spring-boot:run
```

Or simply run/debug `RoommateManagementApplication.java` from VS Code once the environment
variables are set in your launch configuration.

The API starts on **http://localhost:8080**. Swagger UI is available at
`http://localhost:8080/swagger-ui.html` once it's up — useful for exploring/testing every
endpoint without the frontend.

### Verifying the backend started correctly

- Console should show `Started RoommateManagementApplication` with no stack trace.
- `GET http://localhost:8080/swagger-ui.html` should load.
- `POST http://localhost:8080/api/auth/login` with `{"email":"admin@roommates.dev","password":"password123"}`
  should return a JWT (see `docs/API.md` and the credentials table in `README.md`).

## 5. Run the frontend (Angular)

In a separate terminal:

```bash
cd frontend
npm install
ng serve
```

This starts the dev server on **http://localhost:4200** and proxies API calls straight to
`http://localhost:8080/api` (configured in `src/environments/environment.ts`). Open
`http://localhost:4200` in your browser — you should land on the login page.

If you change the backend port or host, update `frontend/src/environments/environment.ts`
(`apiUrl` and `fileBaseUrl`) and `frontend/src/environments/environment.prod.ts` accordingly.

## 6. Log in

Use any of the seeded accounts (all share the password `password123`):

| Email | Role |
|---|---|
| `admin@roommates.dev` | ADMIN |
| `rahul@roommates.dev` | MEMBER |
| `akash@roommates.dev` | MEMBER |
| `priya@roommates.dev` | MODERATOR (granted `MANAGE_TASK`, `MANAGE_GROCERY`) |

Or register a brand-new account from the UI — you'll be prompted to create your own room
and become its admin.

## 7. File uploads

Uploaded files (profile photos, ID proofs, receipts, bill photos, issue photos, inventory
photos) are written under `backend/uploads/` (already scaffolded with one subfolder per
category) and served back at `http://localhost:8080/uploads/...` for everything except ID
proofs, which are only ever served through the authenticated
`GET /api/members/{id}/id-proof` endpoint (never as a static file) so they stay private to
the member and the room admin.

## 8. Running tests

```bash
cd backend
mvn test
```

Backend tests run against an in-memory H2 database (`src/test/resources/application-test.yml`)
so they don't touch your MySQL data.

```bash
cd frontend
ng test
```

## 9. Troubleshooting

**`Communications link failure` / backend won't connect to MySQL**
MySQL isn't running, or the credentials/port don't match. Confirm `mysql -u root -p` works
from the same machine, and that `DB_USERNAME`/`DB_PASSWORD` match.

**`Unknown database 'roommate_management'`**
Run `database/schema.sql` first — it creates the database with `CREATE DATABASE IF NOT EXISTS`.

**Backend starts but every API call returns 401**
Make sure the Angular app is sending the `Authorization: Bearer <token>` header — this is
handled automatically by `core/interceptors/auth.interceptor.ts` once you're logged in. If
you're testing with curl/Postman, copy the `token` from the login response.

**403 Forbidden on a request that should be allowed**
Roles and permissions are enforced server-side (see `RoomAccessService`), independent of
what the UI shows. A MEMBER without the relevant permission (e.g. `MANAGE_EXPENSE`) will be
rejected even if they craft the request by hand — this is intentional. Have the room admin
grant the permission from **Members → Permissions**.

**CORS errors in the browser console**
Confirm the frontend origin matches `CORS_ALLOWED_ORIGINS` (default `http://localhost:4200`).
If you serve the Angular app from a different port, update the backend env var.

**File upload fails with 400**
Check the file type/size against the limits in `application.yml`
(`app.file-storage.allowed-image-types`, `allowed-doc-types`, 10MB per file).

**Maven/npm dependency resolution errors**
Make sure your machine has normal internet access to Maven Central and the npm registry
(this project was authored in a network-restricted sandbox — see the note at the top of
this file). Corporate proxies/VPNs sometimes need `~/.m2/settings.xml` / npm proxy config.
