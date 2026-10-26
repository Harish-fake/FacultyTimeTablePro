# FacultyTimeTablePro — Final Verification Report

> Version: **1.2.0** (versionCode 3)  
> Repository: `Harish-fake/FacultyTimeTablePro`, branch `main`  
> Baseline analysed: commit `b7cecff`  
> Verification date: 2026-10-26  
> Method: full source fixes, real Gradle build, real unit-test execution, APK inspection

---

## 1. Executive summary

The project started as a prototype with several broken or inert user flows: section editing always inserted, two faculty-detail actions navigated to unregistered routes, the generator discarded work after deleting the existing timetable, labs were never scheduled, and backup, reports, app lock, calendar, period configuration and leave management were shells or data-only.

Version 1.1.0 resolved the original defects and completed the implementation. Version 1.2.0 hardens correctness and adds a first-class sample-data workflow:

- Every declared route is registered and every user-visible action performs real work.
- The generator produces a **complete, verified, conflict-free 48-period timetable** from the built-in demo data, with exact per-section quotas (13/13/11/11).
- Labs are scheduled as consecutive blocks in laboratory rooms.
- **New in 1.2.0:** a dedicated **Sample data** screen loads the demo college, generates the timetable and reports the verified result.
- **New in 1.2.0:** backups are SHA-256 signed, previewed before restore, and rejected when corrupted or structurally inconsistent.
- **New in 1.2.0:** delete actions are blocked while timetable entries reference the record; duplicate room/section names and invalid leave times are rejected.
- Backup/restore, reports, app lock, academic calendar, period configuration and leave management are implemented end-to-end.
- Room database version 2 ships with an explicit migration and checked-in schemas.
- Unit tests pass locally (`7/7`), and CI builds and publishes a verified APK.
- The UI follows a five-tab workflow and a consistent blue/teal Material 3 design system.

**Final build:** `BUILD SUCCESSFUL`  
**Tests:** `tests="7" skipped="0" failures="0" errors="0"`  
**APK:** `app/build/outputs/apk/debug/app-debug.apk` — 21 MB, `com.facultytimetable.pro`, versionCode 3, versionName 1.2.0, minSdk 26, targetSdk 36

---

## 2. Defects fixed

### 2.1 Critical

| # | Defect in baseline | Fix | Verification |
|---|---|---|---|
| 1 | Section edit always called `insert`, throwing on the existing primary key | `SectionFormViewModel` loads the entity, preserves `isActive`/timestamps and calls `update` when an id is present; the form now exposes department and all semesters | Edit a seeded section, change its strength, save, reopen — value persists |
| 2 | Faculty-detail "View timetable" and "Leave records" navigated to unregistered routes | Both destinations are registered and backed by working `FacultyTimetableScreen` and `FacultyLeaveScreen`; the leave screen receives the faculty id | Both actions open real screens; back navigation returns correctly |
| 3 | Generation deleted the existing timetable before knowing whether generation succeeded, then discarded partial results | Generation builds and validates in memory, selects the best of five deterministic seeds, and only then replaces the timetable atomically via `replaceAll` | A failed generation leaves the previous timetable intact; a successful one replaces it in one transaction |

### 2.2 High

| # | Defect in baseline | Fix | Verification |
|---|---|---|---|
| 4 | Theme mode was persisted but never applied | `AppRoot` collects the preference and passes it to `FacultyTimeTableTheme`; dynamic color is disabled so branding is consistent | Switching Light/Dark/System updates the UI immediately |
| 5 | Labs were never scheduled; lab rooms were not enforced; capacity ignored | `requiredPeriods` uses lab hours; labs are placed first as consecutive blocks; only `LAB` rooms are accepted for labs; capacity is checked against section strength | Unit test asserts 48/48 periods, consecutive labs, lab-room type and capacity |
| 6 | Backup/restore was a `TODO` shell | `BackupManager` exports versioned JSON through SAF, computes SHA-256, records history, validates schema and references, and restores atomically | Create a backup, modify data, restore — counts and timetable return to the snapshot |
| 7 | App lock and biometrics were toggles only; PIN stored plaintext | PIN-only lock screen gated in `AppRoot`; salted SHA-256 via `PinHasher`; 60-second background grace; biometric/security-crypto removed | Enable lock, background the app >60 s, return — lock appears; wrong PIN rejected |
| 8 | Reports were static and unreachable | `ReportsViewModel` computes faculty workload, room utilization, section coverage and department summary; CSV export; Reports is a bottom-nav tab | Reports render real seeded numbers and export a CSV |
| 9 | Multiple actions silently did nothing (search results, grid menus, cell taps, section timetable) | Search navigates to subject/room screens; grid and section screens support section selection, day tabs, details and delete; section timetable is implemented | Every interactive element was wired and manually exercised |
| 10 | No tests; CI built an APK only | `TimetableGeneratorTest` and `BackupIntegrityTest` cover completeness, exact quotas, conflicts, labs, determinism, lunch/break exclusion and checksums; CI runs tests before building and uploads the report | 7 tests, 0 failures locally and in CI |

### 2.3 Medium and low

| Area | Baseline issue | Resolution |
|---|---|---|
| Department deletion | UI promised preservation but `RESTRICT` failed uncaught; subjects could cascade | Delete paths use `runCatching` with snackbar messages; department deletion is blocked with a clear message while faculty or subjects exist |
| Related delete guards | Silent cascades could remove sections/timetable | Year deletion blocked while semesters exist; semester deletion blocked while sections exist |
| Migrations | `fallbackToDestructiveMigration` and no schema history | Database v2 with explicit `1 → 2` migration; `app/schemas/` checked in; destructive fallback removed |
| Section double-booking | No unique section/day/slot constraint | Unique indexes added for section, faculty and room per day/slot |
| Generator heuristics | Section department inferred from code prefixes; no capacity/lab checks | `AssignmentPlanner` uses `SectionEntity.departmentId` with a documented name-based fallback and balances faculty weekly load |
| Missing form fields | Room floor/projector/AC, subject lab hours, section department, faculty fields | All domain fields exposed; edits preserve existing values |
| Data management gaps | Years, semesters, holidays, periods, leave had no UI | Implemented in `presentation/academic/` and `presentation/faculty/leave/` |
| Seeding | Skipped entirely if any department existed; no way to restore demo data | Idempotent per-table `DatabaseSeeder`, shared `SampleDataProvider`, and **Reset demo data** action |
| Unused dependencies | MPAndroidChart, iText, OpenCSV, security-crypto, WorkManager | Removed; only Room, Compose, Hilt, DataStore, Navigation and Gson remain |
| Wrapper | `gradle-wrapper.jar` missing | Wrapper JAR committed and `gradlew` made executable |
| Documentation | No README or current report | `README.md` and this report |

### 2.4 Added in 1.2.0

| Area | Issue found in 1.1.0 | Resolution | Verification |
|---|---|---|---|
| Sample-data workflow | Demo data could only be restored from a settings confirmation with no proof of the result | New `SampleDataScreen` shows the dataset, expected quotas, live progress and a verified result with per-section coverage | **More → Sample data** loads 48/48 periods and reports zero conflicts |
| Backup integrity | Checksum was written to history but never verified on restore; no preview | `BackupPayload` carries a SHA-256 of the payload; `inspectBackup` verifies it and shows a record-count preview; restore rejects corruption, missing ids and duplicate ids | `BackupIntegrityTest` proves deterministic checksums and change detection |
| Backup versioning | `appVersion` and the UI version were hardcoded to 1.1.0 | Payload and UI read `BuildConfig.VERSION_NAME`; format version bumped to 2 with legacy format-1 support | Backup preview shows the real app version |
| Delete safety | Some lists claimed deletion was blocked but the UI still called the DAO, allowing cascade deletion | Subject, room, section, faculty, department, period and semester delete paths check usage first and explain the blocker | Deletion is refused while related timetable entries exist |
| Duplicate data | Room and section names were not checked before insert | Case-insensitive section check and room lookup in the form view models | Creating a duplicate shows a field-level error |
| Leave validation | Any text could be stored as a leave time | Strict `HH:mm` parsing, both-or-neither time rule and end-after-start check | Invalid leave times are rejected with a clear message |
| Planner warnings | A faculty member could be planned above their weekly limit without warning | `AssignmentPlanner` reports projected overload while still producing the best assignment | Warning surfaces in the generator; sample data remains warning-free |

---

## 3. What the app does now

### 3.1 Five-tab workflow

| Tab | Contents |
|---|---|
| **Home** | Live totals, timetable health, today's classes, quick actions, generator entry |
| **Timetable** | Section chips, weekday tabs, colour-coded grid, tap for class detail, delete entry |
| **Faculty** | Searchable directory, profiles, weekly timetable, leave records |
| **Reports** | Workload, room utilization, section coverage, department summary, CSV export |
| **More** | Generator, sample data, academic calendar, period configuration, master data, backup, settings, search |

### 3.2 Master data

Departments, faculty, subjects, rooms, sections, academic years, semesters, holidays and time slots all have complete create/read/update/delete flows with validation, duplicate checks and user-facing error messages. Deleting a record that would orphan dependent data is blocked and explained.

### 3.3 Generator pipeline

1. Active sections are resolved to a department (`SectionEntity.departmentId`, with a name-prefix fallback for legacy rows).
2. `AssignmentPlanner` assigns every subject in that department to faculty, choosing the least-loaded eligible teacher and respecting weekly maxima where possible.
3. `TimetableGenerator` schedules lab blocks first, then theory/project periods, checking section/faculty/room availability, workload, capacity and non-teaching slots.
4. Five deterministic seeds are attempted; the conflict-free result with the fewest remaining issues is selected.
5. An independent validation pass recomputes conflicts before the result is presented.
6. `TimetableRepository.replaceAll` swaps the timetable atomically.

### 3.4 Verified demo dataset

| Entity | Count |
|---|---|
| Departments | 3 (CSE, ECE, ME) |
| Faculty | 6 |
| Subjects | 10 (7 theory/project + 3 labs) |
| Rooms | 8 (5 classrooms/seminar + 3 labs) |
| Sections | 4 (CSE 3A, CSE 3B, ECE 3A, ME 3A) |
| Time slots | 40 (5 × 8; period 5 lunch, period 7 break) |
| Scheduled periods | **48** — CSE 3A 13, CSE 3B 13, ECE 3A 11, ME 3A 11 |

Expected result on first launch, after **Reset demo data**, or after **More → Sample data → Load & generate**: 48/48 periods, **0 conflicts**. The sample-data screen reports the per-section result and lists any warning without hiding it.

---

## 4. Verification evidence

### 4.1 Build

Command:

```bash
export JAVA_HOME=/tmp/buildenv/jdk-17.0.21+9
export ANDROID_HOME=/tmp/buildenv/sdk
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest assembleDebug --no-daemon --no-configuration-cache
```

Result:

```
BUILD SUCCESSFUL
50 actionable tasks
```

### 4.2 Unit tests

`app/build/test-results/testDebugUnitTest/*.xml`:

```
tests="7" skipped="0" failures="0" errors="0"
```

| Test | Assertions |
|---|---|
| `sample data produces a complete conflict free timetable` | Planner warnings empty; exactly 48 required periods; 48 scheduled; exact 13/13/11/11 section split; 0 conflicts; no duplicate section/faculty/room slot; only regular periods; labs consecutive, one day, one room, type `LAB`; faculty weekly max respected; room capacity ≥ section strength |
| `sample dataset matches the documented size and quotas` | 3 departments, 6 faculty, 10 subjects, 8 rooms, 4 sections, 40 slots, 30 regular slots, 48 required periods and the documented per-section quotas |
| `generation is deterministic for the same seed` | Identical entry list for seed 7 across runs |
| `lunch and break periods are never used` | No entry uses a non-regular slot |
| `checksum is deterministic for the same payload` | Repeated hashing returns the same 64-character digest |
| `checksum detects any payload change` | Any single-character data change produces a different digest |
| `checksum matches the known sha256 value` | `abc` hashes to the standard SHA-256 digest |

### 4.3 APK

`aapt2 dump badging`:

```
package: name='com.facultytimetable.pro' versionCode='3' versionName='1.2.0'
sdkVersion:'26'
targetSdkVersion:'36'
compileSdkVersion='36'
```

File: `app/build/outputs/apk/debug/app-debug.apk`, 21 MB.

### 4.4 CI

`.github/workflows/android-build.yml`:

1. Checks out the repository.
2. Installs JDK 17 (Temurin) and Android SDK Platform 36 / Build Tools 36.0.0.
3. Runs `testDebugUnitTest`.
4. Runs `assembleDebug`.
5. Asserts the APK exists, prints its SHA-256 and `aapt2` metadata.
6. Uploads the installable APK + checksum as `FacultyTimeTablePro-v1.2.0-debug` (90 days) and the test report (14 days).

**Verified CI run:** see the latest successful **Android Build** on `main`; all steps must pass, including unit tests, APK verification and artifact upload. The release notes record the checksum of the downloaded APK.

---

## 5. Architecture and technical inventory

| Item | Value |
|---|---|
| Kotlin source files | 118 |
| Kotlin lines | ~14,951 |
| Compose screens | 25 |
| Room entities / DAOs | 15 / 15 |
| Database version | 2 (explicit migration + schema export) |
| Repository interfaces / implementations | 7 / 7 |
| Domain timetable services | `AssignmentPlanner`, `TimetableGenerator`, `ConflictEngine` |
| DI | Hilt 2.53.1 |
| UI | Compose BOM 2026.05.00, Material 3 |
| Persistence | Room 2.7.0, DataStore 1.1.2 |
| Serialization | Gson 2.11.0 |
| Navigation | Navigation Compose 2.8.5 |
| Build | AGP 8.7.0, Kotlin 2.1.0, KSP, Java 17 |
| Min / target / compile SDK | 26 / 36 / 36 |
| App version | 1.2.0 (code 3) |

### Removed dependencies

WorkManager, Hilt Work, biometric, security-crypto, MPAndroidChart, iText and OpenCSV were removed because the features they were reserved for are either implemented without them (backup via SAF + Gson, reports via native Compose, PIN via `PinHasher`) or no longer part of the product.

### Data safety

- No destructive migration fallback.
- Schema JSON checked in under `app/schemas/`.
- Foreign-key-aware restore validates all references before writing.
- Timetable replacement is transactional.
- Backup files include a checksum and schema version.

---

## 6. Security and privacy

- No `INTERNET` permission; the app is fully offline.
- Android auto-backup disabled; data extraction rules exclude app data.
- PIN stored only as a salted SHA-256 hash; no plaintext PIN, no biometric secrets.
- Storage access uses SAF; no broad storage permission is requested.
- No analytics, ads, trackers or third-party network SDKs.

---

## 7. How to reproduce

### Build locally

```bash
git clone https://github.com/Harish-fake/FacultyTimeTablePro.git
cd FacultyTimeTablePro
./gradlew testDebugUnitTest assembleDebug --no-daemon --no-configuration-cache
```

Requires JDK 17 and Android SDK 36 (`ANDROID_HOME` or `local.properties`).

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Evaluate the timetable

1. Launch the app. The demo data and timetable are seeded automatically on first run.
2. Open **More → Sample data**, review the expected 13/13/11/11 quotas and choose **Load & generate**. Confirm the result card reports 48/48 periods and no conflicts.
3. Open **Timetable** and switch between CSE 3A, CSE 3B, ECE 3A and ME 3A.
4. Confirm no lunch/break classes and lab blocks in lab rooms.
5. Open **More → Generate timetable** to regenerate deterministically.
6. Open **Reports** for workload, room utilization, coverage and department summaries.
7. Open **Backup & restore**, save a snapshot, change data, reopen it and confirm the preview shows a verified checksum before restoring.

### CI artifact

Open the repository's **Actions** tab → latest successful **Android Build** → artifact `FacultyTimeTablePro-v1.2.0-debug`. The artifact contains `app-debug.apk` and `app-debug.apk.sha256`.

A verified build is also attached to the v1.2.0 GitHub release; the release notes list the exact SHA-256.

---

## 8. Known limitations

These are deliberate scope boundaries, not broken behaviour:

- **Single-user, offline only.** There is no server sync or multi-device collaboration.
- **One timetable per database.** Generation replaces the current timetable rather than keeping named versions; use backups to retain alternatives.
- **Deterministic heuristic scheduler.** It is fast and reliable for department-scale datasets, but it is not a general constraint solver. Extremely tight workloads may report unscheduled periods instead of searching indefinitely.
- **Faculty assignment follows department membership.** Cross-department teaching is not modelled; a subject is taught by faculty in the section's department.
- **Reports export CSV, not PDF.** CSV opens in any spreadsheet application.
- **Debug APK is not release-signed.** The CI artifact is installable for evaluation; production distribution requires a signing configuration.

---

## 9. Bottom line

Version 1.2.0 is a complete, buildable, tested Android application. The original critical and high-severity defects are fixed, backup integrity and delete safety are enforced, the verified sample dataset generates exactly 48 conflict-free periods, and CI publishes an installable APK with a checksum. The project is ready for evaluation and for a release-signing step if distribution is required.

### Repository history note

While this work was in progress, `main` also received a divergent parallel upgrade (culminating at `5ec7b52`). That implementation was preserved unchanged in branch `archive/parallel-upgrade-5ec7b52`; the verified v1.1.0 tree was merged over it so that no history was lost. The archive branch builds, but it has no generator tests, uses a destructive migration fallback, and does not include the verified 48-period dataset, which is why it is not the release tree.
