# Database Documentation

Engine: **MySQL 8.0+**. Full DDL lives in `database/schema.sql`; sample data in
`database/seed-data.sql`. Character set is `utf8mb4` throughout.

## Entity-relationship overview

```
users ──< room_members >── rooms
  │            │
  │            ├──< member_permissions >── permissions
  │            │                                ▲
  │            │                                │
  │            └── role (roles) ──< role_permissions
  │
  ├──< expenses (paid_by, created_by, approved_by → room_members)
  │        └──< expense_splits >── room_members
  │        └──< settlements (from_member, to_member → room_members)
  │
  ├──< recurring_expenses (generates expenses on schedule)
  ├──< shopping_items (converts into an expense)
  ├──< bills
  ├──< tasks
  ├──< cleaning_rotations >──< cleaning_schedules
  ├──< announcements
  ├──< messages (sender, receiver → users)
  ├──< notifications
  ├──< polls >──< poll_options >──< poll_votes
  ├──< inventory_items
  ├──< issues
  └──< activity_logs
```

Every table that belongs to a room carries a `room_id` foreign key, which is how
room-level data isolation is enforced (in addition to the server-side checks in
`RoomAccessService` — the database constraint is a second line of defense, not the only one).

## Tables

### `users`
Login identity, independent of any room.

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK AUTO_INCREMENT | |
| `full_name` | VARCHAR(120) NOT NULL | |
| `email` | VARCHAR(160) NOT NULL UNIQUE | login identifier |
| `password` | VARCHAR(255) NOT NULL | BCrypt hash |
| `mobile_number` | VARCHAR(20) | |
| `profile_photo_url` | VARCHAR(255) | relative path, publicly viewable |
| `id_proof_url` | VARCHAR(255) | relative path, **never** exposed as a public static file |
| `enabled` | TINYINT(1) NOT NULL DEFAULT 1 | |
| `reset_token` | VARCHAR(64) | forgot-password flow |
| `reset_token_expiry` | DATETIME | |
| `created_at` / `updated_at` | DATETIME | |

### `rooms`
One row per flat/room. No landlord/owner concept — `created_by` is simply the user who
became the room's first ADMIN.

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `room_name` | VARCHAR(150) NOT NULL | |
| `address` | VARCHAR(255) NOT NULL | |
| `created_by` | BIGINT → `users.id` | |
| `status` | ENUM-like VARCHAR | `ACTIVE` / `INACTIVE` |
| `created_at` / `updated_at` | DATETIME | |

### `roles` / `permissions` / `role_permissions`
Static reference data seeded by `DataInitializer` on backend startup (idempotent — safe to
restart). Three roles: `ADMIN`, `MEMBER`, `MODERATOR`. Fourteen permission codes covering
every manageable module (`ADD_MEMBER`, `MANAGE_EXPENSE`, `APPROVE_EXPENSE`, `MANAGE_BILL`,
`MANAGE_TASK`, `MANAGE_CLEANING`, `MANAGE_GROCERY`, `MANAGE_INVENTORY`,
`MANAGE_ANNOUNCEMENT`, `MANAGE_SETTLEMENT`, `MANAGE_ISSUE`, `MANAGE_POLL`,
`REMOVE_MEMBER`, `VIEW_ACTIVITY_LOG`). ADMIN implicitly has every permission at the
application layer; `role_permissions` records which permissions the ADMIN role has by
default for reference/audit purposes.

### `room_members`
The join between a `user` and a `room` — this is the row that actually carries a role,
and almost every other table references `room_members.id` (not `users.id`) so that the
same person could in principle belong to different rooms over time without conflating
their per-room identity. (The current MVP keeps one *active* room per user, enforced in
`RoomService`/`RoomAccessService`.)

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | referred to elsewhere as "room member id" |
| `room_id` | BIGINT → `rooms.id` | |
| `user_id` | BIGINT → `users.id` | |
| `role_id` | BIGINT → `roles.id` | ADMIN / MEMBER / MODERATOR |
| `room_number` / `bed_number` | VARCHAR(30) | optional |
| `joining_date` | DATE | |
| `status` | VARCHAR | `ACTIVE` / `INACTIVE` / `DEACTIVATED` |
| UNIQUE | `(room_id, user_id)` | a user can't join the same room twice |

### `member_permissions`
Per-MODERATOR permission grants (fine-grained overrides beyond their role).

| Column | Type | Notes |
|---|---|---|
| `room_member_id` | BIGINT → `room_members.id` | |
| `permission_id` | BIGINT → `permissions.id` | |
| `granted` | TINYINT(1) | |
| UNIQUE | `(room_member_id, permission_id)` | |

### `expenses` / `expense_splits`
An expense belongs to a room, was paid by one member, and is split across one or more
members. `split_type` is `EQUAL` or `CUSTOM`; `status` is `PENDING` / `APPROVED` /
`REJECTED`. `expense_splits.share_amount` rows always sum to exactly `expenses.total_amount`
(enforced in `SplitCalculator` before the transaction commits).

| `expenses` column | Notes |
|---|---|
| `paid_by` | → `room_members.id` |
| `created_by` | → `room_members.id` — who logged it (may differ from payer) |
| `approved_by` / `approved_at` / `rejection_reason` | approval workflow |
| `recurring_expense_id` | nullable FK — set when auto-generated |
| `receipt_photo_url` | nullable |

| `expense_splits` column | Notes |
|---|---|
| `expense_id` | → `expenses.id` |
| `room_member_id` | → `room_members.id` |
| `share_amount` | DECIMAL(12,2) |
| UNIQUE | `(expense_id, room_member_id)` |

### `settlements`
Generated automatically when an expense is approved: one row per (payer, ower) pair,
excluding the payer's own share. `status` is `PENDING` / `PAID`; `verified_by_admin` is
set once an admin confirms a member's "mark as paid" claim.

### `recurring_expenses`
Template for auto-generated expenses. `RecurringExpenseScheduler` runs daily
(`0 0 1 * * *`) and calls `RecurringExpenseService.generateDueExpenses`, which creates a
real `expenses` row (+ splits + settlements) whenever `next_due_date <= today` and advances
`next_due_date` by the configured `frequency` (`DAILY` / `WEEKLY` / `MONTHLY` / `YEARLY`).

### `shopping_items`
Grocery/shopping list. `status` `PENDING` → `PURCHASED`. Converting a purchased item to an
expense (`POST /api/shopping-items/{id}/convert-to-expense`) delegates to the same
`ExpenseService` used for manual expenses, so it goes through the identical
approval/split/settlement pipeline.

### `bills`
`status` is `PENDING` / `PARTIALLY_PAID` / `PAID` / `OVERDUE`. `amount_paid` tracks partial
payments; `BillReminderScheduler` runs daily (`0 30 8 * * *`) and flips any bill past its
`due_date` to `OVERDUE`.

### `tasks`
General chores/to-dos, independent of the cleaning rotation system. `priority` is `LOW` /
`MEDIUM` / `HIGH`; `status` is `PENDING` / `IN_PROGRESS` / `COMPLETED`.

### `cleaning_rotations` / `cleaning_schedules`
A rotation defines an ordered list of members (`member_order_csv`, a CSV of
`room_members.id` values) for a `task_type` (KITCHEN / BATHROOM / ROOM / GARBAGE /
COMMON_AREA / OTHER) and an interval in days. `CleaningService.generateOccurrences` walks
the rotation to create concrete `cleaning_schedules` rows; `extendActiveRotations` (run
daily alongside the reminder scheduler) keeps at least 2 future occurrences generated at
all times so the rotation never runs dry. Standalone one-off `cleaning_schedules` rows
(no rotation) are also supported — `rotation_id` is nullable.
`CleaningReminderScheduler` (daily, `0 0 8 * * *`) sends day-before / on-day / overdue
notifications, each flagged so it's only sent once (`reminder_day_before_sent`,
`reminder_on_day_sent`, `reminder_overdue_sent`).

### `announcements`
Room-wide notices, admin/moderator authored.

### `messages`
Both public room chat and private 1:1 messages live in one table; `type` is `PUBLIC` or
`PRIVATE`, `receiver_id` is null for public messages.

### `notifications`
Centralized notification feed (14 types — expense submitted/approved/rejected, settlement
requested/verified, bill due/overdue, task assigned, cleaning reminders, announcement
posted, poll created, issue reported/resolved, member added) fanned out by
`NotificationService.notify()` from every other service.

### `polls` / `poll_options` / `poll_votes`
One vote per member per poll (`UNIQUE(poll_id, voter_id)` on `poll_votes`, plus an explicit
application-level check). `status` is `OPEN` / `CLOSED`.

### `inventory_items`
Shared belongings. `condition_status` column (mapped from the `condition` Java field —
`condition` is a reserved-ish word best avoided as a raw column name) is `WORKING` /
`DAMAGED` / `LOST` / `DISPOSED`.

### `issues`
Maintenance/repair reports. `priority` `LOW`/`MEDIUM`/`HIGH`/`URGENT`; `status`
`OPEN`/`IN_PROGRESS`/`RESOLVED`.

### `activity_logs`
Append-only audit trail. Every mutating service call writes one row here via
`ActivityLogService.log(room, user, module, action, referenceId, description)`. Queried
with dynamic filters (user, module, date range) using JPA Specifications from
`GET /api/activity-logs`.

## Indexes

Beyond primary/foreign keys and the uniques noted above, composite indexes exist on the
hot query paths: `expenses(room_id, expense_date)`, `settlements(room_id, status)`,
`cleaning_schedules(room_id, cleaning_date)`, `messages(room_id, type)`,
`notifications(user_id, is_read)`, `activity_logs(room_id, created_at)`.

## Why `ddl-auto: none`

Hibernate is configured with `ddl-auto: none` (see `backend/src/main/resources/application.yml`)
rather than `validate` or `update`. `schema.sql` is the single source of truth for the
schema; the JPA entities are written to match it exactly, field-for-field. This was a
deliberate choice made because this environment could not run `mvn compile` to verify the
entity↔schema mapping automatically — removing Hibernate's own startup validation avoids a
second, unverified assumption stacking on top of the first. If you modify an entity, update
`schema.sql` (and `seed-data.sql` if relevant) to match, and vice versa.
