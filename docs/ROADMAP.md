# Braculink — Project Roadmap

A BRACU-only platform where students verify with `@g.bracu.ac.bd`, pick their course sections from live registration data, get an auto-built weekly routine, and get matched into same-course section-swap groups — smallest group first.

---

## 0. Good news on the data source

`https://usis-cdn.eniamza.com/connect.json` is a real, public, unauthenticated JSON array — confirmed by fetching it directly. This is almost certainly what the `prepre_reg` frontend tool itself calls under the hood, so the earlier concern about that repo being a scraper of unknown origin is resolved: you can hit this URL directly from your Spring Boot backend, no login, no cookies, no scraping. One courtesy to build in anyway: **poll it politely** (every 5–15 min is plenty, even during active registration) rather than hammering it every few seconds — it's a shared CDN serving the whole university, and hourly-fresh seat counts are more than good enough for a swap-coordination tool.

Each element in the array is one **section** row, shaped like your sample — a `THEORY` section can carry an *embedded* mandatory lab (`labSectionId`, `labCourseCode`, `labFaculties`, `labSchedules`, `labRoomName`, `labName` all non-null, like your `CSE370` → `CSE370L` example), or those lab fields are all `null` if the course has no lab. Separately, some elements are standalone `courseType: "LAB"` rows for courses that are lab-only in their own right — those are independent sections, not attached to anything.

---

## 1. Architecture

```
┌─────────────────────────────┐
│   Frontend (AI-generated)    │   React/Next.js
└───────────────┬───────────────┘
                │ HTTPS / JSON
┌───────────────▼───────────────┐
│   Spring Boot REST API         │
│  ┌───────────┐ ┌─────────────┐ │
│  │   Auth     │ │  Course     │ │  ← domain-gated signup + JWT
│  │ (Security) │ │  sync +     │ │  ← pulls connect.json on a timer
│  │            │ │  picker     │ │
│  └───────────┘ └─────────────┘ │
│  ┌───────────┐ ┌─────────────┐ │
│  │  Routine   │ │  Swap       │ │  ← auto-builds weekly grid
│  │  builder   │ │  Suggestion │ │  ← priority-ranked cycle search
│  │            │ │  Engine     │ │
│  └───────────┘ └─────────────┘ │
│  ┌───────────────────────────┐ │
│  │   Friends (gates routine    │ │  ← no chat — profile shows
│  │   + live status visibility) │ │     FB link / phone instead
│  └───────────────────────────┘ │
└───────────────┬───────────────┘
                │ JdbcTemplate (hand-written SQL)
┌───────────────▼───────────────┐
│   MySQL 8  (+ Redis cache)     │
└─────────────────────────────────┘
```

**Stack:** Spring Boot 3.x, Java 17+, Spring Security + JWT, **Spring JDBC (`JdbcTemplate`)**, **MySQL 8**, Spring Scheduler. No ORM — every query in this project is hand-written SQL. No chat/WebSocket layer needed for MVP — see §5.

**Why JdbcTemplate and not Hibernate:** this is a database course project, so the SQL has to be visible and yours. `JdbcTemplate` keeps 100% of the SQL hand-written while removing only the boilerplate you already know how to write by hand — getting a `Connection`, preparing the statement, closing everything in a `finally`. It is essentially the mature version of the `JdbcUtility` helper class you already built in your own JDBC practice apps. You still write every `SELECT`, `INSERT`, `UPDATE`, and `JOIN` yourself.

**How your existing JDBC knowledge maps over:**

| What you already do by hand | With `JdbcTemplate` |
|---|---|
| `JdbcUtility.getConnection()` | handled by the connection pool; you never call it |
| `connection.prepareStatement(sql)` | `jdbc.query(sql, ...)` / `jdbc.update(sql, ...)` |
| `ps.setString(1, x); ps.setInt(2, y);` | pass them as trailing varargs: `jdbc.update(sql, x, y)` |
| `while (rs.next()) { rs.getString("name") ... }` | a `RowMapper` — same `rs.getX()` calls, written once per table |
| `finally { closeAllConnection(rs, ps, con); }` | automatic — this is the boilerplate it removes |
| `ps.addBatch()` / `ps.executeBatch()` | `jdbc.batchUpdate(sql, List<Object[]>)` |

The SQL string itself is unchanged from what you'd write in plain JDBC — that is the whole point.

**JDBC/MySQL notes worth knowing up front:**
- **You hand-write the schema.** `src/main/resources/schema.sql` holds real `CREATE TABLE` statements with explicit `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`, and `NOT NULL` constraints — mapped directly from your relational-schema diagram. Nothing is auto-generated. Set `spring.sql.init.mode=always` so Spring Boot runs it at startup, and use `CREATE TABLE IF NOT EXISTS` so restarts are safe.
- **Auto-increment PKs:** `id BIGINT AUTO_INCREMENT PRIMARY KEY`, and use `ENGINE=InnoDB` so foreign keys are actually enforced (MyISAM silently ignores them).
- **Naming:** tables and columns in `snake_case` (`course_section`, `current_section_id`), Java fields in `camelCase` (`currentSectionId`). One-to-one correspondence with your ER/relational diagram — `courseCode` → `course_code`, `lastSyncedAt` → `last_synced_at`, and so on. No names change meaning, only case style, which is standard SQL practice.
- **The two JSON columns:** `class_schedules` and `lab_schedules` are MySQL 8 `JSON` columns. Without Hibernate there's no automatic mapping, so your `RowMapper` reads the column as a string and parses it with Jackson: `objectMapper.readValue(rs.getString("class_schedules"), new TypeReference<List<ClassSlot>>(){})`. Writing goes the other way with `writeValueAsString(...)`. That's about four lines in the mapper.
- **Worth asking your teacher:** a strict normalization reading would split those JSON columns into a separate `class_schedule(section_id, day, start_time, end_time)` table (this is Step 6, multivalued attributes, from your Chapter 7 slides). Your ER diagram currently models them as simple attributes, which is why they're JSON columns here. If your teacher wants strict normalization, that's a small, contained change — one extra table plus one extra DAO — and it does not touch any of the matching logic.
- **Transactions:** `@Transactional` works exactly the same with `JdbcTemplate` as with JPA. You need it in one important place — the swap-group propose operation (§6).

---

## 2. Core domain model

This is the final, deduplicated 6-entity model (matches the ER diagram), plus the `FRIENDSHIP` join table from the M:N mapping. Two entities from earlier drafts were folded in on purpose: **Course** merged into **CourseSection** (every `connect.json` row already carries `courseCode`/`courseName`, so a course was never more than a group of its own sections), and **SwapGroupMember** merged into **SwapRequest** (it was strictly 1:1, so its `confirmed`/group-link fields live directly on the request).

With JdbcTemplate each of these becomes **three small things** instead of one annotated entity class: a `CREATE TABLE` block in `schema.sql`, a plain Java POJO in `model/` (no annotations at all — just fields, getters, setters), and a `RowMapper` + DAO in `dao/` holding the hand-written SQL. The names below are unchanged from your ER diagram.

```
CourseSection                                    ← one row per element of connect.json;
 │                                                  also IS the "course" — no separate Course entity
 ├─ id (PK), sectionId (external), courseCode, courseName,
 │  courseType (THEORY | LAB), sectionName (e.g. "15", "15B"),
 │  faculties (theory initials), roomName,
 │  capacity, consumedSeat  →  availableSeats = capacity - consumedSeat,
 │  semesterSessionId,
 │  classSchedules (JSON → [{day, startTime, endTime}], + exam dates),
 │  labSectionId, labFaculties,
 │  labSchedules (JSON, nullable → same {day,startTime,endTime} shape),
 │  lastSyncedAt

User
 ├─ id (PK), studentId, fullName, bracuEmail (unique, @g.bracu.ac.bd only),
 │  passwordHash, phoneNumber (nullable), phonePublic (boolean, default false),
 │  fbProfileUrl (nullable, shown to anyone who can see the profile — see note below)

Enrollment                          ← "I am/will-be in this section", drives BOTH
 ├─ id (PK), semesterSessionId          the routine AND the swap request's "current section"
 │  (links User —enrolls→ Enrollment —held_in→ CourseSection; lab is never stored
 │   separately — it's resolved at read time from the theory section's embedded lab* fields)

SwapRequest
 ├─ id (PK), status[PENDING, RESERVED, MATCHED, CANCELLED, EXPIRED],
 │  confirmed (boolean), createdAt, respondedAt
 │  (relationships: User —requests→ it; it —sits_in→ CourseSection [current];
 │   it —wants_section→ CourseSection [desired]; SwapGroup —contains→ it.
 │   Both section links must resolve to the same courseCode — enforced at the
 │   service layer and structurally by the per-course graph, see §6)

SwapGroup
 ├─ id (PK), status[PROPOSED, CONFIRMED, COMPLETED, EXPIRED, CANCELLED], createdAt

Notification
 ├─ id (PK), type, payload, read, createdAt   (User —receives→ it)

Friendship  (M:N relationship on User, mapped by Ch.7 Step 5 into its own table)
 ├─ requesterId, addresseeId  — composite PRIMARY KEY, both FOREIGN KEY → user(id)
 ├─ status[PENDING, ACCEPTED, DECLINED], createdAt
 │  Direction is carried by the role columns themselves (requester = sender,
 │  addressee = receiver), so no separate requestedBy column is needed.
 │  Gates routine + live-status visibility, nothing else.
```

**ER-diagram vs. schema naming:** on the ER diagram the two CourseSection links off SwapRequest are named as verb-phrase relationships — `sits_in` (current section) and `wants_section` (desired section). In the MySQL schema those same two relationships become the foreign-key columns `current_section_id` and `desired_section_id` on the `swap_request` table, each with an explicit `FOREIGN KEY ... REFERENCES course_section(id)`. Same links, two names, two artifacts — the verb phrase communicates meaning on the diagram, the column is the implementation. The rest of this doc uses the column names.

`PublicProfileDto` (what fellow members of the same `SwapGroup` see of each other, via their linked `SwapRequest`s) always includes `fullName` + `studentId`. `fbProfileUrl` is shown **whenever it's filled in — friend or not, no toggle** — since it's now the actual coordination channel now that there's no in-app chat (§5 explains why). `phoneNumber` stays gated behind `phonePublic` as before, since a phone number is a step more sensitive than a social link; flip that assumption if you'd rather treat it the same way. Neither field is mandatory to fill in.

---

## 3. Course data ingestion & the section picker

**Sync job:** `CourseSyncScheduler` (`@Scheduled`, every 5–15 min) calls `ConnectJsonClient`, which does a plain GET on `connect.json`, deserializes the array into `List<ConnectJsonSectionDto>` (Jackson binds it directly — the JSON keys are already camelCase matching Java field names, no custom mapping needed), and upserts each row into `course_section` keyed on `(section_id, semester_session_id)`. With JdbcTemplate the upsert is one hand-written statement — MySQL's `INSERT ... ON DUPLICATE KEY UPDATE` does it in a single round trip, and `jdbc.batchUpdate(...)` (the same idea as the `addBatch()`/`executeBatch()` you've already used) keeps the several-thousand-row sync fast. Since `courseCode`/`courseName` ride along on every section row, there's no separate `Course` table to maintain — a "course" is just the set of `course_section` rows sharing a `courseCode`.

**Section picker endpoint** — this is what powers your "type a course code, see section bars with faculty + timing" UI:

```
GET /api/courses/{courseCode}/sections?semesterSessionId=...

→ [
    { "sectionName": "1",  "theoryFaculty": "NTN", "theoryTiming": "Sat/Thu 11:00–12:20",
      "hasLab": true,  "labFaculty": "TBA", "labTiming": "Tue 11:00–13:50", "availableSeats": 0 },
    { "sectionName": "11", "theoryFaculty": "MSMA", "theoryTiming": "Mon/Wed 11:00–12:20",
      "hasLab": false, "availableSeats": 12 },
    { "sectionName": "12", ... },
    { "sectionName": "13", ... },
    { "sectionName": "15B", ... }
  ]
```

A course typically has a handful to a few dozen sections — fetch this list once per course code and let the frontend do prefix-filtering client-side (typing "1" narrows to 1, 11, 12, 13, 15B). No need for server-side search complexity here. This same endpoint feeds **both** boxes in the swap-request form ("your section" / "section you want") and the "add a course" step of the routine builder.

---

## 4. Routine builder

Student adds a course by `courseCode` + `sectionName` only (an `Enrollment` row). The backend resolves the rest automatically:

1. Look up `CourseSection` for `(courseCode, sectionName, courseType=THEORY, semesterSessionId)`.
2. Read `sectionSchedule.classSchedules` → the theory day/time blocks, tagged with `faculties`.
3. If `labSectionId` is non-null (your `CSE370` → `CSE370L` case), also pull `labSchedules`, `labFaculties`, `labRoomName` — the student never selects the lab separately, it's bundled from the same record.
4. Repeat for every `Enrollment` the student has this semester, merge all blocks into one weekly grid keyed by day → sorted time slots, each slot tagged `courseCode (Theory|Lab)`, faculty initial, room.

Worked example straight from your sample: adding `CSE370` section `15` produces **Sat 11:00–12:20 (NTN, 09B-12C)**, **Thu 11:00–12:20 (NTN, 09B-12C)**, and automatically **Tue 11:00–13:50 (TBA, 12F-31L, "CSE370L")** — three blocks from one input.

**Grid layout, confirmed against your screenshot.** It's not a fixed hourly grid — rows are the *distinct `(startTime, endTime)` pairs* that actually occur across the student's blocks, sorted by start time, and columns are the 7 weekdays (Sunday → Saturday). Your screenshot shows exactly this: two separate rows both start at 11:00 (one runs to 12:20, the other to 1:50) because they're different `(start,end)` pairs, not because of any fixed-hour scaffold.

`RoutineService` builds it like this:
1. Pull every block (theory `classSchedules` + any auto-attached `labSchedules`) across all of a student's `Enrollment`s this semester.
2. Group them by the exact `(startTime, endTime)` tuple → each distinct tuple is one row, sorted ascending by `startTime`.
3. Within a row, place each block under its `day` column.
4. Format each cell as `{courseCode}-{sectionName} -{facultyInitial}-{room}` — e.g. `CSE370-15 -NTN-09B-12C` for theory, `CSE370L-15 -TBA-12F-31L` for the paired lab (same section number, per your screenshot's `CSE260`/`CSE260L`-16A and `CSE370`/`CSE370L`-15 pairs).

`WeeklyRoutineDto` returned by the API is just `List<RoutineRowDto{ timeLabel, cells: Map<DayOfWeek, String> }>` — the frontend renders it as a straight table, no layout logic needed on that side.

**Free bonus this data model gives you almost for free:** clash detection. Since every block has a precise day + start/end time, checking "does this desired section overlap something already in my routine" before a student even submits a swap request is a simple interval-overlap check on the same structures — worth adding once the core flow works.

---

## 5. Friends & live free/busy status

New feature, separate from swap groups entirely: a student's full weekly routine — and a live "are they free right now" check — is only visible to **accepted friends**, nobody else.

**Flow:** `Friendship` starts `PENDING` when one student sends a request, flips to `ACCEPTED` or `DECLINED` when the other responds. Two students are "friends" only once `ACCEPTED`. Normalize `(userAId, userBId)` at write time (smaller ID first) so you can't end up with duplicate rows depending on who sent the request.

```
POST   /api/friends/request   { addresseeId }
POST   /api/friends/{id}/accept
POST   /api/friends/{id}/decline
DELETE /api/friends/{id}                      ← unfriend
GET    /api/friends                            ← accepted list
GET    /api/friends/requests                   ← pending incoming
```

**Gated endpoints — both check `FriendshipService.areFriends(currentUser, targetUser)` server-side and return 403 otherwise:**

```
GET /api/users/{userId}/routine    → the full WeeklyRoutineDto from §4
GET /api/users/{userId}/status     → live free/busy, computed on request
```

**Live status is just an interval-containment check reusing §4's data** — no separate storage needed:
- Map "now" to `(DayOfWeek, LocalTime)` in `Asia/Dhaka`.
- Scan that student's merged blocks for one where `day == today` and `startTime ≤ now < endTime`.
- Match → `{ "status": "BUSY", "courseCode": "CSE370", "until": "12:20" }`. No match → `{ "status": "FREE" }`.
- Nice-to-have once the core works: "free until 2:00 PM" by finding the next block later today, instead of a flat FREE/BUSY.

This is deliberately unrelated to swap-group visibility (§2) — a swap-group partner sees your name, student ID, and FB link/phone regardless of friend status, since that's about coordinating one specific swap. Friendship is a separate, broader "let this person see my whole schedule" permission.

---

## 6. The matching engine — suggest, propose, invite, confirm

Same hard rule as before, unchanged: **a swap only ever exchanges sections within one course.** Partition entirely by `courseCode` — one `SwapGraph` per course, so cross-course matching isn't a validation you remember, it's a state the algorithm can't reach.

**This is a suggest → propose → invite → confirm flow, not silent auto-matching, and the student picks which suggestion to act on.**

**Step 1 — generate suggestions.** When a student submits a `SwapRequest` (have = H, want = W, course = C), search **every size from 2 through 5** and return all of them, not just the smallest:

1. **Size 2:** is there another unreserved request in the same course with `have = W` and `want = H`? Direct swap-back.
2. **Size 3:** bounded DFS from `W`, one hop: someone with `have = W, want = X`, then someone with `have = X, want = H`, closing the loop.
3. **Size 4, then size 5** (your stated cap): same idea, one more hop each time, depth-capped so it stays cheap.

Show the student the full menu of viable groups it found — a size-2 result is flagged as the fastest/easiest, but it's their choice, not an automatic pick. Use a **Union-Find** structure per course as a cheap pre-filter: check connectivity between `W` and `H` before running any DFS at all — if they're not even in the same component, skip straight to "no suggestions yet."

**Step 2 — propose.** Clicking one specific suggestion is a **propose** action. It creates a `SwapGroup` (status `PROPOSED`), links exactly the `SwapRequest`s in *that* candidate group to it via `groupId`, and flips only those requests to `RESERVED`. Crucially: **only the other members of that chosen group get notified** — nobody else's pending request is touched, and this student's other suggestions (the size-3 and size-4 options they didn't pick) stay untouched too, for now.

**Step 3 — invite & confirm.** Each invited member sees the proposal and accepts or declines independently, flipping their own `SwapRequest.confirmed`. Once **every** member in the group has confirmed, the `SwapGroup` becomes `CONFIRMED` and all its requests become `MATCHED` for good.

**Step 4 — the reservation is what keeps this safe.** The instant a group is proposed (not once it's confirmed), its members are `RESERVED` — this is what stops the same student from being proposed into two different candidate groups at once. Concretely: if student B appeared in both a size-2 suggestion for student A *and* a size-3 suggestion for student D, and A proposes first, B's request flips to `RESERVED` immediately. If D then tries to propose their size-3 group, that propose call should fail gracefully ("this option is no longer available") and hand back a refreshed suggestion list — not send an invite into a group that can't actually form.

**Step 5 — falling apart, and re-entering the pool.** If anyone declines, or a timeout passes (e.g. 24h) without full confirmation, release the reservation: the group is `CANCELLED`, every member's request drops back to `PENDING`, `groupId` clears, and they're eligible for fresh suggestion searches again — including ones they weren't part of before.

This is the same class of problem as before (Top Trading Cycles / cyclic exchange), just with an explicit human-in-the-loop confirmation step layered on, which is the right call given real people have to actually show up and register the new section.

---

## 7. Build order (phases)

| Phase | Goal |
|---|---|
| 0 | Spec, ERD, wireframes |
| 1 | Auth: `@g.bracu.ac.bd`-gated signup/login, email OTP, JWT |
| 2 | **Hand-write `schema.sql`** (all 7 tables, PK/FK/UNIQUE), + `CourseSection` POJO, RowMapper and DAO, + one-off import from a saved `connect.json` snapshot |
| 3 | `CourseSyncScheduler` hitting the real `connect.json` on a timer — seat counts and section lists are now live |
| 4 | Section-picker endpoint + frontend autocomplete (course code → section bars with faculty/timing) |
| 5 | `Enrollment` DAO + CRUD, then routine builder (auto-attach lab, weekly grid endpoint) |
| 6 | Student profile: phone (toggle-gated) + FB link (always shown if filled), neither mandatory |
| 7 | Swap requests: create/cancel, enforce same-course rule |
| 8 | Matching engine v1 — **size-2 direct swap suggestions only**, with the propose/invite/confirm flow |
| 9 | Matching engine v2 — generalize to sizes 3–5 with bounded DFS + Union-Find; show all viable sizes, student picks which to propose |
| 10 | Friend system: request/accept/decline/unfriend |
| 11 | Friend-gated routine view + live free/busy status endpoint |
| 12 | Notifications (in-app + email) on suggestion, confirmation, friend request, expiry |
| 13 | Admin tools, rate limiting, load-test the matching engine, Redis caching for hot section/seat data |
| 14 | Deploy: Docker + CI/CD, managed MySQL, pick a host |

Same advice as before: don't skip 8 to jump straight to 9. A working direct-swap-with-confirmation is a complete, shippable product on its own.

---

## 8. File structure (Spring Boot, Maven, package-by-feature)

```
braculink/
├── pom.xml                                      ← spring-boot-starter-jdbc (NOT data-jpa)
├── src/main/java/com/braculink/
│   ├── BraculinkApplication.java
│   │
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   └── OpenApiConfig.java
│   │
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/EmailVerificationService.java
│   │   ├── dao/OtpDao.java                      ← hand-written SQL
│   │   ├── dto/SignupRequest.java, LoginRequest.java, JwtResponse.java
│   │   └── jwt/JwtUtil.java, JwtAuthFilter.java
│   │
│   ├── user/
│   │   ├── model/User.java                      ← plain POJO, no annotations
│   │   ├── dao/UserDao.java                     ← all USER SQL lives here
│   │   ├── dao/UserRowMapper.java
│   │   ├── service/UserService.java
│   │   ├── dto/PublicProfileDto.java
│   │   └── controller/UserController.java
│   │
│   ├── course/
│   │   ├── model/CourseSection.java             ← also serves as the "course"
│   │   ├── model/ClassSlot.java                 ← {day, startTime, endTime}
│   │   ├── dao/CourseSectionDao.java
│   │   ├── dao/CourseSectionRowMapper.java      ← parses the two JSON columns
│   │   ├── dto/ConnectJsonSectionDto.java       ← 1:1 mapping of connect.json's shape
│   │   ├── dto/SectionPickerDto.java
│   │   ├── sync/ConnectJsonClient.java          ← RestClient GET + Jackson
│   │   ├── sync/CourseSyncScheduler.java        ← @Scheduled
│   │   ├── service/CourseService.java
│   │   ├── service/SectionPickerService.java
│   │   └── controller/CourseController.java
│   │
│   ├── routine/
│   │   ├── model/Enrollment.java
│   │   ├── dao/EnrollmentDao.java
│   │   ├── dao/EnrollmentRowMapper.java
│   │   ├── dto/RoutineRowDto.java, WeeklyRoutineDto.java, LiveStatusDto.java
│   │   ├── service/RoutineService.java          ← weekly grid + getLiveStatus()
│   │   └── controller/RoutineController.java    ← friend-gated, see §5
│   │
│   ├── swap/
│   │   ├── model/SwapRequest.java, SwapGroup.java
│   │   ├── dao/SwapRequestDao.java, SwapGroupDao.java
│   │   ├── dao/SwapRequestRowMapper.java, SwapGroupRowMapper.java
│   │   ├── engine/SwapGraph.java                ← pure Java, no SQL, no Spring
│   │   ├── engine/UnionFind.java
│   │   ├── engine/CycleMatchingService.java     ← all size-2→5 candidate groups
│   │   ├── dto/SwapSuggestionDto.java
│   │   ├── service/SwapRequestService.java
│   │   ├── service/SwapGroupProposalService.java ← propose/invite/confirm, @Transactional
│   │   └── controller/SwapController.java
│   │
│   ├── friend/
│   │   ├── model/Friendship.java
│   │   ├── dao/FriendshipDao.java               ← composite-PK SQL lives here
│   │   ├── dao/FriendshipRowMapper.java
│   │   ├── service/FriendshipService.java       ← areFriends()
│   │   └── controller/FriendController.java
│   │
│   ├── notification/
│   │   ├── model/Notification.java
│   │   ├── dao/NotificationDao.java
│   │   ├── dao/NotificationRowMapper.java
│   │   ├── service/NotificationService.java
│   │   └── controller/NotificationController.java
│   │
│   └── common/
│       ├── exception/GlobalExceptionHandler.java, ApiException.java
│       ├── dto/ApiResponse.java
│       └── util/EmailDomainValidator.java
│
├── src/main/resources/
│   ├── application.yml                          ← datasource + spring.sql.init.mode=always
│   ├── application-dev.yml, application-prod.yml
│   ├── schema.sql        ★ HAND-WRITTEN DDL — the deliverable your teacher wants
│   └── data.sql          (optional seed data for demoing)
│
└── src/test/java/com/braculink/
    ├── swap/engine/CycleMatchingServiceTest.java  ← most valuable tests you'll write
    ├── course/sync/ConnectJsonClientTest.java
    └── ... (mirrors main package structure)
```

**`schema.sql` is the centrepiece for a database course** — it's the direct, line-by-line realisation of your relational-schema diagram: 7 `CREATE TABLE` blocks, every `PRIMARY KEY` (including the composite one on `friendship`), every `FOREIGN KEY` with its `REFERENCES`, and the `UNIQUE` constraint on `(section_id, semester_session_id)`. Show that file next to the drawio diagram and the mapping is self-evident.

**Frontend (separate repo):**
```
braculink-frontend/
├── app/
├── components/
│   ├── auth/, courses/ (SectionPicker, SeatCounter), routine/ (WeeklyGrid, FreeBusyBadge),
│   ├── swap/ (SwapRequestForm, SuggestionList), friends/ (FriendRequests, FriendList), profile/
├── lib/api.ts
└── lib/types.ts
```

---

## 9. Frontend, given you're Java-only

Same plan as before: generate an OpenAPI spec with `springdoc-openapi` off your controllers (near-zero extra work), feed it to an AI frontend tool (v0.dev, bolt.new, or Claude) to scaffold real screens against your actual endpoints, and iterate from there. On the swap-group screen, replace the chat box idea with a simple "contact via Facebook" link / phone number pulled straight from `PublicProfileDto`.

---

## 10. Non-functional notes

- **Email domain + OTP** at signup, as before.
- **Rate-limit** swap-request creation, friend requests, and the section-picker endpoint.
- **Always use `?` placeholders**, never string concatenation, in every DAO — same rule as the `PreparedStatement` work you've already done. This is what keeps the app safe from SQL injection, and it's the kind of thing a database course marks for.
- **Idempotent sync:** upsert by `(section_id, semester_session_id)` — the `UNIQUE KEY` in `schema.sql` plus `ON DUPLICATE KEY UPDATE` is what actually guarantees this, so the constraint is doing real work, not decoration.
- **Idempotent matching:** the `RESERVED` state (§6) is what prevents double-booking a student into two groups — don't skip it even for the MVP 2-way case. Wrap the propose operation in `@Transactional` so the whole group flips to RESERVED or none of it does; `@Transactional` behaves identically over JdbcTemplate.
- **Show data freshness:** display `lastSyncedAt` ("seats as of X min ago") so nobody treats a cached seat count as a live guarantee.
- **Timezone:** compute live free/busy status (§5) in `Asia/Dhaka`, not server-default or UTC — an off-by-timezone bug here just silently shows everyone as busy or free at the wrong times.
- **Privacy boundary, worth keeping distinct in code too:** friend-gated data (full routine, live status) and swap-group-visible data (name, ID, FB link, phone) are two separate authorization checks. Don't let one accidentally cover for the other.

---

### Suggested next step
Phase 1 (auth) and Phase 2 (course models + a saved JSON snapshot for dev) can start in parallel.