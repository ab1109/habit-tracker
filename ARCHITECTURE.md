# Habit Tracker Architecture

## Purpose

The habit tracker is a collaborative application for defining habits, recording check-ins, and showing progress to individuals and groups.

The system is a **modular monolith**: one deployable application with explicit internal module boundaries. We keep the modules separate in code so that domain rules remain understandable and can be changed without turning the application into a distributed system.

## Goals

- Let a user create and manage habits.
- Let a user record an idempotent check-in for a habit.
- Represent collaborative groups and their shared habit progress.
- Derive streaks and progress from stored history.
- Send notifications without putting notification concerns into habit or check-in logic.
- Keep every vertical slice runnable and testable locally.

## Non-goals

- Microservices.
- Mutable streak counters as the source of truth.
- Reconstructing a check-in's day from its UTC timestamp at read time.
- Editing an applied migration.

## Module Boundaries

### `habits`

Owns habit definitions and schedule configuration.

Responsibilities:

- Create, update, archive, and read habits.
- Store the schedule type and schedule parameters.
- Validate schedule configuration.
- Expose habit information needed by check-ins and progress calculations.

The habits module does not record check-ins or send notifications.

### `groups`

Owns collaborative groups, membership, and group-level views of progress.

Responsibilities:

- Create and manage groups.
- Add and remove members according to the authorization rules.
- Associate group members with shared habits where the product rules allow it.
- Request progress data from the relevant domain logic.

The groups module does not maintain a separate mutable progress counter. Group progress is calculated from check-in history and the applicable habit schedules.

### `checkins`

Owns check-in creation and check-in history.

A check-in stores:

- The habit and user it belongs to.
- The UTC timestamp at which it was recorded.
- The user's resolved local date for that check-in.

The local date is calculated when the check-in is created using the user's timezone and stored as a `DATE`. Reads must use this stored value; they must not recalculate the day from the UTC timestamp.

A unique constraint on `(habit_id, user_id, local_date)` makes check-ins idempotent. Repeating the same check-in for the same habit, user, and local date does not create a second record.

The check-ins module owns check-in history, but it should not store a mutable streak counter.

### `notifications`

Owns notification preferences, notification scheduling, and delivery attempts.

Responsibilities:

- Decide which notifications are enabled.
- Schedule reminders or progress notifications.
- Record delivery attempts and outcomes.
- Consume domain events or application-level notifications without changing the source-of-truth data in other modules.

Notification delivery must not be required for recording a check-in successfully.

## Dependency Direction

The application layer coordinates requests between modules. Domain rules stay in the module that owns them.

Preferred direction:

```text
HTTP/UI -> application services -> domain modules -> persistence
                                      |
                                      +-> domain events -> notifications
```

Rules:

- A module may call a public application or domain interface of another module, not its private implementation details.
- Persistence models should not be passed directly between modules.
- Cross-module workflows should be explicit in an application service.
- Notifications are downstream of successful domain actions and should not be part of the transaction that defines the action's source of truth unless required by the chosen infrastructure.

## Data Invariants

### Local dates and timezones

The timestamp and local date answer different questions:

- `recorded_at` answers: "When did the server record this event in UTC?"
- `local_date` answers: "Which calendar day did this count for the user?"

Example: a user in `America/Los_Angeles` checks in at `2026-09-07 00:30` local time. The UTC timestamp may still be on `2026-09-07`, but for a user in another timezone the same instant could belong to a different local date. The check-in's local date is resolved once, using the checking-in user's timezone, and then persisted.

The timezone used for local-date resolution must be part of the check-in command's trusted input or be loaded from the user's current profile according to the product decision. The implementation must make that choice explicit.

### Idempotent check-ins

At most one check-in exists for a habit, user, and local date. Application-level duplicate detection improves the error or response, but the database unique constraint is the final protection against races.

### Derived streaks

A streak is computed from check-in history and the habit's schedule. It is never maintained by incrementing or decrementing a stored counter.

The streak calculation must branch by schedule type:

- **Daily**: a required day is missed when there is no check-in for that local date.
- **N times per week**: progress is evaluated against the weekly target; a single empty day is not automatically equivalent to missing the whole week.
- **Specific weekday**: only the configured weekdays are required, so non-configured days do not break the streak.

The exact definition of an active streak, especially for an incomplete current period, belongs in the domain specification and must be covered by examples and tests.

### Group progress

Group progress is an aggregation over members, habits, schedules, and check-in history. It is derived data. Any cache must be replaceable by recomputing from the underlying records.

## Persistence and Migrations

- Tables and constraints are owned by the module that owns the corresponding data.
- Every schema change is introduced through a new migration.
- Once a migration has been applied, it is immutable.
- Constraints that express domain invariants, especially the check-in uniqueness constraint, belong in the database as well as in application validation.
- Indexes should be added to support demonstrated query patterns, such as looking up check-ins by habit, user, and local-date range. Each index should have a documented query it serves.

The concrete database and migration tool remain an explicit project decision until selected.

## Testing Strategy

Tests are written before or alongside each vertical slice.

### Domain tests

Mandatory domain coverage includes:

- Schedule validation.
- Daily streaks, including a missing day.
- N-times-per-week streaks, including a week that meets the target on non-consecutive days.
- Specific-weekday streaks, including non-required days.
- Local-date resolution around a timezone boundary.
- Idempotent check-in creation.
- Group progress aggregation.

Tests should use small, hand-checkable examples and include edge cases such as an empty history, duplicate submissions, and the current incomplete schedule period.

### Integration tests

Integration tests should verify:

- Database constraints reject duplicate check-ins under concurrent-looking requests.
- Migrations create the expected schema.
- Application services translate domain results into their public response shape.

### End-to-end tests

End-to-end coverage should be added for user-visible slices after the relevant domain and integration tests exist. It should remain focused on workflows rather than repeating every domain case.

## Vertical Slice Order

A reasonable learning-oriented order is:

1. Create a habit with a validated schedule.
2. Record an idempotent check-in with a persisted local date.
3. Calculate a streak from check-in history.
4. Create a group and show aggregated progress.
5. Add notification preferences and one notification workflow.

Each slice includes its schema change, migration, application boundary, tests, and minimal user interface needed to demonstrate the behavior. No later slice should hide an unresolved domain rule from an earlier one.

## Open Decisions

These choices should be made explicitly before the first implementation slice:

- Application language and web framework.
- Database engine and migration tool.
- Authentication and user identity model.
- Where user timezone is stored and how timezone changes affect future check-ins.
- Exact schedule representation for daily, weekly-target, and specific-weekday habits.
- Exact semantics of a current incomplete period in streak calculations.
- Whether domain events are persisted or are initially dispatched in-process.
- Initial notification delivery mechanism.

## Decisions Made

Resolved open decisions, recorded as each slice settled them.

- **Language, framework, database, migrations:** Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway.
- **Identity:** no authentication yet. The caller is identified by a trusted `X-User-Id` header.
- **Timezone for check-ins:** supplied per request in the `X-Timezone` header (the client's IANA zone) and resolved to `local_date` once, at check-in time. Reads that depend on "today" (progress, streaks) also take `X-Timezone`, so progress is always relative to the viewer's current local date.
- **Weeks:** ISO weeks, Monday to Sunday.
- **Streak units:** daily habits count consecutive days; specific-weekday habits count consecutive *scheduled* days (other days are skipped, and check-ins on them don't count); N-times-per-week habits count consecutive weeks with at least N distinct check-in days.
- **Current incomplete period:** if today (or this week, for weekly habits) is already satisfied it counts toward the streak; otherwise it is *open* and the streak runs through the previous period without being broken. Check-ins dated after the viewer's today are ignored.
- **Weekly progress:** a week's target is 7 for daily habits, N for N-times-per-week, and the number of chosen weekdays for specific-weekday habits; `done` is capped at the target.
- **Access:** only a personal habit's owner may check in to it or read its history and progress (extended to circles in slice 4).
