# API Documentation

Base URL (local dev): `http://localhost:8080/api`

Interactive docs (once the backend is running): **Swagger UI** at
`http://localhost:8080/swagger-ui.html` (raw OpenAPI JSON at `/v3/api-docs`) — generated
directly from the controllers/DTOs, so it always reflects the exact request/response shape.
This document is the human-readable map of what's available and how authorization works;
Swagger is the source of truth for exact field names/types.

## Response envelope

Every endpoint (success or error) returns the same JSON envelope:

```json
{
  "success": true,
  "message": "Expense created",
  "data": { "...": "..." },
  "status": 200,
  "timestamp": "2026-09-11T10:15:30"
}
```

On errors, `success` is `false`, `data` is typically `null` (or, for validation errors, an
object of `{ field: message }`), and `status` mirrors the HTTP status code. Handled
centrally by `GlobalExceptionHandler` — controllers never format errors themselves.

| HTTP status | When |
|---|---|
| 400 | Validation failure, bad request body, business-rule violation (e.g. custom split doesn't sum to the total) |
| 401 | Missing/invalid/expired JWT |
| 403 | Authenticated, but lacking the role/permission for this action, or the resource belongs to a different room |
| 404 | Resource not found |
| 409 | Conflict (e.g. duplicate vote, duplicate room membership) |
| 413 | Uploaded file exceeds the configured size limit |
| 500 | Unexpected server error |

## Authentication

All endpoints except `/api/auth/**` and the Swagger/`/uploads/**` paths require a JWT:

```
Authorization: Bearer <token>
```

Obtain a token from `POST /api/auth/login` or `POST /api/auth/register`. Tokens expire
after `JWT_EXPIRATION_MS` (24h by default) — the frontend's `authInterceptor` logs the user
out automatically on a 401.

Authorization beyond "is this a valid token" is enforced **entirely server-side** in each
service via `RoomAccessService`, independent of anything the Angular UI hides or shows:

- **Room isolation** — every query is scoped to the caller's own `room_id`; a member of
  Room A gets a 403/404 (never another room's data) if they try to touch Room B's resources.
- **Role checks** — `ADMIN` can do everything; `MEMBER` can only manage their own
  profile/expenses/tasks/etc.; `MODERATOR` gets whatever subset of permissions the admin
  has explicitly granted via `member_permissions`.
- **Permission checks** — actions like approving expenses, managing bills, or removing a
  member check the specific permission code, not just "is this an admin."

## Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | public | Create a user account. Returns a JWT immediately; the user has no room yet (`roomId: null`) until they create or are added to one. |
| POST | `/login` | public | `{ email, password }` → JWT + user/room summary. |
| POST | `/forgot-password` | public | `{ email }` → always returns success (never reveals whether the email exists); generates a reset token server-side. |
| POST | `/reset-password` | public | `{ token, newPassword }` → resets the password if the token is valid and unexpired. |

## Rooms — `/api/rooms`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `` | authenticated, no existing room | Creates a room and makes the caller its ADMIN. |
| GET | `/{id}` | member of that room | Room details + member count. |

## Members — `/api/members`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `` | any member | List all members of the caller's room. |
| POST | `` | `ADD_MEMBER` (ADMIN always) | Add a member by email — finds an existing user or creates one with a generated temporary password (returned once in the response). |
| GET | `/{id}` | same room | Single member. |
| PUT | `/{id}` | self or `ADD_MEMBER`/admin | Update name/mobile/room/bed. |
| PUT | `/{id}/role` | ADMIN | Change MEMBER ↔ MODERATOR (an ADMIN can't be demoted this way — see below). |
| PUT | `/{id}/status` | ADMIN | Activate/deactivate a member (can't deactivate the ADMIN). |
| DELETE | `/{id}` | ADMIN | Soft-remove (deactivate) a member. |
| POST | `/{id}/photo` | self or ADMIN | Multipart upload, profile photo. |
| POST | `/{id}/id-proof` | self or ADMIN | Multipart upload, ID proof. |
| GET | `/{id}/id-proof` | self or ADMIN only | Streams the stored ID proof file — **never** exposed as a public static URL. |
| GET | `/{id}/permissions` | ADMIN | List all permission codes and whether this member has each granted. |
| PUT | `/{id}/permissions` | ADMIN | Bulk-update a MODERATOR's granted permissions (rejected for ADMIN targets — an admin's permissions aren't customizable). |

## Expenses — `/api/expenses`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `?status=` | any member | List expenses, optional status filter (`PENDING`/`APPROVED`/`REJECTED`). |
| POST | `` | `MANAGE_EXPENSE` or ADMIN | Create an expense with `EQUAL` or `CUSTOM` split. Auto-approved if the creator can approve expenses, otherwise `PENDING` and admins/moderators-with-permission are notified. Custom splits must sum exactly to the total (400 otherwise). |
| GET | `/{id}` | same room | Full detail incl. splits. |
| PUT | `/{id}` | creator (while PENDING) or ADMIN | Edit an expense. |
| DELETE | `/{id}` | creator (while PENDING) or ADMIN | Deletes the expense and any generated settlements. |
| POST | `/{id}/approve` | `APPROVE_EXPENSE` or ADMIN | `{ action: "APPROVE" \| "REJECT", rejectionReason? }`. Approving generates settlements. |
| POST | `/{id}/receipt` | creator or ADMIN | Multipart upload, receipt photo. |

## Settlements — `/api/settlements`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `?status=` | any member | List settlements for the room. |
| POST | `/{id}/pay` | the ower, or ADMIN | Marks a settlement `PAID` with an optional payment date. |
| POST | `/{id}/verify` | ADMIN | Confirms a "paid" claim (`verifiedByAdmin: true`). |
| GET | `/balances` | any member | Per-member `{ totalPaid, totalShare, balance }` across all approved expenses. |
| GET | `/summary` | any member | Net "who owes whom" after cancelling out pairwise debts. |

## Recurring expenses — `/api/recurring-expenses`

Standard CRUD (`GET`, `POST`, `PUT /{id}`, `DELETE /{id}`), gated by `MANAGE_EXPENSE`/ADMIN.
`PUT` also accepts `{ active: boolean }` to pause/resume. A daily scheduled job
(`RecurringExpenseScheduler`) generates the actual `expenses` rows once `nextDueDate` is due.

## Grocery / shopping list — `/api/shopping-items`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `?status=` | any member | List items. |
| POST | `` | any member | Add an item to the list. |
| PUT | `/{id}` | `MANAGE_GROCERY`/ADMIN, or the adder | Edit. |
| DELETE | `/{id}` | `MANAGE_GROCERY`/ADMIN, or the adder | Remove. |
| POST | `/{id}/item-photo` | any member | Multipart upload. |
| POST | `/{id}/bill-photo` | any member | Multipart upload. |
| POST | `/{id}/convert-to-expense` | `MANAGE_GROCERY`/ADMIN | `{ actualAmount, paidByMemberId, splitType, customSplits?, equalSplitMemberIds? }` — creates a real expense (see Expenses) from a purchased item. |

## Bills — `/api/bills`

Standard CRUD gated by `MANAGE_BILL`/ADMIN, plus `POST /{id}/photo` (multipart). `PUT`
accepts partial updates including `amountPaid`/`status`/`paidByMemberId` to record a
payment. A daily scheduler flips overdue bills to `OVERDUE` automatically.

## Tasks — `/api/tasks`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `?mine=true\|false` | any member | List all tasks, or just the caller's own. |
| POST | `` | `MANAGE_TASK`/ADMIN | Assign a task. |
| PUT | `/{id}` | the assignee (status only) or `MANAGE_TASK`/ADMIN (any field) | Update. |
| DELETE | `/{id}` | `MANAGE_TASK`/ADMIN | Remove. |

## Cleaning — `/api`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/cleaning-schedules?mine=` | any member | List schedule entries. |
| POST | `/cleaning-schedules` | `MANAGE_CLEANING`/ADMIN | One-off schedule entry. |
| PUT | `/cleaning-schedules/{id}` | assignee (status only) or `MANAGE_CLEANING`/ADMIN | Update. |
| DELETE | `/cleaning-schedules/{id}` | `MANAGE_CLEANING`/ADMIN | Remove. |
| GET | `/cleaning-rotations` | any member | List rotations with resolved member names and whose turn is next. |
| POST | `/cleaning-rotations` | `MANAGE_CLEANING`/ADMIN | Create a rotation; auto-generates the first few occurrences. |
| PUT | `/cleaning-rotations/{id}` | `MANAGE_CLEANING`/ADMIN | Edit, or `{ active: boolean }` to pause/resume. |

A daily job keeps every active rotation topped up with future occurrences and sends
day-before / on-day / overdue reminder notifications.

## Announcements — `/api/announcements`

`GET` (any member), `POST` (`MANAGE_ANNOUNCEMENT`/ADMIN), `DELETE /{id}` (`MANAGE_ANNOUNCEMENT`/ADMIN).

## Messages — `/api/messages`

| Method | Path | Description |
|---|---|---|
| GET | `/public` | Room-wide chat history. |
| POST | `/public` | `{ content }` — post to the room chat. |
| GET | `/private/{userId}` | Conversation with another member; marks their messages to you as read. |
| POST | `/private` | `{ receiverUserId, content }` — recipient must be in the same room. |

## Notifications — `/api/notifications`

`GET` (list, newest first), `GET /unread-count`, `PUT /{id}/read`, `PUT /read-all`.

## Polls — `/api/polls`

`GET` (list with vote counts + the caller's own vote), `POST` (`MANAGE_POLL`/ADMIN, `{ question, options[], closesAt? }`),
`POST /{id}/vote` (`{ optionId }`, one vote per member — 409 on a repeat vote),
`PUT /{id}/close` (`MANAGE_POLL`/ADMIN).

## Inventory — `/api/inventory`

Standard CRUD (any member can add; `MANAGE_INVENTORY`/ADMIN or the adder can edit/delete),
plus `POST /{id}/photo`.

## Issues — `/api/issues`

`GET ?status=`, `POST` (any member can report), `PUT /{id}` (`{ status }`, `MANAGE_ISSUE`/ADMIN),
`POST /{id}/photo`.

## Activity log — `/api/activity-logs`

`GET ?userId=&module=&from=&to=` — `VIEW_ACTIVITY_LOG`/ADMIN only. `module` is one of
`ROOM, MEMBER, EXPENSE, SETTLEMENT, BILL, GROCERY, TASK, CLEANING, ANNOUNCEMENT, MESSAGE, POLL, INVENTORY, ISSUE, PERMISSION, AUTH`.

## Dashboard — `/api/dashboard`

| Method | Path | Description |
|---|---|---|
| GET | `/member` | Member-facing dashboard: this month's room spend, the caller's contribution/pending amount, upcoming bills, their pending tasks, next cleaning date, unread counts, recent expenses/announcements. |
| GET | `/admin` | Admin-facing dashboard: member counts, monthly spend, pending payments/expenses/tasks, open issues, upcoming bills/cleaning, recent activity. |
| GET | `/analytics` | 6-month expense trend, category breakdown, per-member contribution, current vs. previous month totals, paid vs. pending totals. Powers **Reports → analytics**. |

## Reports — `/api/reports`

| Method | Path | `?format=` |
|---|---|---|
| GET | `/monthly-expense` | `csv` (default) or `pdf` |
| GET | `/member-contribution` | `csv` or `pdf` |
| GET | `/pending-settlement` | `csv` or `pdf` |
| GET | `/bill-report` | `csv` or `pdf` |

All four are ADMIN-only, streamed as a file download (`Content-Disposition: attachment`).
CSV is generated directly; PDF is rendered from the same data with Apache PDFBox.

## File uploads generally

All upload endpoints are `multipart/form-data` with a single `file` part, validated
server-side for size (10MB default) and content type (images: JPEG/PNG/WebP; documents:
those plus PDF). The response is `{ fileUrl }`, a relative path you resolve against the
backend's origin (handled by the frontend's `FileUploadService.resolveUrl`) — except ID
proofs, which return no browsable URL and must be fetched through the authenticated
`GET /api/members/{id}/id-proof` endpoint.
