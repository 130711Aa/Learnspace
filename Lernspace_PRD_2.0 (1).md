# Lernspace --- Product Requirements Document (PRD) 2.0

**Product:** Lernspace\
**Tagline:** Your Personal Learning Operating System\
**Version:** 2.0\
**Status:** Product definition / implementation-ready draft\
**Target platform:** Android tablet first\
**Primary test device:** Motorola Moto Pad 60 Neo\
**Primary input:** Active stylus and touch\
**Proposed stack:** Kotlin, Jetpack Compose, Room, DataStore,
WorkManager, Android Notifications\
**Data strategy:** Local-first

------------------------------------------------------------------------

## 1. Executive Summary

Lernspace is an Android application that helps one person manage the
complete learning process in one connected workspace.

It is not merely a note-taking application, calendar, or task manager.
It combines:

1.  **Plan** --- manage tasks, deadlines, study sessions, calendar
    events, and reminders.
2.  **Learn** --- organize subjects, topics, goals, and learning
    activities.
3.  **Organize** --- store notes, PDFs, annotations, and visual learning
    materials.
4.  **Review** --- revisit material, track understanding, and eventually
    use flashcards and spaced repetition.

The product's daily value comes from helping the user decide what to do
next, remember important commitments, and keep learning materials
connected to the work being done.

### Product decisions

  -----------------------------------------------------------------------
  Decision                            Locked direction
  ----------------------------------- -----------------------------------
  Product vision                      All-in-one Learning Operating
                                      System

  Scheduling                          Hybrid: user sets goals and
                                      constraints; app assists with
                                      scheduling

  Dashboard                           Action-oriented

  Data                                Local-first; core functions work
                                      offline

  Platform                            Android native

  Language/UI                         Kotlin + Jetpack Compose

  Local database                      Room

  First device                        Motorola Moto Pad 60 Neo

  Development editor                  VS Code, with Android SDK/Gradle
                                      toolchain
  -----------------------------------------------------------------------

## 2. Product Vision and Principles

### Vision statement

**Lernspace helps learners plan, execute, organize, and evaluate their
learning without having to maintain several disconnected tools.**

### Product promise

When a user opens Lernspace, they should be able to answer:

-   What should I do next?
-   What is due soon?
-   What learning goal am I working toward?
-   Where are the notes and materials for this topic?
-   What should I review again?

### Product principles

1.  **Action over administration.** The user should spend more time
    learning than managing the app.
2.  **Connected by context.** Tasks, study sessions, notes, and
    documents can relate to a subject or topic.
3.  **Local-first.** Core workflows must work without an account or
    network connection.
4.  **User remains in control.** Automatic scheduling proposes changes;
    it does not silently rearrange commitments.
5.  **Reliable reminders.** Reminder state must be persisted and
    recoverable.
6.  **Stylus-first workspace.** Handwriting quality is a core product
    capability, even though the complete handwriting engine is not part
    of the first planner MVP.
7.  **Progress should be meaningful.** Time spent is useful context, but
    it must not be presented as proof of mastery.

## 3. Target User and Primary Use Cases

### Primary user

A student who manages multiple subjects, assignments, deadlines, study
sessions, and learning materials, and who prefers handwritten notes
using an active stylus.

### Primary use cases

-   Record a new assignment and its deadline quickly.
-   See today's tasks and the next scheduled study session.
-   View deadlines and events in a calendar.
-   Receive manually configured and automatic reminders.
-   Organize subjects into topics.
-   Schedule study sessions around existing commitments.
-   Open notes or documents associated with a topic.
-   Track whether a task is not started, in progress, or completed.
-   Review basic study activity without turning the app into a
    burdensome reporting tool.

## 4. Scope and Release Strategy

### MVP 1.0 --- Personal Learning Manager

The MVP must already be useful every day, even before advanced
handwriting and review features are complete.

**Included:**

-   Action-oriented home dashboard.
-   Subject and topic management.
-   Task and assignment management.
-   Calendar views.
-   Manual reminders.
-   Automatic deadline reminders.
-   Basic hybrid study scheduling.
-   Basic text notes linked to subjects/topics.
-   Local persistence and offline operation.
-   Basic learning activity summary.

**Explicitly excluded from MVP 1.0:**

-   Full handwriting engine.
-   Advanced PDF annotation.
-   Infinite canvas.
-   Flashcards and spaced repetition.
-   AI-generated study plans.
-   Cloud synchronization and user accounts.
-   Collaboration and sharing.
-   Desktop companion.

These are planned capabilities, not MVP requirements.

### Suggested release phases

  -----------------------------------------------------------------------
  Phase                               Scope
  ----------------------------------- -----------------------------------
  Foundation                          Navigation, design system, Room
                                      database, settings

  Planner core                        Tasks, calendar, deadlines,
                                      subjects, topics

  Reminder engine                     Manual/automatic reminders,
                                      rescheduling, recovery

  Hybrid scheduling                   User constraints, suggested study
                                      blocks, confirmation

  Basic workspace                     Text notes and document references

  Handwriting                         Ink Lab, stylus input, pen tools,
                                      editing

  Documents                           PDF rendering and annotation

  Review                              Flashcards, spaced repetition,
                                      mastery tracking

  Intelligence                        Optional recommendations and
                                      learning analytics
  -----------------------------------------------------------------------

## 5. Information Architecture

### Primary navigation

Recommended top-level navigation:

1.  **Home**
2.  **Planner**
3.  **Learning**
4.  **Workspace**
5.  **Insights**

A global quick-add action should be available from the main app shell.

### Module responsibilities

#### 5.1 Home --- Command Center

Purpose: answer "What should I do now?"

MVP content:

-   Today's date.
-   Next upcoming event or study session.
-   Tasks due today.
-   Overdue tasks.
-   Upcoming deadlines.
-   Quick-add task.
-   Quick-start study session.
-   Small daily progress summary.

Home must prioritize actionable items. Large charts and decorative
statistics must not push today's work below the fold.

#### 5.2 Planner --- Tasks, Calendar, and Reminders

Purpose: manage commitments and time.

MVP capabilities:

-   Task list with filters.
-   Day, week, and month calendar views.
-   Deadline display.
-   Calendar events.
-   Study session blocks.
-   Manual reminders.
-   Automatic reminder rules.
-   Snooze and dismiss.
-   Reschedule.
-   Recurring events/tasks where supported.

Tasks and calendar events are distinct concepts:

-   A **Task** is work to complete and has a completion status.
-   A **CalendarEvent** is something occupying a time interval.
-   A **StudySession** is a planned or completed learning activity.
-   A task may have a deadline without occupying a calendar time.
-   A task may optionally be assigned a study session.

#### 5.3 Learning --- Subjects and Topics

Purpose: organize learning content.

MVP capabilities:

-   Create, edit, archive, and reorder subjects.
-   Create topics within a subject.
-   Set optional target dates and learning goals.
-   View tasks and notes associated with a topic.
-   Set a simple self-reported topic status.

Suggested topic status values:

-   `NOT_STARTED`
-   `LEARNING`
-   `NEEDS_REVIEW`
-   `COMFORTABLE`

These are user judgments, not automatically inferred mastery scores.

#### 5.4 Workspace --- Notes and Documents

Purpose: keep learning materials near the relevant learning context.

MVP capabilities:

-   Basic text notes.
-   Link notes to a subject and optionally a topic.
-   Store document metadata and local file references.
-   Open a note from its subject/topic context.

Later capabilities:

-   Handwritten notebooks.
-   PDF reader and annotation.
-   Infinite canvas.
-   LaTeX editor.
-   Image and diagram tools.
-   Cross-reference to exact PDF pages and annotation locations.

#### 5.5 Insights

Purpose: show useful summaries without creating pressure to optimize
every minute.

MVP capabilities:

-   Tasks completed in a selected period.
-   Study sessions completed.
-   Study time by subject, when session durations are available.
-   Upcoming workload summary.

Do not equate hours studied with mastery.

## 6. Functional Requirements

Priority definitions:

-   **P0:** Required for MVP launch.
-   **P1:** Important, but can follow the first usable release.
-   **P2:** Future enhancement.

### 6.1 Home dashboard

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  HOME-01           P0                Show today's date User sees today's
                                      and agenda        relevant items on
                                                        opening Home

  HOME-02           P0                Show tasks due    Incomplete tasks
                                      today             due today are
                                                        visible

  HOME-03           P0                Show overdue      Overdue
                                      tasks             incomplete tasks
                                                        are clearly
                                                        identified

  HOME-04           P0                Show next         The nearest
                                      upcoming study    future scheduled
                                      session/event     item is shown

  HOME-05           P0                Quick-add task    User can create a
                                                        task without
                                                        navigating
                                                        through multiple
                                                        screens

  HOME-06           P0                Quick-start study User can start an
                                      session           unscheduled study
                                                        session

  HOME-07           P1                Daily completion  Completed and
                                      summary           remaining tasks
                                                        are summarized

  HOME-08           P1                Customizable      User can
                                      dashboard         hide/reorder
                                      sections          selected sections
  -----------------------------------------------------------------------

### 6.2 Subjects and topics

  ---------------------------------------------------------------------------
  ID                Priority          Requirement           Acceptance
                                                            summary
  ----------------- ----------------- --------------------- -----------------
  LEARN-01          P0                Create/edit/archive   Subject persists
                                      subject               locally

  LEARN-02          P0                Create/edit/archive   Topic belongs to
                                      topic                 a subject

  LEARN-03          P0                Optional subject      Visual
                                      color/icon            identification is
                                                            available

  LEARN-04          P0                Link task to          Task can be
                                      subject/topic         filtered by
                                                            learning context

  LEARN-05          P0                Link note to          Note appears in
                                      subject/topic         the relevant
                                                            context

  LEARN-06          P1                Topic learning goal   User can define a
                                                            goal and optional
                                                            target date

  LEARN-07          P1                Topic status          User can update
                                                            their
                                                            self-assessed
                                                            status
  ---------------------------------------------------------------------------

### 6.3 Tasks and assignments

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  TASK-01           P0                Create task       Title is
                                                        required; other
                                                        fields are
                                                        optional

  TASK-02           P0                Edit and delete   Changes persist
                                      task              locally

  TASK-03           P0                Task status       Support `TODO`,
                                                        `IN_PROGRESS`,
                                                        `COMPLETED`,
                                                        `CANCELLED`

  TASK-04           P0                Due date/time     Deadline can be
                                                        date-only or
                                                        date-time

  TASK-05           P0                Priority          Support `LOW`,
                                                        `MEDIUM`, `HIGH`

  TASK-06           P0                Optional          Task can be
                                      subject/topic     connected to
                                      link              learning context

  TASK-07           P0                Task filters      Filter by status,
                                                        date, priority,
                                                        subject

  TASK-08           P0                Overdue           Incomplete tasks
                                      calculation       past their
                                                        deadline are
                                                        overdue

  TASK-09           P1                Subtasks          A task can
                                                        contain smaller
                                                        checklist items

  TASK-10           P1                Recurring tasks   Repeating tasks
                                                        can be generated
                                                        from a recurrence
                                                        rule

  TASK-11           P1                Estimated         User can set
                                      duration          expected work
                                                        duration

  TASK-12           P1                Attachments       Task can link to
                                                        a document or
                                                        note
  -----------------------------------------------------------------------

### 6.4 Calendar and events

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  CAL-01            P0                Month view        Events, sessions,
                                                        and deadline
                                                        markers are
                                                        visible

  CAL-02            P0                Week view         Time-based
                                                        schedule is
                                                        visible

  CAL-03            P0                Day agenda        Items are ordered
                                                        by time

  CAL-04            P0                Create calendar   Start and end
                                      event             time are
                                                        supported

  CAL-05            P0                Display task      Date-only
                                      deadlines         deadlines appear
                                                        as all-day
                                                        markers

  CAL-06            P0                Display study     Planned sessions
                                      sessions          appear in the
                                                        calendar

  CAL-07            P0                Open item from    Selecting an item
                                      calendar          opens its detail

  CAL-08            P1                Drag to           User can move
                                      reschedule        eligible time
                                                        blocks

  CAL-09            P1                Recurring events  Repetition rules
                                                        are supported

  CAL-10            P1                Conflict          Overlapping time
                                      indication        blocks are
                                                        identified
  -----------------------------------------------------------------------

### 6.5 Hybrid study scheduling

Hybrid scheduling means the user controls the goal and constraints,
while Lernspace assists in finding available time.

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  SCHED-01          P0                Create study      User can plan a
                                      goal/session      session for a
                                      manually          subject/topic

  SCHED-02          P0                Set preferred     User can select a
                                      duration          session duration

  SCHED-03          P0                Set availability  User can define
                                                        available
                                                        days/time windows

  SCHED-04          P0                Suggest available App proposes
                                      slots             slots that do not
                                                        overlap protected
                                                        events

  SCHED-05          P0                User confirmation Suggested
                                                        sessions are not
                                                        saved until
                                                        confirmed

  SCHED-06          P0                Reschedule        User can request
                                      suggestion        another suggested
                                                        slot

  SCHED-07          P1                Split large study A long duration
                                      goals             can be divided
                                                        into smaller
                                                        sessions

  SCHED-08          P1                Workload          Suggestions
                                      balancing         consider existing
                                                        planned study
                                                        load

  SCHED-09          P2                Adaptive          Suggestions can
                                      suggestions       consider past
                                                        completion
                                                        patterns
  -----------------------------------------------------------------------

**MVP scheduling constraint:** use a transparent, deterministic
rule-based scheduler. Do not require AI.

Suggested first algorithm:

1.  Read the user's requested subject/topic and target duration.
2.  Read the requested completion window.
3.  Read availability windows.
4.  Exclude occupied calendar events and protected blocks.
5.  Exclude past times and invalid durations.
6.  Generate candidate slots.
7.  Rank candidates by user preference and proximity to the target date.
8.  Show suggestions with a short explanation.
9.  Save only after user confirmation.

The scheduler must never silently move existing events or create
sessions without user approval.

### 6.6 Reminder and notification engine

  ---------------------------------------------------------------------------
  ID                Priority          Requirement           Acceptance
                                                            summary
  ----------------- ----------------- --------------------- -----------------
  REM-01            P0                Create manual         User chooses
                                      reminder              date/time and
                                                            linked item

  REM-02            P0                Automatic deadline    User can enable
                                      reminder              default deadline
                                                            reminder rules

  REM-03            P0                Configure reminder    Examples: one day
                                      lead time             before or one
                                                            hour before

  REM-04            P0                Snooze                Notification can
                                                            be postponed
                                                            using supported
                                                            choices

  REM-05            P0                Dismiss               User can dismiss
                                                            a reminder

  REM-06            P0                Reschedule after item Pending reminder
                                      edit                  is recalculated

  REM-07            P0                Cancel after          Obsolete reminder
                                      completion/deletion   does not fire

  REM-08            P0                Offline operation     Scheduled local
                                                            reminders do not
                                                            require internet

  REM-09            P0                Restore schedules     Pending reminders
                                      after reboot          are reconstructed
                                                            from persisted
                                                            data

  REM-10            P1                Quiet hours           User can define
                                                            notification
                                                            quiet hours

  REM-11            P1                Notification grouping Related
                                                            notifications are
                                                            grouped

  REM-12            P1                Reminder history      User can inspect
                                                            recent reminder
                                                            activity
  ---------------------------------------------------------------------------

Reminder types:

-   `MANUAL`: explicitly created by the user.
-   `AUTOMATIC`: generated from a rule, such as a deadline lead time.
-   `SCHEDULE`: generated for a confirmed study session.

The app must distinguish the reminder's source and linked item.

**Android implementation considerations:**

-   Persist reminder definitions in Room; do not rely on an in-memory
    timer.
-   Use WorkManager for deferrable background work.
-   Use AlarmManager only where a user-visible reminder needs more
    precise timing and platform policy permits it.
-   Handle Android notification permission where required.
-   Handle exact-alarm access restrictions on applicable Android
    versions.
-   Reconcile and reschedule pending alarms after reboot, app update,
    time-zone change, or relevant item edits.
-   Provide a clear fallback if exact alarm access is unavailable.
-   Use stable notification IDs so reminders can be updated or
    cancelled.
-   Do not promise exact delivery if Android power-management or
    permission restrictions prevent it.

### 6.7 Basic notes and documents

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  NOTE-01           P0                Create/edit text  Note is stored
                                      note              locally

  NOTE-02           P0                Optional          Note appears in
                                      subject/topic     learning context
                                      link              

  NOTE-03           P0                Autosave          Recent edits are
                                                        persisted without
                                                        an explicit save
                                                        button

  NOTE-04           P0                Search note       Search returns
                                      titles/content    matching local
                                                        notes

  DOC-01            P1                Import document   User can add a
                                      metadata          document to a
                                                        subject/topic

  DOC-02            P1                Open local        App can open a
                                      document          valid stored
                                                        document
                                                        reference

  DOC-03            P1                Document          User can repair a
                                      relinking         missing file
                                                        reference
  -----------------------------------------------------------------------

Advanced handwritten notes and PDF annotation are not required to ship
the planner MVP, but the data architecture should not prevent their
later addition.

### 6.8 Insights

  -----------------------------------------------------------------------
  ID                Priority          Requirement       Acceptance
                                                        summary
  ----------------- ----------------- ----------------- -----------------
  INS-01            P1                Task completion   Completed/total
                                      summary           tasks for a
                                                        period

  INS-02            P1                Study session     Completed
                                      summary           sessions and
                                                        duration

  INS-03            P1                Subject breakdown Study activity
                                                        grouped by
                                                        subject

  INS-04            P2                Topic mastery     Display
                                      trends            user-entered
                                                        progress over
                                                        time
  -----------------------------------------------------------------------

## 7. Core User Flows

### Flow A --- First launch and setup

1.  User opens Lernspace.
2.  User chooses basic preferences such as week start day and default
    reminder behavior.
3.  User creates one or more subjects.
4.  User optionally adds topics to a subject.
5.  User defines typical availability windows for study scheduling.
6.  User lands on Home.
7.  App explains notification permission when the user first enables
    reminders that need it.

**Success condition:** user can create their first task and see it on
Home without completing a long onboarding form.

### Flow B --- Quick-add assignment

1.  User taps Quick Add.
2.  User selects Task.
3.  User enters a title.
4.  User optionally selects subject and topic.
5.  User sets due date, optional due time, priority, and estimated
    duration.
6.  User optionally enables automatic reminders or adds a manual
    reminder.
7.  User saves.
8.  Task appears in Planner and relevant Home sections.
9.  Reminder engine schedules eligible reminders.

**Success condition:** a basic task can be captured quickly, with only a
title required.

### Flow C --- Review calendar and reschedule

1.  User opens Planner.
2.  User selects week or month view.
3.  User sees events, study sessions, and deadline markers.
4.  User selects an item.
5.  User opens its detail.
6.  User changes the date/time or requests a new suggested slot.
7.  App checks conflicts.
8.  User confirms the change.
9.  Calendar and reminders update.

**Success condition:** changing an item's time updates its visible
schedule and associated reminders consistently.

### Flow D --- Create a hybrid study plan

1.  User opens Planner and selects Plan Study.
2.  User selects a subject and optional topic.
3.  User enters a target duration or number of sessions.
4.  User selects a target date/window.
5.  User selects preferred study days and availability.
6.  Scheduler reads existing events and protected time blocks.
7.  App displays candidate slots and explains why they were suggested.
8.  User accepts, edits, or rejects suggestions.
9.  Confirmed sessions are saved and shown in the calendar.
10. Session reminders are scheduled if enabled.

**Success condition:** no session is created or existing commitment
moved without user confirmation.

### Flow E --- Complete a study session

1.  User starts a scheduled session from Home or Planner.
2.  App shows the subject/topic and optional session goal.
3.  User starts the session timer or records start time.
4.  User studies using their materials.
5.  User ends the session.
6.  App records actual start/end and optional reflection.
7.  User may update topic status.
8.  Session appears in learning history and Insights.

**Success condition:** a completed session is recorded even if the user
does not fill in optional reflection fields.

### Flow F --- Reminder interaction

1.  A scheduled reminder becomes due.
2.  Android displays the notification if permission and system
    conditions allow it.
3.  User opens the notification, snoozes it, or dismisses it.
4.  Opening navigates to the linked task/event/session.
5.  Snooze creates a new scheduled trigger and updates persisted
    reminder state.
6.  Completing or deleting the linked item cancels obsolete triggers.

**Success condition:** reminder actions are reflected consistently in
Room and the Android notification system.

### Flow G --- Open learning material

1.  User opens Learning.
2.  User selects a subject.
3.  User selects a topic.
4.  User sees related tasks, notes, sessions, and documents.
5.  User opens or creates a note.
6.  The note is automatically associated with the selected context
    unless the user changes it.

**Success condition:** users can find relevant learning material from
the topic, not only from a global file list.

## 8. Database Design --- Room

### 8.1 Database principles

-   Use Room as the local source of truth.
-   Use stable UUID strings for entity identifiers.
-   Store timestamps as epoch milliseconds in UTC.
-   Store date-only deadlines as ISO `LocalDate` strings (`YYYY-MM-DD`)
    to avoid accidental time-zone shifts.
-   Store time-zone IDs where a scheduled local time must retain its
    intended zone.
-   Use explicit enum converters or stable string values.
-   Add indexes for common filters and foreign keys.
-   Use foreign-key constraints and deliberate delete behavior.
-   Prefer archive/soft-delete for user-created learning content where
    recovery is valuable.
-   Keep file bytes outside Room; store metadata and app-managed file
    references in Room.
-   Store reminder definitions separately from scheduled Android alarm
    identifiers.
-   Store scheduler-generated sessions with provenance so they can be
    distinguished from manually created sessions.

### 8.2 MVP entities

#### SubjectEntity

  Field         Type      Notes
  ------------- --------- --------------------------
  id            String    UUID primary key
  name          String    Required
  description   String?   Optional
  colorArgb     Long?     Optional display color
  iconKey       String?   Optional icon identifier
  sortOrder     Int       Display ordering
  isArchived    Boolean   Soft archive
  createdAt     Long      Epoch milliseconds
  updatedAt     Long      Epoch milliseconds

#### TopicEntity

  -----------------------------------------------------------------------
  Field                   Type                    Notes
  ----------------------- ----------------------- -----------------------
  id                      String                  UUID primary key

  subjectId               String                  FK to Subject

  name                    String                  Required

  description             String?                 Optional

  status                  String                  NOT_STARTED / LEARNING
                                                  / NEEDS_REVIEW /
                                                  COMFORTABLE

  targetDate              String?                 ISO LocalDate

  sortOrder               Int                     Display ordering

  isArchived              Boolean                 Soft archive

  createdAt               Long                    Epoch milliseconds

  updatedAt               Long                    Epoch milliseconds
  -----------------------------------------------------------------------

#### TaskEntity

  Field              Type      Notes
  ------------------ --------- --------------------------------------------
  id                 String    UUID primary key
  title              String    Required
  description        String?   Optional
  subjectId          String?   Optional FK
  topicId            String?   Optional FK
  status             String    TODO / IN_PROGRESS / COMPLETED / CANCELLED
  priority           String    LOW / MEDIUM / HIGH
  dueDate            String?   ISO LocalDate
  dueAt              Long?     Optional exact instant
  estimatedMinutes   Int?      Optional
  completedAt        Long?     Set when completed
  createdAt          Long      Epoch milliseconds
  updatedAt          Long      Epoch milliseconds
  archivedAt         Long?     Optional soft delete

#### CalendarEventEntity

  Field              Type      Notes
  ------------------ --------- ---------------------------
  id                 String    UUID primary key
  title              String    Required
  description        String?   Optional
  startAt            Long      Epoch milliseconds
  endAt              Long      Epoch milliseconds
  timeZoneId         String    Intended local zone
  isAllDay           Boolean   All-day event flag
  subjectId          String?   Optional FK
  topicId            String?   Optional FK
  location           String?   Optional
  recurrenceRuleId   String?   Future recurrence support
  createdAt          Long      Epoch milliseconds
  updatedAt          Long      Epoch milliseconds

#### StudySessionEntity

  Field            Type      Notes
  ---------------- --------- ---------------------------------------------
  id               String    UUID primary key
  title            String    Required
  subjectId        String?   Optional FK
  topicId          String?   Optional FK
  taskId           String?   Optional related task
  plannedStartAt   Long?     Optional planned start
  plannedEndAt     Long?     Optional planned end
  actualStartAt    Long?     Actual start
  actualEndAt      Long?     Actual end
  status           String    PLANNED / IN_PROGRESS / COMPLETED / SKIPPED
  origin           String    MANUAL / SCHEDULER
  notes            String?   Optional reflection
  createdAt        Long      Epoch milliseconds
  updatedAt        Long      Epoch milliseconds

#### ReminderEntity

  Field            Type      Notes
  ---------------- --------- --------------------------------------------------
  id               String    UUID primary key
  title            String    Required display title
  type             String    MANUAL / AUTOMATIC / SCHEDULE
  targetType       String?   TASK / EVENT / STUDY_SESSION / NONE
  targetId         String?   ID of linked item
  triggerAt        Long      Epoch milliseconds
  timeZoneId       String    Relevant zone
  status           String    ACTIVE / FIRED / SNOOZED / DISMISSED / CANCELLED
  snoozedUntil     Long?     Optional new trigger
  ruleKey          String?   Identifies generated rule
  notificationId   Int       Stable Android notification ID
  createdAt        Long      Epoch milliseconds
  updatedAt        Long      Epoch milliseconds

#### AvailabilityWindowEntity

  Field         Type      Notes
  ------------- --------- -----------------------------------
  id            String    UUID primary key
  dayOfWeek     Int       1--7 using ISO weekday convention
  startMinute   Int       Minutes after midnight
  endMinute     Int       Minutes after midnight
  timeZoneId    String    Local time zone
  isEnabled     Boolean   Whether available
  createdAt     Long      Epoch milliseconds
  updatedAt     Long      Epoch milliseconds

#### NoteEntity

  Field        Type      Notes
  ------------ --------- ----------------------
  id           String    UUID primary key
  title        String    Required
  content      String    MVP text content
  subjectId    String?   Optional FK
  topicId      String?   Optional FK
  createdAt    Long      Epoch milliseconds
  updatedAt    Long      Epoch milliseconds
  archivedAt   Long?     Optional soft delete

#### UserSettingsEntity

  Field                        Type      Notes
  ---------------------------- --------- ---------------------------
  id                           Int       Fixed primary key, e.g. 1
  weekStartsOn                 Int       ISO weekday
  defaultReminderLeadMinutes   Int       User default
  notificationsEnabled         Boolean   App-level preference
  quietHoursStartMinute        Int?      Future support
  quietHoursEndMinute          Int?      Future support
  timeZoneId                   String    Current preferred zone
  updatedAt                    Long      Epoch milliseconds

### 8.3 Relationships

``` text
Subject 1 ─── N Topic
Subject 1 ─── N Task
Topic   1 ─── N Task

Subject 1 ─── N CalendarEvent
Topic   1 ─── N CalendarEvent

Subject 1 ─── N StudySession
Topic   1 ─── N StudySession
Task    1 ─── N StudySession (optional association)

Task          1 ─── N Reminder
CalendarEvent 1 ─── N Reminder
StudySession  1 ─── N Reminder

Subject 1 ─── N Note
Topic   1 ─── N Note
```

A reminder may also be standalone (`targetType = NONE`,
`targetId = null`).

### 8.4 Room implementation notes

-   Use `@Entity`, `@PrimaryKey`, `@ForeignKey`, and `@Index`.
-   Add indexes for `subjectId`, `topicId`, `status`, `dueDate`,
    `dueAt`, `startAt`, `triggerAt`, and `targetId` as appropriate.
-   Use `@Transaction` for operations that update an item and its
    related reminders.
-   Use DAO `Flow` queries for reactive UI updates.
-   Use repository interfaces to keep ViewModels independent from Room
    implementation.
-   Use explicit database migrations; do not use destructive migrations
    for real user data.
-   Add a schema export and keep versioned schema files in source
    control.
-   Use `RoomDatabase.Callback` only for safe initial setup, not for
    long-running work.
-   Consider a separate file-management layer for PDFs and images.

### 8.5 Example Room entity (Kotlin)

``` kotlin
@Entity(
    tableName = "tasks",
    indices = [
        Index("subjectId"),
        Index("topicId"),
        Index("status"),
        Index("dueDate"),
        Index("dueAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val subjectId: String?,
    val topicId: String?,
    val status: String,
    val priority: String,
    val dueDate: String?,
    val dueAt: Long?,
    val estimatedMinutes: Int?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?
)
```

**Important integrity rule:** if a topic is deleted or archived, the app
must not leave tasks pointing to an invalid topic. The application
should either prevent deletion while linked items exist, reassign them,
or use a deliberate `SET_NULL` strategy. Subject/topic archival is safer
than hard deletion for normal user actions.

### 8.6 Entities intentionally deferred

Do not create every future table in the first database migration.

Potential later entities:

-   `SubtaskEntity`
-   `RecurrenceRuleEntity`
-   `LearningGoalEntity`
-   `DocumentEntity`
-   `NotebookEntity`
-   `AnnotationEntity`
-   `CanvasEntity`
-   `FlashcardDeckEntity`
-   `FlashcardEntity`
-   `ReviewSessionEntity`
-   `ReviewRecordEntity`
-   `TopicProgressEntity`
-   `NotificationLogEntity`

Add them when the corresponding feature is scheduled for implementation.

## 9. Hybrid Scheduler --- MVP Rules

The MVP scheduler should be predictable and explainable.

### Inputs

-   Subject and optional topic.
-   Desired total study duration.
-   Target date or date range.
-   Preferred days.
-   Availability windows.
-   Preferred session length.
-   Existing calendar events.
-   Existing planned study sessions.
-   Optional buffer between sessions.

### Output

A list of suggested time slots with:

-   Start and end time.
-   Subject/topic.
-   Duration.
-   Reason for suggestion.
-   Conflict status.

### Scheduling rules

1.  Never schedule in the past.
2.  Never overlap protected calendar events.
3.  Never overlap another confirmed study session.
4.  Respect availability windows.
5.  Respect the target date/window.
6.  Prefer the user's chosen session duration.
7.  Split a long goal only if the user allows it.
8.  Do not modify existing commitments automatically.
9.  Require explicit confirmation before saving suggestions.
10. If no slot is available, explain why and ask the user to widen
    constraints.

### MVP limitation

The scheduler is an assistant, not an autonomous calendar manager. It
does not infer a perfect timetable or guarantee that the user will
follow it.

## 10. Reminder Engine --- State and Reliability

### Reminder lifecycle

``` text
ACTIVE
  ├── trigger reached ──> FIRED
  ├── user snoozes ─────> SNOOZED ──> ACTIVE
  ├── user dismisses ───> DISMISSED
  └── target completed ─> CANCELLED
```

Implementation must account for the fact that Android may delay or
restrict background work.

### Required reconciliation points

-   App launch.
-   Device reboot.
-   Time-zone change.
-   System time change.
-   Task/event/session update.
-   Task/event/session completion.
-   Reminder settings change.
-   Notification permission change where detectable.

The database is the source of truth. Android alarms and notifications
are scheduled projections of persisted reminder records.

## 11. Non-Functional Requirements

  -----------------------------------------------------------------------
  ID                      Category                Requirement
  ----------------------- ----------------------- -----------------------
  NFR-01                  Offline                 Core planner and
                                                  learning data remain
                                                  usable without internet

  NFR-02                  Data integrity          App must not silently
                                                  lose saved tasks,
                                                  notes, or reminders

  NFR-03                  Performance             Common list/calendar
                                                  interactions should
                                                  feel responsive on the
                                                  target tablet

  NFR-04                  Startup                 Home should show useful
                                                  cached local data
                                                  without waiting for
                                                  network

  NFR-05                  Accessibility           Text scaling, adequate
                                                  contrast, and touch
                                                  targets must be
                                                  considered

  NFR-06                  Tablet UX               Layout should use
                                                  available tablet space
                                                  rather than simply
                                                  stretch a phone layout

  NFR-07                  Reliability             Reminder definitions
                                                  persist across process
                                                  death and device reboot

  NFR-08                  Maintainability         UI, domain logic, and
                                                  data access must be
                                                  separated

  NFR-09                  Privacy                 No account or cloud
                                                  upload is required for
                                                  MVP

  NFR-10                  Backup                  A documented local
                                                  export/backup path
                                                  should be planned
                                                  before broad daily use

  NFR-11                  Time correctness        Date-only deadlines and
                                                  time-based events must
                                                  be represented
                                                  separately

  NFR-12                  Observability           Development builds
                                                  should log scheduling
                                                  and database failures
                                                  without exposing
                                                  private note content
  -----------------------------------------------------------------------

### Performance testing principles

Do not set arbitrary performance claims before measuring the app.
Establish baseline measurements on the Moto Pad 60 Neo.

Test: - Home with hundreds of tasks. - Calendar with dense weeks. -
Large subject/topic lists. - Repeated task edits and reminder
rescheduling. - App process death and restoration. - Device reboot and
reminder recovery. - Long notes and large document metadata collections.

## 12. UX and Interaction Requirements

### Action-oriented dashboard

Suggested Home hierarchy:

1.  **Next action:** nearest relevant task or study session.
2.  **Today:** scheduled sessions and events.
3.  **Due soon:** overdue and upcoming tasks.
4.  **Quick actions:** add task, plan study, add note.
5.  **Progress:** compact daily summary.

The dashboard should avoid overwhelming the user with every metric.

### Quick capture

A task must be creatable with only a title. Subject, topic, deadline,
priority, and reminder are optional fields.

### Calendar visual language

Use consistent visual distinctions for:

-   Calendar events.
-   Study sessions.
-   Task deadlines.
-   Overdue tasks.

Do not rely on color alone; include icons, labels, or accessible text.

### Tablet and stylus

-   Support comfortable touch targets.
-   Keep frequent actions accessible.
-   Avoid placing essential controls only in tiny menus.
-   Reserve the dedicated handwriting interaction model for the
    Workspace/Ink Engine phase.
-   Do not let planner gestures conflict with stylus writing gestures in
    future workspace screens.

## 13. MVP Acceptance Criteria

The MVP is considered ready for personal daily use when all P0 criteria
pass.

### A. Foundation and data

-   [ ] App installs and launches on the target Android tablet.
-   [ ] App works with airplane mode enabled.
-   [ ] Subjects, topics, tasks, events, sessions, reminders, and notes
    persist after app restart.
-   [ ] Database migrations preserve existing test data.
-   [ ] Invalid foreign-key references cannot be created through normal
    app flows.

### B. Subjects and topics

-   [ ] User can create, edit, archive, and reorder subjects.
-   [ ] User can create and edit topics under a subject.
-   [ ] User can open a subject and see its topics and linked
    tasks/notes.
-   [ ] Archived subjects/topics do not appear as active choices for new
    items.

### C. Tasks

-   [ ] User can create a task using only a title.
-   [ ] User can add a deadline, priority, subject, and topic.
-   [ ] User can change task status.
-   [ ] Completed tasks are not shown as overdue.
-   [ ] Incomplete tasks past their deadline are identified as overdue.
-   [ ] Editing or completing a task updates related reminders.

### D. Calendar

-   [ ] Month, week, and day views show the correct local dates.
-   [ ] Date-only deadlines appear as all-day items.
-   [ ] Time-based events appear at the correct local time.
-   [ ] Selecting a calendar item opens its detail.
-   [ ] Rescheduling an item updates the calendar and associated
    reminder schedule.

### E. Hybrid scheduling

-   [ ] User can define availability windows.
-   [ ] User can request study slots for a subject/topic and duration.
-   [ ] Suggestions do not overlap protected events or confirmed
    sessions.
-   [ ] Suggestions respect the requested date window.
-   [ ] User can accept or reject each suggestion.
-   [ ] No suggested session is saved without confirmation.
-   [ ] Existing events are never silently moved.

### F. Reminders

-   [ ] User can create a manual reminder.
-   [ ] User can enable automatic deadline reminders.
-   [ ] User can configure reminder lead time.
-   [ ] User can snooze and dismiss a reminder.
-   [ ] Completing or deleting a linked item cancels its obsolete
    reminders.
-   [ ] Editing a deadline reschedules its automatic reminders.
-   [ ] Pending reminders are reconstructed after app process death.
-   [ ] Pending reminders are reconciled after device reboot.
-   [ ] App handles notification permission denial gracefully.
-   [ ] App explains when exact-time delivery cannot be guaranteed by
    the system.

### G. Home dashboard

-   [ ] Home shows today's date and relevant agenda.
-   [ ] Home shows tasks due today.
-   [ ] Home identifies overdue tasks.
-   [ ] Home shows the next upcoming scheduled item.
-   [ ] Quick-add task is accessible.
-   [ ] Empty states provide a clear next action.

### H. Basic workspace

-   [ ] User can create and edit a text note.
-   [ ] Note autosaves.
-   [ ] Note can be linked to a subject/topic.
-   [ ] Note can be found from its learning context.
-   [ ] Note content survives app restart.

### I. Usability and performance

-   [ ] Main navigation is understandable without a tutorial.
-   [ ] A basic task can be captured with minimal required input.
-   [ ] Common interactions are responsive on the target tablet.
-   [ ] No known data-loss bug remains in the core workflows.
-   [ ] The app remains useful without network access.

## 14. MVP Definition of Done

MVP 1.0 is not defined by the number of screens. It is defined by one
complete daily workflow:

1.  User opens Lernspace.
2.  User sees what needs attention today.
3.  User creates an assignment with a deadline.
4.  User sees it on the calendar.
5.  User receives the configured reminder.
6.  User plans a study session around existing commitments.
7.  User studies and records the session.
8.  User opens or creates a note associated with the relevant topic.
9.  User can close the app and later find all information intact.

If this workflow is reliable, Lernspace has delivered its first real
value.

## 15. Technical Architecture Proposal

Suggested high-level package structure:

``` text
app/
  navigation/
  ui/
    home/
    planner/
    learning/
    workspace/
    insights/
    components/

core/
  common/
  database/
  notifications/
  scheduling/
  design/

data/
  local/
    dao/
    entity/
    converter/
  repository/

domain/
  model/
  repository/
  usecase/

feature/
  home/
  planner/
  learning/
  workspace/
  insights/
```

Suggested architectural boundaries:

-   **UI:** Compose screens and ViewModels.
-   **Domain:** use cases, business rules, scheduler logic, and domain
    models.
-   **Data:** Room entities, DAOs, repositories, file references.
-   **Core services:** reminder scheduling, notification handling, time
    utilities.

Use Kotlin Coroutines and Flow for asynchronous work and reactive
database observation.

Do not over-engineer dependency injection or module boundaries before
the first vertical slice works. Start with a maintainable single app
module if needed, then split Gradle modules when there is a clear
benefit.

## 16. Key Risks and Mitigations

  -----------------------------------------------------------------------
  Risk                    Impact                  Mitigation
  ----------------------- ----------------------- -----------------------
  Scope grows too quickly MVP never ships         Freeze P0 scope and
                                                  defer handwriting/AI

  Android reminder        Reminders may be        Persist definitions,
  restrictions            delayed                 use appropriate APIs,
                                                  explain permissions and
                                                  limitations

  Time-zone/date bugs     Wrong calendar or       Separate date-only
                          deadline display        values from instants;
                                                  test time-zone changes

  Scheduler complexity    Confusing or poor       Start with
                          suggestions             deterministic
                                                  rule-based slots

  Database relationship   Orphaned tasks/notes    Foreign keys, indexes,
  mistakes                                        migrations, soft
                                                  archive

  Local-only data loss    User loses learning     Plan export/backup
                          history                 before relying on app
                                                  long term

  Stylus engine consumes  Planner delayed         Build Ink Lab as a
  time                                            separate milestone
                                                  after planner
                                                  foundation
  -----------------------------------------------------------------------

## 17. Open Decisions

These should be decided during implementation, not guessed prematurely:

1.  Exact calendar week-start preference and locale handling.
2.  Default automatic reminder presets.
3.  Whether recurring tasks are required in the first public MVP or can
    wait for P1.
4.  Local backup/export format.
5.  PDF file ownership and Android Storage Access Framework strategy.
6.  Final handwriting rendering engine and data format.
7.  Whether a future desktop companion should share a database format or
    sync protocol.

## 18. Recommended First Implementation Sprint

The first sprint should produce a working vertical slice, not a large
collection of empty screens.

### Sprint 1

1.  Create Kotlin + Jetpack Compose project.
2.  Establish app navigation and tablet layout.
3.  Add Room database and migrations.
4.  Implement Subject and Topic entities/DAOs.
5.  Implement Task entity/DAO/repository.
6.  Build quick-add task flow.
7.  Show tasks on Home.
8.  Add task status and due-date handling.
9.  Add basic tests for overdue calculation and persistence.

### Sprint 2

1.  Calendar day/week/month foundations.
2.  CalendarEvent and StudySession.
3.  Availability windows.
4.  Rule-based scheduling suggestions.
5.  User confirmation flow.

### Sprint 3

1.  Reminder entity and repository.
2.  Notification channels and permission handling.
3.  Manual reminder scheduling.
4.  Automatic deadline reminder rules.
5.  Snooze, dismiss, cancellation, and rescheduling.
6.  Reboot/process-death recovery tests.

### Sprint 4

1.  Basic text notes.
2.  Subject/topic context screens.
3.  Home dashboard polish.
4.  Basic Insights.
5.  Offline and regression testing.

------------------------------------------------------------------------

## Final Product Definition

**Lernspace is a local-first, Android-native Personal Learning Operating
System.**

It helps learners:

-   **Plan** tasks, deadlines, calendars, and study sessions.
-   **Learn** through organized subjects, topics, and goals.
-   **Organize** notes, documents, and handwritten materials.
-   **Review** learning through progress tracking and future
    spaced-repetition tools.

Its first release focuses on a reliable daily planner, connected
learning structure, hybrid scheduling, and reminders. Its long-term
differentiation is the combination of learning management and a
high-quality stylus-first workspace.

**The MVP succeeds when the user can rely on Lernspace every day to know
what to do, remember when to do it, and keep the materials needed to do
it.**
