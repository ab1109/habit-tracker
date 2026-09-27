# Habit Tracker ("Cohabit")

A collaborative habit tracker built as a modular monolith (Spring Boot + PostgreSQL). You define habits, record idempotent check-ins, see streaks derived from your history, share progress with small groups ("circles"), and get a weekly digest per circle.

- Design and module boundaries: [ARCHITECTURE.md](ARCHITECTURE.md). Its **Decisions Made** section covers streak rules, timezones and access rules.
- Web UI: plain HTML/CSS/JS in [src/main/resources/static/](src/main/resources/static/), served by the same app at `/`.

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

When you see `Started HabitTrackerApplication`, the app is listening on **http://localhost:8080**. Flyway applies any pending migrations (`V1`–`V5`) automatically on startup.

Stop it with `Ctrl+C`. Stop the database with `docker compose stop`.

---

## 4. Test the UI by hand

Open **http://localhost:8080** in a browser.

### 4.1 First run: create an identity

There is no login yet. On first load the app asks for a **display name**, then generates a user id and stores it in the browser's `localStorage`. Your timezone is detected from the browser and sent with every request.

### 4.2 Personal habits (Today screen)

1. Click **New habit** and create one of each schedule type:
   - *Every day*
   - *N times a week* (for example 4)
   - *Chosen weekdays* (for example Mon/Wed/Fri)
2. Click the round **check-in button** at the left of a habit. It turns done, and the streak and weekly count update.
3. Click it again. The app says *"Already checked in today. Checking in again changed nothing."*: the server returned `200` and did not create a second record.
4. Check that a habit you haven't done today says **Today open** (or **This week open** for weekly habits), not missed.
5. Check the weekday habit: days outside its schedule are shown as days off, and they never break the streak.
6. A brand-new habit says **No streak yet** / **No weeks on target yet**; days before it was created are never counted as missed.

### 4.3 Habit detail

Click a habit to see its current streak, best run, "this week" progress, the 12-week *weeks on target* row and the heatmap. From here you can **Share with a circle** or **Archive** it.

### 4.4 Circles: test collaboration in one browser

Use the **Profile · switch user** menu (bottom of the sidebar) to act as several people:

1. As **Maya**: create a circle (for example "Flat 4B").
2. Open the profile menu, choose **New identity**, and create **Dev**. Copy Dev's user id with **Copy**.
3. Switch back to **Maya**, open the circle, click **+ Invite someone**, and paste Dev's id with the name "Dev".
4. As Maya, **share** a personal habit with the circle (from the habit's detail page).
5. As Maya, create a **joint habit** in the circle (for example "Kitchen reset", every day).
6. Switch to **Dev** and open the circle:
   - Maya's shared habit and her progress are visible, but Dev cannot check in for her.
   - Open the joint habit and click **Check in for the group**. The app says *"Covered for the group"*.
7. Switch to **Maya** and check in to the same joint habit. The app says *"Already covered — changed nothing"*, and the day still shows Dev as the one who covered it.
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

Every request identifies the caller with `X-User-Id`. Anything that depends on "today" also needs `X-Timezone` (an IANA zone such as `Asia/Kolkata`). Errors come back as `{"message": "..."}` with status 400, 403 or 404.

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

# Someone else can't check in to Maya's habit (403)
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$HID/checkins -H "X-User-Id: $DEV" -H "X-Timezone: $ZONE"

# Derived progress: streak, current period state, this week, last 12 weeks
curl -s $BASE/habits/$HID/progress -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# History by local-date range (max 400 days)
curl -s "$BASE/habits/$HID/checkins?from=2026-09-01&to=2026-09-30" -H "X-User-Id: $MAYA"

# Archive
curl -s -X PATCH $BASE/habits/$HID/archive
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

# Dev covers today (201); Maya's check-in then returns Dev's record (200, "already covered")
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$JID/checkins -H "X-User-Id: $DEV"  -H "X-Timezone: $ZONE"
curl -s -w ' [%{http_code}]\n' -X POST $BASE/habits/$JID/checkins -H "X-User-Id: $MAYA" -H "X-Timezone: $ZONE"

# Aggregated circle progress: totals, per-habit progress, coverage by performer, activity feed
curl -s $BASE/groups/$GID/progress -H "X-User-Id: $DEV" -H "X-Timezone: $ZONE"

curl -s $BASE/groups -H "X-User-Id: $DEV"                          # circles I belong to
curl -s -X DELETE $BASE/groups/$GID/members/$DEV -H "X-User-Id: $DEV"   # leave (204)
```

Membership rules: any member may invite; a member may leave; only the creator may remove other people.

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
| UI shows stale data after switching users | Reload the page; each identity's data is fetched fresh |

---

## 8. Project layout

```text
src/main/java/com/habittracker/
  habits/         habit definitions and schedules
  checkins/       check-in recording and history; access policy
  streaks/        streak and weekly-progress calculation (pure)
  groups/         circles, membership, shared/joint habits, group progress
  notifications/  preferences, weekly digest, delivery log
  common/         shared exceptions, error handling, clock and scheduling config
src/main/resources/
  db/migration/   Flyway migrations V1–V5 (never edit an applied one)
  static/         web UI
```

Each module follows `api` → `application` → `domain` ← `infrastructure`: domain code is plain Java, and persistence lives behind repository interfaces.

### Current limitations

- No authentication: `X-User-Id` is trusted as-is.
- `PATCH /habits/{id}/archive` has no permission check yet.
- Digest delivery only writes to the log (there are no email addresses yet).
- Not built from the design mock: the live wall chat, nudges, and habit notes.
