# Migrating data from KraftLog Android

The existing Android app offers two JSON exports:

- **History JSON v1**: all completed strength, running and bouldering sessions
- **Routine JSON v1**: one routine and its exercise targets

In the Nextcloud app, open **Verlauf → Import** and select either file.
KraftLog recognizes both formats. During import it:

1. deduplicates sessions by `startedAt`;
2. resolves exercises by their case-insensitive names rather than trusting
   Android database IDs;
3. creates missing exercises as custom strength exercises;
4. remaps all routine and session relations to Nextcloud-owned IDs; and
5. validates types, timestamps and numeric ranges on the server.

The Android history export does not contain body-weight records, exercise
metadata, reminder preferences or complete routine links. Those values cannot
be recovered from that JSON. A lossless migration would require either:

- a consolidated export added to the Android app; or
- direct access to its `kraftlog.db` Room database and `reminder_prefs`.

After importing, use **Verlauf → Export** in Nextcloud. That full backup
contains exercises, routines, sessions and body-weight records.
