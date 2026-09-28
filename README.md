# Habit Tracker ("Cohabit")

A collaborative habit tracker built as a modular monolith (Spring Boot + PostgreSQL). You define habits, record idempotent check-ins, see streaks derived from your history, share progress with small groups ("circles"), and get a weekly digest per circle.

Live app link: https://cohabit-r62g.onrender.com

- Design and module boundaries: [ARCHITECTURE.md](ARCHITECTURE.md). Its **Decisions Made** section covers streak rules, timezones and access rules.
- Web UI: plain HTML/CSS/JS in [src/main/resources/static/](src/main/resources/static/), served by the same app at `/`.

**Two sign-in modes**, chosen by the server:

| Mode | When | Who you are |
|---|---|---|
| **Local (dev)** | `mvn spring-boot:run` on your machine, and the automated tests | You pick a name in the browser; every request sends it as the `X-User-Id` header, which the server trusts. Handy for testing, **never for sharing**. |
| **Google** | The `google` Spring profile, which is what the Docker image and Render deployment use | You sign in with your Google account; the server keeps a session. `X-User-Id` is ignored. |

---

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java (JDK) | 21 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Docker (Desktop or Engine) with Compose | any recent | `docker info` |

Docker is needed both for the local database and for the automated tests (Testcontainers starts a throwaway PostgreSQL for each test run).

---

## 2. Run the automated tests

Docker must be running. The tests do **not** use your local dev database.

```bash
mvn test
```

All tests should pass. What they cover:

| Kind | Where | What it proves |
|---|---|---|
| Domain unit tests | `src/test/java/**/domain/` | Schedule validation, streak rules for all three schedule types (missed days, non-consecutive weekly targets, non-required weekdays, the open current period), local-date resolution across timezones, circle membership rules |
| Application unit tests | `**/application/*Test.java` | Check-in race handling, joint check-ins, digest content and failure isolation (repositories mocked) |
| Integration tests | `*IntegrationTest.java` | Full HTTP → service → real PostgreSQL round trips, including 403/404/400 responses |
| Google sign-in mode | `users/GoogleAuthIntegrationTest` | Public landing page and `/auth/config`, `401` for the API when signed out, redirect to Google, one stable user per Google account, CSRF required for writes, `X-User-Id` ignored, logout |
| Concurrency | `checkins/ConcurrentCheckInIntegrationTest` | 8 simultaneous check-ins for the same day → exactly one row, one `201` and seven `200`s |
| Architecture | `architecture/ModuleBoundaryTest` | No module reaches into another module's `infrastructure` package; domain code has no Spring/JPA imports |

Run a subset:

```bash
mvn test -Dtest=ProgressCalculatorTest                 # one class
mvn test -Dtest='ProgressCalculatorTest$Daily'         # one nested group
mvn test -Dtest='GroupIntegrationTest#oneMembers*'     # matching methods
```

Reports are written to `target/surefire-reports/`.

---

## 3. Run the app locally

### 3.1 Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL 16 on **localhost:5433** (not the default 5432, to avoid clashing with a locally installed Postgres). The database, user and password are all `habit_tracker`, and the data persists in a Docker volume.

### 3.2 Start the application

```bash
mvn spring-boot:run
```

When you see `Started HabitTrackerApplication`, the app is listening on **http://localhost:8080**. Flyway applies any pending migrations (`V1`–`V7`) automatically on startup. This runs in **local (dev) mode**, and the log warns that `X-User-Id` is trusted. That's expected on your machine.

Stop it with `Ctrl+C`. Stop the database with `docker compose stop`.

---

## 4. Test the UI by hand

Open **http://localhost:8080** in a browser.

### 4.1 First run: landing page and identity

1. The first visit shows the **landing page** ("Habits you share a roof with."). Try **How it works** and **Circles** in the header: they scroll to those sections.
2. Click **Get started** (or **Log in** / **Start a circle**). In local mode the app asks for a **display name**, then generates a user id and keeps it in the browser's `localStorage`. Your timezone is detected from the browser and sent with every request.
3. **Log out** is at the bottom of the profile menu (bottom of the sidebar). In local mode it returns you to the landing page but remembers your identities, so **Log in** lets you pick one again. In Google mode it ends your session.

### 4.2 Personal habits (Today screen)

1. Click **New habit** and create one of each schedule type:
   - *Every day*
   - *N times a week* (for example 4)
   - *Chosen weekdays* (for example Mon/Wed/Fri)
2. Click the round **check-in button** at the left of a habit. It turns done, and the streak and weekly count update.
3. **Checked in by mistake?** Click the done button again: the app asks *"Uncheck …?"*, and confirming removes today's check-in (the streak and weekly count go back). The same option is on the habit's detail page as **Checked in today · Uncheck**. Only today's check-in can be undone.
   (Repeating a check-in never creates a duplicate. The server answers `200` with the existing record; section 5.1 shows how to see it with curl.)
4. Check that a habit you haven't done today says **Today open** (or **This week open** for weekly habits), not missed.
5. Check the weekday habit: days outside its schedule are shown as days off, and they never break the streak.
6. A brand-new habit says **No streak yet** / **No weeks on target yet**; days before it was created are never counted as missed.

### 4.3 Habit detail

Click a habit to see its current streak, best run, "this week" progress, the 12-week *weeks on target* row and the heatmap. From here you can **Share with a circle** or **Archive** it.

### 4.4 Circles: test collaboration in one browser

Use the **Profile · switch user** menu (bottom of the sidebar) to act as several people:

1. As **Maya**: create a circle (for example "Flat 4B").
2. Open the circle, click **+ Invite someone**, then **Create invite link**, and **Copy** the link. It works for anyone, any number of times, for 7 days.
3. Open the profile menu, choose **New identity**, and create **Dev**. Paste the invite link into the address bar: you'll see **Join Flat 4B**. Click **Join circle**.
   (In local mode the invite dialog also has an *add by user id* form; that's what the profile menu's **Copy** id is for.)
4. As Maya, **share** a personal habit with the circle (from the habit's detail page).
5. As Maya, create a **joint habit** in the circle (for example "Kitchen reset", every day).
6. Switch to **Dev** and open the circle:
   - Maya's shared habit and her progress are visible, but Dev cannot check in for her.
   - Open the joint habit and click **Check in for the group**. The app says *"Covered for the group"*.
7. Switch to **Maya** and check in to the same joint habit. The app says *"Already covered — changed nothing"*, and the day still shows Dev as the one who covered it.
   Maya gets no **Undo my check-in** link, because only the member who covered the day can undo it. Switch to Dev to see it.
8. The circle page now shows the combined weekly count ("N of M across the circle"), the joint habit's *who covered it* split, and the activity feed.
9. As Dev, **leave** the circle. Dev loses access, and any habits Dev shared disappear from the circle.

To try another person's view on a separate device or browser, use a private window. Identities are per browser.

### 4.5 Notifications

Open **Notifications** (bell icon):

- Toggle the **weekly digest** and set the **timezone** that defines "Sunday evening" for you.
- The **delivery log** lists digests that have been sent to you.

Each circle page also shows a **digest preview**: what this week's digest would say right now.

When digests are actually sent: an hourly job sends each circle member one digest per week, from **Sunday 18:00 in that member's preference timezone**. To see a real delivery, have the app running at the top of an hour after Sunday 18:00 in your preference timezone. The digest text also appears in the app log (`Notification to <user>: ...`), because the first delivery channel only logs. Running the job again in the same week does not send a second digest.

### 4.6 Theme and layout

- Toggle **Light / Dark / System** and check that both themes are readable.
- Narrow the window to phone width (about 380px). The sidebar collapses into a mobile bar.

---

## 5. Test the backend API directly (curl)

These examples use **local (dev) mode**, where every request identifies the caller with `X-User-Id`. In Google mode the API needs a browser session instead, so use the UI there. Anything that depends on "today" also needs `X-Timezone` (an IANA zone such as `Asia/Kolkata`). Errors come back as `{"message": "..."}` with status 400, 403 or 404.

The snippets below use bash and `python3` to pull ids out of the JSON. `jq -r .id` works too if you have it.

```bash
BASE=http://localhost:8080
MAYA=11111111-1111-1111-1111-111111111111
DEV=22222222-2222-2222-2222-222222222222
ZONE=Asia/Kolkata
id() { python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])'; }
```

### 5.1 Habits, check-ins, streaks

```bash
# Create a personal habit (201)
HID=$(curl -s -X POST $BASE/habits -H "X-User-Id: $MAYA" -H 'Content-Type: application/json' \
  -d '{"name":"Read 20 pages","scheduleType":"N_TIMES_PER_WEEK","timesPerWeek":6}' | id)

# Other schedule bodies:
#   {"name":"Meditate","scheduleType":"DAILY"}
#   {"name":"Gym","scheduleType":"SPECIFIC_WEEKDAYS","weekdays":["MONDAY","WEDNESDAY","FRIDAY"]}
# Invalid, e.g. timesPerWeek 10 -> 400 with a message

curl -s $BASE/habits -H "X-User-Id: $MAYA"                       # my active habits

# Check in: 201 the first time, 200 with the same record after that (idempotent)
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$HID/checkins -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$HID/checkins -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# Undo today's check-in (checked in by mistake): 204, and 204 again if there's nothing left to undo
curl -s -w '[%{http_code}]\n' -X DELETE $BASE/habits/$HID/checkins/today -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# Someone else can't check in to Maya's habit (403)
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$HID/checkins -H "X-User-Id: $DEV" -H "X-Timezone: $ZONE"

# Derived progress: streak, current period state, this week, last 12 weeks
curl -s $BASE/habits/$HID/progress -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# History by local-date range (max 400 days)
curl -s "$BASE/habits/$HID/checkins?from=2026-09-01&to=2026-09-30" -H "X-User-Id: $MAYA"

# Archive: the owner only (403 for anyone else); for a joint habit, any member of its circle
curl -s -X PATCH $BASE/habits/$HID/archive -H "X-User-Id: $MAYA"
```

### 5.2 Circles, shared and joint habits

```bash
GID=$(curl -s -X POST $BASE/groups -H "X-User-Id: $MAYA" -H 'Content-Type: application/json' \
  -d '{"name":"Flat 4B","displayName":"Maya"}' | id)

# Any member can invite (idempotent)
curl -s -X POST $BASE/groups/$GID/members -H "X-User-Id: $MAYA" -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$DEV\",\"displayName\":\"Dev\"}"

# Share a personal habit with the circle (visibility only) -> 204
curl -s -w '[%{http_code}]\n' -X POST $BASE/groups/$GID/shared-habits -H "X-User-Id: $MAYA" \
  -H 'Content-Type: application/json' -d "{\"habitId\":\"$HID\"}"

# Dev can now read Maya's progress (200), but still can't check in for her (403)
curl -s -o /dev/null -w '%{http_code}\n' $BASE/habits/$HID/progress -H "X-User-Id: $DEV" -H "X-Timezone: $ZONE"

# Joint habit owned by the circle
JID=$(curl -s -X POST $BASE/groups/$GID/habits -H "X-User-Id: $MAYA" -H 'Content-Type: application/json' \
  -d '{"name":"Kitchen reset","scheduleType":"DAILY"}' | id)

# Dev covers today (201); Maya's check-in then returns Dev's record (200, "already covered").
# Only Dev can undo it: DELETE .../checkins/today as Maya -> 403, as Dev -> 204.
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$JID/checkins -H "X-User-Id: $DEV"  -H "X-Timezone: $ZONE"
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$JID/checkins -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# Aggregated circle progress: totals, per-habit progress, coverage by performer, activity feed
curl -s $BASE/groups/$GID/progress -H "X-User-Id: $DEV" -H "X-Timezone: $ZONE"

# Invite link: any member creates one (valid 7 days, multi-use); anyone with the token can join
TOKEN=$(curl -s -X POST $BASE/groups/$GID/invites -H "X-User-Id: $MAYA" | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
PRIYA=33333333-3333-3333-3333-333333333333
curl -s $BASE/invites/$TOKEN -H "X-User-Id: $PRIYA"                  # circle name, member count, alreadyMember
curl -s -X POST $BASE/invites/$TOKEN/accept -H "X-User-Id: $PRIYA" -H 'Content-Type: application/json' \
  -d '{"displayName":"Priya"}'                                        # joins (idempotent)

curl -s $BASE/groups -H "X-User-Id: $DEV"                          # circles I belong to
curl -s -X DELETE $BASE/groups/$GID/members/$DEV -H "X-User-Id: $DEV"   # leave (204)
```

Membership rules: any member may invite (by link, or by user id in local mode); a member may leave; only the creator may remove other people.

### 5.3 Notifications

```bash
curl -s $BASE/me/notification-preferences -H "X-User-Id: $MAYA"        # defaults: enabled, UTC
curl -s -X PUT $BASE/me/notification-preferences -H "X-User-Id: $MAYA" \
  -H 'Content-Type: application/json' -d '{"weeklyDigestEnabled":true,"timezone":"Asia/Kolkata"}'
curl -s $BASE/groups/$GID/digest -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"   # preview
curl -s $BASE/me/notifications -H "X-User-Id: $MAYA"                    # delivery log
```

---

## 6. Inspect the database

```bash
docker compose exec postgres psql -U habit_tracker -d habit_tracker
```

Useful queries:

```sql
\dt                                                        -- tables
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT habit_id, group_id, performed_by_user_id, local_date, recorded_at
  FROM checkins ORDER BY recorded_at DESC LIMIT 10;       -- one row per habit+day, never duplicated
SELECT user_id, group_id, period_start, status, subject
  FROM notification_deliveries ORDER BY attempted_at DESC;
```

Note: streaks and progress are **not stored anywhere**. They are recomputed from `checkins` on every request.

### Reset to an empty database

```bash
docker compose down -v      # removes the data volume
docker compose up -d        # fresh database; migrations re-run on next app start
```

In the browser, clear the site's local storage (or use a private window) to drop your saved identities.

---

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| App fails with `Connection refused` to `localhost:5433` | Start the database: `docker compose up -d` |
| Tests fail with `Could not find a valid Docker environment` | Start Docker Desktop; Testcontainers needs it |
| `Port 8080 is already in use` | Stop the other process, or run with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| Flyway `Validate failed: Migrations have failed validation` | An applied migration file was edited. Migrations are immutable: revert the edit and add a new `V<n>__*.sql`. On a throwaway dev DB you can also reset (section 6). |
| `400 Unknown time-zone ID` | `X-Timezone` must be an IANA zone such as `Europe/London`, not an abbreviation like `IST` |
| `403` on a habit or circle | Expected for non-owners and non-members; check which `X-User-Id` you're sending (in the UI: profile menu) |
| Google: `Error 400: redirect_uri_mismatch` | The **Authorized redirect URI** in Google Cloud must be exactly `https://<your-host>/login/oauth2/code/google` (or `http://localhost:8080/login/oauth2/code/google` locally) |
| Google: "Access blocked" / "app has not completed verification" | While the consent screen is in *Testing*, only the Google accounts listed as **test users** can sign in. Add your friends' addresses there |
| Google mode: every button returns `403` | CSRF token missing: hard-refresh the page so the browser picks up the `XSRF-TOKEN` cookie. Locally over plain http, also set `SESSION_COOKIE_SECURE=false` |
| Google mode: signed out after a redeploy | Sessions are kept in memory, so a restart signs everyone out. Sign in again |
| UI shows stale data after switching users | Reload the page; each identity's data is fetched fresh |
| A UI change isn't showing up | Restart the app (`Ctrl+C`, then `mvn spring-boot:run`) and hard-refresh the browser (`Cmd+Shift+R` / `Ctrl+Shift+R`); the browser caches the JS files |

---

## 8. Try Google sign-in on your machine (optional)

Useful to check the Google flow before deploying.

1. Create Google OAuth credentials as in section 9, step 2, and add this **Authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`
2. Run:

   ```bash
   SPRING_PROFILES_ACTIVE=google \
   GOOGLE_CLIENT_ID=<your client id> \
   GOOGLE_CLIENT_SECRET=<your client secret> \
   SESSION_COOKIE_SECURE=false \
   mvn spring-boot:run
   ```

   `SESSION_COOKIE_SECURE=false` is only for plain-http localhost. Never set it on a real deployment.
3. Open http://localhost:8080: the landing page now says **Continue with Google**.

---

## 9. Deploy to Render (share with friends)

What you'll end up with: `https://<name>.onrender.com`, where friends sign in with Google, create circles and invite each other by link. The repo includes a [`Dockerfile`](Dockerfile) and a Render Blueprint, [`render.yaml`](render.yaml), which creates the web service and a PostgreSQL database together.

### Step 1: Push the repo to GitHub

Render deploys from your GitHub repository (already `ab1109/habit-tracker`). Make sure `main` is pushed.

### Step 2: Create the Render services

1. Sign in at https://render.com (you can sign in with GitHub) and allow access to the repository.
2. Click **New → Blueprint**, pick the repository, and confirm. Render reads `render.yaml` and proposes:
   - **cohabit-db**: PostgreSQL
   - **cohabit**: the web service, built from the `Dockerfile`, with `SPRING_PROFILES_ACTIVE=google` and the database connection filled in automatically
3. When it asks for `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, you can enter placeholders for now; step 3 gives you the real values.
4. Apply. The first build takes a few minutes. Note the service URL, e.g. `https://cohabit-xxxx.onrender.com`.

### Step 3: Create Google sign-in credentials

1. Go to https://console.cloud.google.com and create a project (e.g. "Cohabit").
2. **APIs & Services → OAuth consent screen**: choose **External**, fill in the app name ("Cohabit"), your support email and developer email. The scopes you need are only `openid`, `email` and `profile` (the defaults).
3. Under **Test users**, add your own Google address and your friends' addresses. While the app is in *Testing* status, only these accounts can sign in. That's the simplest setup for a small group, and needs no Google review.
4. **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Application type: **Web application**
   - **Authorized redirect URIs**: `https://<your-service>.onrender.com/login/oauth2/code/google` (use your URL from step 2; it must match exactly)
5. Copy the **Client ID** and **Client secret**.

### Step 4: Add the credentials to Render

In Render, open the **cohabit** service → **Environment**, set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to the real values, and save. Render redeploys automatically.

### Step 5: Check it and share

1. Open `https://<your-service>.onrender.com`. You should see the landing page with **Continue with Google**.
2. Sign in, create a circle, click **+ Invite someone → Create invite link**, and send the link to a friend (who must be in the test-user list from step 3).
3. Your friend opens the link, signs in with Google, and taps **Join circle**.

Every push to `main` redeploys automatically.

### Good to know about Render's free tier

Plans and limits change, so check Render's current pricing page. At the time of writing:
- **Free web services sleep when idle.** The first visit after a quiet spell takes up to about a minute to wake. The weekly digest job only runs while the service is awake, so on the free tier Sunday digests can be delayed or skipped. A paid instance avoids both.
- **Free PostgreSQL databases expire** after a limited period. Upgrade the database, or back it up, before relying on it.
- **Sessions are in memory:** each redeploy signs everyone out. Signing in again is enough, and no data is lost.

### Other hosts

The same image runs anywhere that runs Docker containers. Set these environment variables:

| Variable | Value |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Your PostgreSQL 16 connection |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | From step 3 (with that host's redirect URI) |
| `PORT` | Port to listen on (default 8080) |
| `SPRING_PROFILES_ACTIVE` | `google` (already the image's default) |

Serve it over HTTPS behind a proxy that sets `X-Forwarded-Proto`/`X-Forwarded-Host`; the app uses them to build the Google redirect URI.

---

## 10. Project layout

```text
src/main/java/com/habittracker/
  habits/         habit definitions and schedules
  checkins/       check-in recording and history; access policy
  streaks/        streak and weekly-progress calculation (pure)
  groups/         circles, membership, shared/joint habits, group progress
  notifications/  preferences, weekly digest, delivery log
  users/          sign-in (Google or local header), accounts, security config
  common/         shared exceptions, error handling, clock and scheduling config
src/main/resources/
  db/migration/   Flyway migrations V1–V7 (never edit an applied one)
  static/         web UI
```

Each module follows `api` → `application` → `domain` ← `infrastructure`: domain code is plain Java, and persistence lives behind repository interfaces.

### Current limitations

- Digest delivery only writes to the log and the in-app Notifications page. There's no email yet.
- Sessions are kept in memory, so a restart signs everyone out and the app runs as a single instance.
- You can't rename a habit or change its schedule (archive it and create a new one), and only today's check-in can be unchecked.
- Not built from the design mock: live updates (pages refresh when you navigate), the wall chat, nudges, and habit notes.
