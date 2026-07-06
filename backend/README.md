# Aeon Backend

Production-oriented backend scaffold for the Aeon mobile app. This backend is built for Render and targets Supabase PostgreSQL. The first scope covers only the current `finance`, `focus`, and `tasks` domains.

## Stack

- `Fastify` for the HTTP service layer
- `postgres.js` for direct PostgreSQL access
- `Supabase Auth JWT` verification through JWKS using `jose`
- plain SQL migrations with an internal migration runner
- strict TypeScript with Vitest domain tests

## Architecture

- `src/app.ts`: service composition, plugins, and route registration
- `src/plugins/db.ts`: pooled PostgreSQL connection
- `src/plugins/auth.ts`: bearer-token verification for Aeon's first-party JWTs, with optional Supabase JWT compatibility
- `src/modules/auth`: email OTP, password signup, refresh-session, and provider status APIs
- `src/modules/tasks`: task, project, subtask, reminder, and completion-log APIs
- `src/modules/focus`: focus session, routine, occurrence, and routine-log APIs
- `src/modules/finance`: account, category, transaction, budget, and overview APIs
- `src/modules/bootstrap`: idempotent defaults for finance categories, wallet account, and focus templates
- `db/migrations`: production schema changes tracked in SQL

## Data Model

The schema is multi-tenant by design:

- every domain table is scoped by `user_id`
- primary keys are composite: `(user_id, id)`
- IDs remain compatible with the app’s local-first prefixed string IDs
- `created_at`, `updated_at`, and `deleted_at` are present for sync safety and soft deletion

Current schema coverage:

- `tasks`, `task_projects`, `task_subtasks`, `task_reminders`, `task_completion_logs`
- `focus_sessions`, `focus_routine_templates`, `focus_routine_items`, `focus_routine_occurrences`, `focus_routine_logs`
- `finance_accounts`, `finance_categories`, `finance_transactions`, `finance_budgets`
- `app_users`

## Render Setup

1. Create a Supabase project and keep the PostgreSQL connection string ready.
2. Create a Render Web Service from this repository.
3. Point Render to [render.yaml](/E:/Apps/tracking/backend/render.yaml:1) or configure the same values manually.
4. Set the required environment variables:

- `DATABASE_URL`
- `AUTH_JWT_SECRET`
- `AUTH_TOKEN_HASH_PEPPER`
- `AUTH_EMAIL_FROM`
- `RESEND_API_KEY`
- `NODE_ENV=production`

Recommended:

- use the Supabase direct database URL if your network path supports it
- otherwise use the Supabase session pooler URL on port `5432`
- keep `prepare: false` as configured because pooled PostgreSQL setups are more stable that way
- keep `AUTH_JWT_SECRET` and `AUTH_TOKEN_HASH_PEPPER` different from each other

Optional compatibility / provider variables:

- `SUPABASE_URL`
- `SUPABASE_JWT_AUDIENCE`
- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `GOOGLE_OAUTH_REDIRECT_URI`
- `AUTH_ALLOWED_MOBILE_REDIRECT_URIS`

## Auth Notes

- Clients should never connect to the database directly for these domains.
- The mobile app can authenticate against this backend using email OTP plus password and then call domain APIs with the returned bearer access token.
- Refresh tokens are opaque, hashed before storage, and rotated on refresh.
- Signup OTP emails are sent through Resend.
- The backend still accepts Supabase JWTs when `SUPABASE_URL` is configured, so existing external auth can coexist during migration.

## Google OAuth For Android

Current production callback values for this project:

- Google OAuth redirect URI: `https://aeon-9cds.onrender.com/v1/auth/google/callback`
- Mobile app deep link URI: `aeon://auth/callback`
- Render env `AUTH_ALLOWED_MOBILE_REDIRECT_URIS`: `aeon://auth/callback`

Production notes:

- keep `GOOGLE_OAUTH_CLIENT_SECRET` only in Render environment variables
- do not commit Google secrets into the repo, screenshots, or `.env.example`
- set `CORS_ORIGIN` explicitly in production; wildcard `*` is blocked now
- if your Google OAuth app is still in testing mode, only listed test users can sign in
- publish the OAuth consent screen before treating Google sign-in as public production auth

## Local Development

```bash
cd backend
npm install
cp .env.example .env
npm run db:migrate
npm run dev
```

## Scripts

```bash
npm run build
npm run test
npm run db:migrate
npm run dev
```

## Render Free Keep-Alive Setup

After deployment, open this URL in your browser:

`https://YOUR_RENDER_BACKEND_URL.onrender.com/api/health`

Expected response:

```json
{
  "ok": true,
  "service": "aeon-backend",
  "status": "healthy"
}
```

Route summary:

- `GET /api/health`: public shallow health route for Render free keep-alive monitors
- `HEAD /api/health`: lightweight public probe with no response body
- `GET /api/health/deep`: public dependency-aware health route for manual diagnostics
- `GET /health/live`: legacy lightweight liveness route kept for compatibility
- `GET /health/ready`: legacy readiness route kept for compatibility and database-aware checks

Important behavior:

- `/api/health` does not touch the database, external APIs, Google OAuth, Resend, or file storage
- `/api/health/deep` performs a minimal database probe with `select 1`
- Redis is not part of the current Aeon backend, so `/api/health/deep` reports `redis: false` without failing the response
- health routes are public and do not require authentication
- health routes use quieter route logging to avoid noisy request logs from external monitors

Environment reference values:

- `BACKEND_PUBLIC_URL=https://YOUR_RENDER_BACKEND_URL.onrender.com`
- `HEALTH_CHECK_PATH=/api/health`

Then create an external monitor:

Option A: UptimeRobot

- Monitor Type: HTTP(s)
- Friendly Name: Aeon Backend Keep Alive
- URL: `https://YOUR_RENDER_BACKEND_URL.onrender.com/api/health`
- Interval: 5 minutes

Option B: cron-job.org

- URL: `https://YOUR_RENDER_BACKEND_URL.onrender.com/api/health`
- Method: GET
- Schedule: every 5 minutes

## API Surface

### Tasks

- `GET /v1/tasks`
- `GET /v1/tasks/dashboard`
- `GET /v1/tasks/projects`
- `POST /v1/tasks/projects`
- `DELETE /v1/tasks/projects/:projectId`
- `GET /v1/tasks/completion-logs`
- `GET /v1/tasks/:taskId`
- `GET /v1/tasks/:taskId/subtasks`
- `GET /v1/tasks/:taskId/reminders`
- `POST /v1/tasks`
- `PATCH /v1/tasks/:taskId`
- `POST /v1/tasks/:taskId/complete`
- `POST /v1/tasks/:taskId/reopen`
- `POST /v1/tasks/:taskId/snooze`
- `DELETE /v1/tasks/:taskId`

### Focus

- `GET /v1/focus/templates`
- `GET /v1/focus/items`
- `POST /v1/focus/items`
- `PATCH /v1/focus/items/:itemId`
- `DELETE /v1/focus/items/:itemId`
- `GET /v1/focus/occurrences`
- `GET /v1/focus/dashboard`
- `POST /v1/focus/occurrences/:occurrenceId/start`
- `POST /v1/focus/occurrences/:occurrenceId/done`
- `POST /v1/focus/occurrences/:occurrenceId/skip`
- `POST /v1/focus/occurrences/:occurrenceId/miss`
- `POST /v1/focus/occurrences/:occurrenceId/snooze`
- `POST /v1/focus/occurrences/:occurrenceId/reschedule`
- `GET /v1/focus/sessions`
- `POST /v1/focus/sessions`
- `POST /v1/focus/sessions/:sessionId/complete`
- `POST /v1/focus/sessions/:sessionId/cancel`

### Finance

- `GET /v1/finance/categories`
- `POST /v1/finance/categories`
- `DELETE /v1/finance/categories/:categoryId`
- `GET /v1/finance/accounts`
- `POST /v1/finance/accounts`
- `DELETE /v1/finance/accounts/:accountId`
- `GET /v1/finance/transactions`
- `GET /v1/finance/transactions/:transactionId`
- `POST /v1/finance/transactions`
- `DELETE /v1/finance/transactions/:transactionId`
- `GET /v1/finance/budgets`
- `POST /v1/finance/budgets/month`
- `GET /v1/finance/overview/:month`

### Auth

- `GET /v1/auth/providers`
- `POST /v1/auth/signup/request-otp`
- `POST /v1/auth/signup/verify-otp`
- `POST /v1/auth/signup/complete`
- `POST /v1/auth/signin/password`
- `GET /v1/auth/google/start`
- `GET /v1/auth/google/callback`
- `POST /v1/auth/google/exchange`
- `POST /v1/auth/session/refresh`
- `POST /v1/auth/signout`
- `GET /v1/auth/me`

## Implementation Notes

- Finance defaults are seeded lazily per user: category catalog plus a default `Wallet` account.
- Focus defaults are seeded lazily per user: the current template catalog from the app.
- Finance writes trigger account balance recalculation and monthly budget spend recalculation.
- Focus occurrence generation follows the same daily, weekday, weekend, and `custom:` repeat rules already used in the app.
- Task completion can generate the next recurring task when the stored recurrence rule requires it.
