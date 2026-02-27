# KraftLog

A local-first Android workout tracking app built with Jetpack Compose and Material3.

## Features

- **Exercise Library** — 31 pre-loaded exercises across Strength, Cardio, Calisthenics, Flexibility, and Plyometrics categories. Add your own custom exercises.
- **Routine Builder** — Create reusable workout templates with exercises, target sets/reps/weight, and rest time.
- **Workout Logging** — Start a session from any routine or as a quick ad-hoc workout. Log sets with weight and reps in real time with an elapsed timer.
- **History & Progress** — Browse past sessions grouped by month, view total volume per session, and track set history per exercise.

All data is stored locally — no account or internet connection required.

## Screenshots

*Coming soon*

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.10 |
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose 2.9.7 |
| Database | Room 2.8.4 |
| Annotation Processing | KSP 2.3.6 |
| State Management | ViewModel + StateFlow |
| Build | AGP 9.0.1, Gradle version catalog |

## Architecture

```
app/
└── src/main/java/de/nyxnord/kraftlog/
    ├── KraftLogApplication.kt       # Manual DI container
    ├── MainActivity.kt              # Entry point
    ├── data/
    │   ├── local/
    │   │   ├── entity/              # Room entities
    │   │   ├── dao/                 # Data access objects
    │   │   ├── relation/            # Room relation classes
    │   │   ├── Converters.kt        # Type converters
    │   │   └── KraftLogDatabase.kt  # Database + seed data
    │   └── repository/              # Repository layer
    └── ui/
        ├── navigation/              # NavHost + route definitions
        ├── home/                    # Dashboard
        ├── routines/                # Routine list, detail, edit
        ├── exercises/               # Exercise library + detail
        ├── workout/                 # Active workout session
        └── history/                 # Session history + detail
```

The app follows a straightforward data → repository → ViewModel → Composable pattern. Dependency injection is handled manually via `KraftLogApplication`, which lazily initializes the Room database and exposes the three repository instances. No Hilt or Dagger.

## Database Schema

| Table | Description |
|---|---|
| `exercises` | Exercise definitions (seeded + user-created) |
| `routines` | Workout templates |
| `routine_exercises` | Exercises within a routine with targets |
| `workout_sessions` | Individual workout sessions |
| `workout_sets` | Logged sets within a session |

## Requirements

- Android 16 (API 36) or higher
- Android Studio Meerkat or newer

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle (`File → Sync Project with Gradle Files`)
4. Run on a device or emulator running Android 16+

```bash
./gradlew assembleDebug
```

## License

MIT
