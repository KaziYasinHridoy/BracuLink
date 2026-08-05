# CLAUDE.md

## Project

Braculink: BRACU students sign up with `@g.bracu.ac.bd` email, see live course
section data, build their class routine, add friends, and get matched into
groups to swap sections of the same course.

## Stack — READ THIS FIRST

Java 17, Spring Boot 3.x, MySQL 8, Spring Security + JWT, Maven.

**DATA ACCESS IS SPRING JDBC (`JdbcTemplate`) WITH HAND-WRITTEN SQL.**
Absolutely **NO JPA, NO Hibernate, NO `@Entity`, NO `JpaRepository`, NO `ddl-auto`.**
This is a database course project — every query must be visible SQL.

Runs on localhost only: no Docker, no CI, no Redis, no Flyway.

## Data access pattern (follow this everywhere)

- `model/` -> plain POJOs, no annotations, just fields + getters/setters
- `dao/` -> classes holding all SQL, using `JdbcTemplate`
- `dao/` -> one `RowMapper` per table, mapping `ResultSet` -> POJO
- Always use `?` placeholders, never string concatenation (SQL injection).
- Schema lives in `src/main/resources/schema.sql`, hand-written, never generated.

## Tables (7 — final, do not add more without asking)

- `course_section` (also serves as "course" — there is no separate course table)
- `user`
- `enrollment`
- `swap_request`
- `swap_group`
- `notification`
- `friendship`: composite PK (`requester_id`, `addressee_id`) + `status`, `created_at`

## Naming

Tables/columns `snake_case`, Java fields `camelCase`.
`course_code` <-> `courseCode`, `current_section_id` <-> `currentSectionId`, etc.

## Key rules

- Swaps are ONLY between sections of the same course. Never course-to-course.
- Section data comes from `https://usis-cdn.eniamza.com/connect.json` (public, no auth)
- Live free/busy status is computed in `Asia/Dhaka` timezone
- Full routine + live status visible ONLY to accepted friends
- FB profile link visible to anyone; phone gated behind `phone_public` flag

## Conventions

- DTOs for all API input/output, never expose models directly
- Constructor injection, no `@Autowired` on fields
- Controllers return `ApiResponse<T>`

## Commands

```
mvn spring-boot:run
mvn test
mvn clean compile
```