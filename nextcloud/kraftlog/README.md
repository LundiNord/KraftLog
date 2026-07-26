# KraftLog for Nextcloud

KraftLog is a native, multi-user Nextcloud app for tracking strength
workouts, running, bouldering, routines, exercise progress and body weight.
Each account can access only its own records.

## Requirements

- Nextcloud 33–35
- PHP 8.2–8.5
- SQLite, MySQL/MariaDB or PostgreSQL

The app has no Composer, npm or CDN runtime dependencies. Its checked-in
JavaScript and CSS can be installed directly.

## Install from the source tree

1. Copy the `kraftlog` directory to a configured Nextcloud app directory,
   for example `custom_apps/kraftlog`.
2. Ensure the web-server user owns or can read the directory.
3. Enable the app:

   ```bash
   sudo -E -u www-data php occ app:enable kraftlog
   ```

4. Open **KraftLog** from the Nextcloud navigation. On first use, the app
   creates the 25 exercises and three routines from the Android version for
   that user.

If an administrator updates an unpacked development copy, run:

```bash
sudo -E -u www-data php occ upgrade
sudo -E -u www-data php occ migrations:status kraftlog
```

## Troubleshooting failed API requests

If an action shows an HTTP 500 error, KraftLog displays a short error ID and
writes the underlying exception to the Nextcloud log without exposing database
details in the browser. Reproduce the action, then search for the error ID in
**Administration settings → Logging** or in `data/nextcloud.log`. The matching
entry has the message `KraftLog API request failed`.

## Features

- Per-user exercise library with categories, muscle groups and instructions
- Front/back muscle diagrams ported from the Android app for exercise cards and details
- Wide-screen layout that expands dashboards and workout views up to 1760 px
- Reusable routines with ordered exercises and per-set targets
- Resumable strength workouts with live/rest timers, RPE and bodyweight sets
- Running sessions with distance, manual/live duration, pace and notes
- Bouldering sessions with attempted/completed routes and notes
- History, lifetime metrics, exercise records and routine-from-session
- Body-weight trend, changes and statistics
- Full JSON backup plus Android history/routine JSON import
- Responsive desktop and mobile layout using the Nextcloud theme variables

## Data model and security

The migration creates eight `kl_*` tables. Every table includes `user_id`.
All read, update and delete operations include both the object identity and
the authenticated Nextcloud user identity. The browser cannot submit a user
identity. Aggregate updates for routines and sessions run in transactions,
imports are committed as a single transaction, and App Framework CSRF
protection remains enabled on every API route. Unique database constraints
prevent duplicate seed data and more than one active session per account.
KraftLog also removes all workout data before a Nextcloud user is deleted or
an external user ID is unassigned.

## Development checks

```bash
php -l appinfo/routes.php
find lib templates -name '*.php' -exec php -l {} \;
node --check js/kraftlog.js
sudo -E -u www-data php occ app:check-code kraftlog
```

Open `tests/ui-smoke.html` directly in a browser for a backend-free visual
smoke test with representative data.

## Scope

Android-only integrations such as the Glance home-screen widget,
WorkManager notifications and GPS tracking are not part of this server-side
rewrite. A Nextcloud Dashboard widget, Notifications integration and an
authenticated OCS sync API can be added independently.
