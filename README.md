# FacultyTimeTablePro

An offline-first Android application for building and managing college faculty timetables. It replaces spreadsheet-driven scheduling with verified master data, a constraint-aware automatic generator, and role-focused timetable views.

**Version 1.1.0** · `minSdk 26` · `targetSdk 36` · Kotlin 2.1 · Jetpack Compose · Room · Hilt

---

## Highlights

- **Conflict-free automatic generation** — respects faculty, room, section, workload, lab-continuity and room-capacity constraints.
- **Real lab scheduling** — labs are placed as consecutive blocks in actual laboratory rooms, never in classrooms.
- **Seeded demo that actually works** — first launch creates a complete, conflict-free 48-period timetable across four sections. No setup required to evaluate the app.
- **Five-tab professional workflow** — Home, Timetable, Faculty, Reports, More.
- **Complete master data** — departments, faculty, subjects (theory/lab/project), rooms, sections, academic years, semesters, holidays and configurable periods.
- **Faculty operations** — individual faculty timetables, leave records and substitute tracking.
- **Real reports** — faculty workload, room utilization, section coverage and department summary, with CSV export.
- **Real backup and restore** — versioned JSON snapshots written through the Android Storage Access Framework, with checksum verification, preview and atomic restore.
- **PIN app lock** — salted SHA-256 PIN hashing, 60-second background grace period, no plaintext credentials.
- **Predictable database upgrades** — Room schema export checked in and a real `1 → 2` migration; no destructive fallback.

---

## Screens

| Tab / area | What it does |
|---|---|
| **Home** | Live counts, timetable health, today's classes, quick actions |
| **Timetable** | Section picker, day tabs, weekly grid, class details, delete entry |
| **Faculty** | Directory, profiles, weekly timetable, leave management |
| **Reports** | Workload, room utilization, section coverage, department summary, CSV export |
| **More** | Generator, academic calendar, periods, master data, backup, settings |

### Master data

- Departments with code, head and description
- Faculty with designation, qualifications, contact details and weekly-hour limits
- Subjects with theory/lab hours, lab-required flag and department
- Rooms with type, capacity, building, floor, projector and AC
- Sections with department, semester and strength
- Academic years, semesters and holidays
- Time slots per weekday with regular / lunch / break types

### Timetable generation

`AssignmentPlanner` deterministically maps each section's subjects to faculty in the section's department, balancing weekly load. `TimetableGenerator` then schedules every required period:

1. Lab blocks are placed first, as consecutive periods in a single day and a single lab room.
2. Theory and project periods are placed next using the section's home room when possible.
3. Every candidate placement checks section, faculty and room availability, weekly workload and room capacity.
4. Five deterministic seeds are attempted; the best conflict-free result is kept and independently validated.
5. The previous timetable is replaced atomically — a failed generation never destroys the existing timetable.

Supported conflict types include faculty clash, room clash, section clash, lab continuity, workload exceeded, room capacity exceeded and unscheduled periods.

---

## Demo data

The built-in sample data (`SampleDataProvider`) is the same data used by the unit tests, so what you see in the app is exactly what is verified.

| Entity | Count | Examples |
|---|---|---|
| Departments | 3 | CSE, ECE, ME |
| Faculty | 6 | Dr. Arun Kumar, Prof. Neha Gupta, Dr. Amit Singh |
| Subjects | 10 | DSA, DBMS, DSA Lab, Digital Logic, Thermodynamics |
| Rooms | 8 | CSE-101, ECE Lab, ME Lab, Seminar Hall |
| Sections | 4 | CSE 3A, CSE 3B, ECE 3A, ME 3A |
| Time slots | 40 | 5 days × 8 periods (period 5 lunch, period 7 break) |
| Generated periods | **48** | CSE 3A 13, CSE 3B 13, ECE 3A 11, ME 3A 11 |

Generation result on this dataset: **48/48 periods scheduled, 0 conflicts**, all labs consecutive and in lab rooms, all rooms large enough for their sections, all faculty within weekly limits.

You can regenerate or restore this dataset at any time from **More → Backup & data → Reset demo data** (or the Settings screen), and run **More → Generate timetable**.

---

## Architecture

```
app/src/main/java/com/facultytimetable/pro
├── data
│   ├── local
│   │   ├── backup/         JSON snapshot export/import, SAF, checksums
│   │   ├── datastore/      AppPreferences (theme, lock, onboarding)
│   │   ├── db/             Room database v2, 15 entities, 15 DAOs, migration
│   │   ├── security/       PinHasher (salted SHA-256)
│   │   └── seed/           SampleDataProvider, idempotent DatabaseSeeder
│   ├── model/              Cross-entity projections
│   └── repository/         Repository implementations
├── di/                     Hilt modules
├── domain
│   ├── repository/         Repository contracts
│   └── usecase/timetable/  AssignmentPlanner, TimetableGenerator, ConflictEngine
└── presentation
    ├── academic/           Calendar, semesters, holidays, period configuration
    ├── backup/             SAF backup/restore UI and view model
    ├── common/             Shared Compose components, subject colors
    ├── dashboard/          Home screen and live statistics
    ├── department/ faculty/ room/ section/ subject/
    ├── faculty/leave/      Leave records
    ├── more/               Five-tab hub
    ├── navigation/         Routes and NavGraph
    ├── reports/            Report calculations and CSV export
    ├── root/               Theme + PIN lock gate
    ├── settings/           Preferences and data management
    └── timetable/          Grid, section, faculty, generator
```

- **Pattern:** MVVM with a repository boundary and immutable UI state.
- **Reactive:** Room `Flow`s are combined into `StateFlow` screen state.
- **DI:** Hilt throughout, including the database, repositories, planner and generator.
- **Persistence:** Room 2.7 with exported schemas in `app/schemas/` and no destructive migration.
- **Offline:** no `INTERNET` permission; all data stays on-device.

### Database

Version 2 adds section ownership to sections and supporting indexes. The `1 → 2` migration is implemented explicitly in `AppDatabase.kt`; `fallbackToDestructiveMigration()` is not used.

Timetable entries have database-level uniqueness for section, faculty and room per day/slot, so direct writes cannot silently double-book.

---

## Build

### Requirements

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- `ANDROID_HOME` set, or a `local.properties` file with `sdk.dir`

### Commands

```bash
# Unit tests
./gradlew testDebugUnitTest --no-daemon --no-configuration-cache

# Debug APK
./gradlew assembleDebug --no-daemon --no-configuration-cache

# Both (what CI runs)
./gradlew testDebugUnitTest assembleDebug --no-daemon --no-configuration-cache
```

Output:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper JAR is committed, so a fresh clone builds without regenerating the wrapper.

### CI

`.github/workflows/android-build.yml` runs on every push to `main` and on manual dispatch:

1. JDK 17 (Temurin) and Android SDK 36
2. `testDebugUnitTest`
3. `assembleDebug`
4. APK presence check, SHA-256 checksum and `aapt2` metadata dump
5. Uploads the installable APK and checksum as the `FacultyTimeTablePro-v1.1.0-debug` artifact (90-day retention), plus the unit-test report

Download the latest APK from the **Actions** tab of the repository: open the latest successful *Android Build* run and use the artifact section.

**Direct download:** [FacultyTimeTablePro v1.1.0 release](https://github.com/Harish-fake/FacultyTimeTablePro/releases/tag/v1.1.0)  
APK SHA-256: `73be798e7e2cbfefffafc6bd0905f0c3a1a925db58a03631f934944db10ef930`

---

## Tests

`app/src/test/java/com/facultytimetable/pro/domain/usecase/timetable/TimetableGeneratorTest.kt` verifies:

1. `sample data produces a complete conflict free timetable` — every required period is scheduled; zero conflicts; no duplicate section, faculty or room slots; no lunch/break teaching; labs are consecutive, single-day, single-room and in a `LAB`; faculty weekly limits and room capacities hold.
2. `generation is deterministic for the same seed` — identical output for identical input and seed.
3. `lunch and break periods are never used` — non-teaching slots are excluded.

Local result (JDK 17, Android SDK 36):

```
tests="3" skipped="0" failures="0" errors="0"
BUILD SUCCESSFUL
```

---

## Backup format

Backups are JSON documents containing a schema version, export timestamp, checksum, row counts and all tables. Restore:

1. Validates the schema version and checksum.
2. Checks every foreign-key reference before writing.
3. Replaces tables atomically inside a Room transaction.

Files are written and read with the Storage Access Framework, so no broad storage permission is required. Backup history (name, size, checksum, timestamp) is recorded in the app.

---

## Privacy and security

- 100% offline; the app declares no network permission.
- Android auto-backup is disabled; data extraction rules exclude app data.
- App lock uses a salted SHA-256 hash. The PIN is never stored in plaintext.
- Biometrics and unused security libraries were intentionally removed.
- Clearing app data or uninstalling removes all local data; export a backup first.

---

## Project status

Version 1.1.0 is a complete, buildable release of the application:

- All previously broken user flows are fixed (section editing, faculty timetable/leave navigation, destructive generation, report/backup stubs, direct DAO writes).
- All promised screens are implemented and reachable.
- Database migration and schema export replace the destructive prototype setup.
- The generator handles labs, capacity, workload and determinism, and is covered by tests.
- CI produces a verified, installable APK artifact.

See [`PROJECT_REPORT.md`](PROJECT_REPORT.md) for the detailed verification report, including every defect that was fixed and the evidence for each fix.
