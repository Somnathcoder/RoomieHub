# Roommate Management System

A full-stack web application for roommates sharing a flat/room to manage expenses, bills,
chores, cleaning rotations, announcements, messaging, polls, inventory, maintenance issues
and more — with one **Room Admin** per room and no landlord/owner concept anywhere in the
system.

> **Build status note**: this project was authored in a sandboxed environment with no
> access to Maven Central or the npm registry, so the backend and frontend builds could not
> be executed here to confirm they compile cleanly. Every file was hand-written and
> manually cross-checked (entity fields vs. DTOs vs. repositories vs. `schema.sql`; Angular
> component imports vs. actual exports; service method signatures vs. call sites), but you
> should run `mvn clean install` / `npm install` yourself as the first step — see
> `docs/SETUP.md` — since that's the one thing that couldn't be verified end-to-end here.

## Features

- **Authentication** — JWT-based login/register, BCrypt password hashing, forgot/reset
  password, route guards and an HTTP interceptor that attaches the token and handles
  session expiry automatically.
- **Room management** — create a room and become its admin; every other module is scoped
  to a room, enforced server-side (a member of Room A can never read or write Room B's data).
- **Member management** — add members by email (existing account or auto-created with a
  temporary password), profile photos, ID proof uploads (private — never publicly
  browsable, viewable only by the member themselves and the admin), activate/deactivate.
- **Roles & permissions** — ADMIN (full control), MEMBER (their own stuff), MODERATOR
  (an admin-configurable subset of permissions such as `MANAGE_EXPENSE`, `MANAGE_TASK`,
  `MANAGE_CLEANING`, etc.) — all enforced in the service layer, not just hidden in the UI.
- **Expenses** — equal or custom split (validated to sum exactly to the total), receipt
  photos, a PENDING → APPROVED/REJECTED approval workflow with rejection reasons.
- **Settlements** — automatic "who owes whom" generation from approved expenses, mark-as-
  paid, admin verification, running balances and a netted-out summary per pair.
- **Recurring expenses** — daily/weekly/monthly/yearly templates that auto-generate real,
  approved expenses (with splits and settlements) on schedule.
- **Grocery / shopping list** — track what's needed, mark purchased, convert straight into
  a split expense.
- **Bills** — electricity/WiFi/gas/water/maintenance/other, partial payments,
  automatic overdue detection.
- **Tasks & cleaning** — general chores plus a dedicated cleaning-rotation engine that
  round-robins assignments across members on a configurable interval, auto-extends itself
  so it never runs dry, and sends day-before/on-day/overdue reminders.
- **Announcements & messaging** — room-wide notices plus public room chat and private 1:1
  messaging.
- **Notifications** — a single in-app feed fed by every other module (expense
  submitted/approved, settlement requested, bill overdue, task assigned, cleaning
  reminders, and more).
- **Polls** — one vote per member, live results, admin can close early.
- **Inventory** — shared belongings with condition tracking (working/damaged/lost/disposed).
- **Issues / maintenance** — report problems with a photo and priority, track to resolution.
- **Calendar-adjacent data** — bill due dates, cleaning schedule and tasks all carry dates
  the dashboard surfaces as "upcoming"; combine with the activity log for a full timeline.
- **Dashboards** — distinct admin and member views: stats, upcoming items, recent activity.
- **Analytics & reports** — expense trends by month/category/member, CSV and PDF export for
  four report types.
- **Activity log** — an append-only audit trail of every mutating action, filterable by
  member, module and date range.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17 (standalone components, signals, `@if`/`@for`), TypeScript, Angular Reactive Forms, Angular Router (functional guards), `HttpClient` with a functional interceptor, hand-rolled responsive CSS design system |
| Backend | Java 17, Spring Boot 3.3, Spring Security 6 + JWT (jjwt 0.12), Spring Data JPA/Hibernate, Bean Validation, a global exception handler, Maven |
| Database | MySQL 8, managed with MySQL Workbench / plain SQL scripts |
| Extras | springdoc-openapi (Swagger UI), Apache PDFBox (PDF reports), Spring `@Scheduled` jobs for reminders/overdue detection/recurring generation |

No Node.js/Express, MongoDB, PHP or Firebase anywhere in the stack, per the project brief.

## Architecture

```
Angular SPA (4200)  ──HTTPS/JSON──>  Spring Boot REST API (8080)  ──JDBC──>  MySQL
     │                                        │
     ├─ core/services (1 per module)          ├─ controller → service → repository → entity
     ├─ core/guards + interceptor (JWT)        ├─ DTOs (records) in/out — entities never
     ├─ features/<module> (standalone          │  serialized directly
     │  components: list + forms + actions)    ├─ RoomAccessService — the single gate for
     └─ shared/components (badges, modals,     │  room isolation + role/permission checks
        empty/loading states, toasts)          └─ GlobalExceptionHandler — one JSON error
                                                   shape for every failure
```

Every API response — success or failure — uses the same envelope:
`{ success, message, data, status, timestamp }`. See `docs/API.md` for the full endpoint
reference and `docs/DATABASE.md` for the schema.

## Project structure

```
roommate-management/
├── backend/                Spring Boot API
│   └── src/main/java/com/roommate/management/
│       ├── controller/      19 REST controllers
│       ├── service/         business logic (one service per module + RoomAccessService,
│       │                    ActivityLogService, NotificationService, FileStorageService)
│       ├── repository/      Spring Data JPA repositories
│       ├── entity/          JPA entities (+ entity/enums)
│       ├── dto/              request/response records
│       ├── security/        JWT filter, JwtUtil, UserDetailsService
│       ├── config/          Security, CORS, OpenAPI, file storage, seed data
│       ├── exception/       ApiResponse envelope + GlobalExceptionHandler
│       ├── scheduler/       cleaning reminders, bill overdue, recurring expenses
│       └── util/            split calculation, password generation
├── frontend/                Angular SPA
│   └── src/app/
│       ├── core/             models, services, guards, interceptor
│       ├── features/         one folder per module (dashboard, members, expenses, ...)
│       ├── layout/           the authenticated shell (sidebar + navbar)
│       └── shared/           reusable UI components
├── database/
│   ├── schema.sql            all 25 tables, keys, constraints, indexes
│   └── seed-data.sql         demo room, 4 members, sample data across every module
├── docs/
│   ├── API.md                endpoint reference
│   ├── DATABASE.md           schema documentation
│   └── SETUP.md              step-by-step local setup
└── README.md                 this file
```

## Quick start

Full details, environment variables and troubleshooting are in **`docs/SETUP.md`** — the
short version:

```bash
# 1. MySQL
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed-data.sql

# 2. Backend (http://localhost:8080)
cd backend
mvn clean install
mvn spring-boot:run

# 3. Frontend (http://localhost:4200), in a second terminal
cd frontend
npm install
ng serve
```

Then open **http://localhost:4200** and log in.

## Default development credentials

Seeded by `database/seed-data.sql` (fictional names, no real personal information):

| Email | Password | Role |
|---|---|---|
| `admin@roommates.dev` | `password123` | ADMIN |
| `rahul@roommates.dev` | `password123` | MEMBER |
| `akash@roommates.dev` | `password123` | MEMBER |
| `priya@roommates.dev` | `password123` | MODERATOR (granted `MANAGE_TASK`, `MANAGE_GROCERY`) |

All four belong to the seeded room **"Sunrise PG - Flat 4B"**, which already has sample
expenses, bills, tasks, a cleaning rotation, messages and more so the app isn't empty on
first login. You can also just register a new account and create your own room from scratch.

## Security notes

- Every JWT-protected endpoint checks the caller's room membership before touching any
  data — this is enforced in `RoomAccessService` on the backend, not assumed from the
  frontend's routing/UI state.
- Permissions are additive on top of role: MODERATOR starts with none and is granted
  specific capabilities by the admin; the checks live in the service layer so they can't be
  bypassed by calling the API directly.
- Passwords are BCrypt-hashed (never stored or logged in plain text).
- ID proof documents are stored like any other upload but are **only ever served** through
  an authenticated, ownership-checked endpoint (`GET /api/members/{id}/id-proof`) — they are
  never reachable as a static file the way profile photos and receipts are.
- File uploads are validated server-side for size and content type, and stored under
  randomly generated filenames (the original filename is never trusted or exposed as a path).

## Documentation

- [`docs/SETUP.md`](docs/SETUP.md) — prerequisites, MySQL/backend/frontend setup, environment
  variables, running tests, troubleshooting.
- [`docs/API.md`](docs/API.md) — every endpoint, its auth requirements, and what it does.
- [`docs/DATABASE.md`](docs/DATABASE.md) — table-by-table schema documentation and the ERD.
- Once the backend is running, **Swagger UI** at `http://localhost:8080/swagger-ui.html`
  gives you the exact, always-current request/response shape for every endpoint.
