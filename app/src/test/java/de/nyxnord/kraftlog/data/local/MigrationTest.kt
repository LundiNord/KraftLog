package de.nyxnord.kraftlog.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the hand-written migrations against real SQLite.
 *
 * Room's MigrationTestHelper needs an exported schema JSON for every historical version,
 * but only the current schema can be exported by the compiler. So the pre-migration
 * databases are created here with the exact DDL each migration expects — which is itself
 * part of what is under test: if someone changes a migration's assumptions about the old
 * layout, these creates stop matching and the tests fail loudly.
 *
 * Room requires a complete migration path from the database's version to the current one,
 * so every test registers the full chain. After migrating, opening through Room at
 * version 5 also runs the compiled entities' schema validation against what the
 * migrations actually produced.
 *
 * These tables carry the user's saved workouts; losing them is the one failure this app
 * cannot recover from by refetching.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    private val allMigrations = arrayOf(
        KraftLogDatabase.MIGRATION_1_2,
        KraftLogDatabase.MIGRATION_2_3,
        KraftLogDatabase.MIGRATION_3_4,
        KraftLogDatabase.MIGRATION_4_5,
    )

    @After
    fun teardown() {
        context.deleteDatabase(dbName)
    }

    /**
     * Opens a raw database at the given legacy version and applies [build].
     *
     * Uses the same FrameworkSQLiteOpenHelper machinery Room itself uses, so the file on
     * disk is byte-for-byte the kind of database Room later opens for migration. A raw
     * SQLiteDatabase writes a subtly different header/journal setup, which made Room's
     * post-migration index validation see an empty indices set.
     */
    private fun createLegacy(version: Int, build: (SupportSQLiteDatabase) -> Unit) {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        file.delete()   // a leftover from a previous run would carry stale state

        var seeded = false
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createLegacySchema(db, version)
                build(db)
                seeded = true
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(config)
        // Accessing the database triggers onCreate (schema + seed). Closing releases the
        // file for Room's later migration run.
        helper.writableDatabase.execSQL(
            "INSERT INTO routines (name, description, createdAt, lastUsedAt) VALUES ('open', '', 0, NULL)"
        )
        helper.writableDatabase.execSQL("DELETE FROM routines WHERE name = 'open'")
        helper.close()
    }

    /** Full legacy DDL for [version], including tables added by earlier migrations. */
    private fun createLegacySchema(db: SupportSQLiteDatabase, version: Int) {
        createCoreTables(db, version)
        if (version >= 4) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'STRENGTH'")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS running_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    distanceKm REAL NOT NULL,
                    durationSeconds INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bouldering_routes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    grade TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                )
            """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_running_entries_sessionId ON running_entries(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bouldering_routes_sessionId ON bouldering_routes(sessionId)")
        }
    }

    /** The tables all legacy versions had in common, before any migration added more. */
    private fun createCoreTables(db: SupportSQLiteDatabase, version: Int) {
        // Per-set columns were introduced by MIGRATION_1_2 / _2_3 via ALTER TABLE with a
        // DEFAULT. A default that the entity does not declare makes Room's validation
        // reject the migrated schema, so a legacy database at version >= 2 must carry the
        // columns *without* the default — exactly what the rebuilt table looks like.
        val perSetColumns = when {
            version >= 3 -> ", targetWeightsPerSet TEXT NOT NULL, targetRepsPerSet TEXT NOT NULL"
            version == 2 -> ", targetWeightsPerSet TEXT NOT NULL"
            else -> ""
        }
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                primaryMuscles TEXT NOT NULL,
                secondaryMuscles TEXT NOT NULL,
                instructions TEXT NOT NULL DEFAULT '',
                isCustom INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())
        // From version 2 on, MIGRATION_1_2's rebuild has already replaced the original
        // id-keyed table with this composite-PK layout — so a database created directly
        // at version >= 2 must use it from the start.
        // id column only exists at v1; from v2 on the rebuild uses a composite PK.
        val idColumn = if (version == 1) "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," else ""
        val compositePk = if (version == 1) "" else "PRIMARY KEY(routineId, exerciseId),"
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS routine_exercises (
                $idColumn
                routineId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                orderIndex INTEGER NOT NULL,
                targetSets INTEGER NOT NULL,
                targetReps INTEGER NOT NULL,
                targetWeightKg REAL,
                restSeconds INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT ''$perSetColumns,
                $compositePk
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER,
                name TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                setNumber INTEGER NOT NULL,
                reps INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                isBodyweight INTEGER NOT NULL DEFAULT 0,
                rpe REAL,
                loggedAt INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        // Room generates an index for every FK column; original installs had them too.
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineId ON routine_exercises(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_routineId ON workout_sessions(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_sessionId ON workout_sets(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_exerciseId ON workout_sets(exerciseId)")
    }

    /** Applies migrations in order, then validates via Room at version 5. */
    private fun migrate(): KraftLogDatabase =
        Room.databaseBuilder(context, KraftLogDatabase::class.java, dbName)
            .addMigrations(*allMigrations)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `migrate 1 to 2 adds targetWeightsPerSet preserving rows`() {
        val file = context.getDatabasePath(dbName)
        createLegacy(1) { db ->
            db.execSQL("""
                INSERT INTO routine_exercises
                    (routineId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds, notes)
                VALUES (1, 1, 0, 3, 10, 60.0, 90, 'keep me')
            """.trimIndent())
        }
        migrate().withDb { db ->
            db.openHelper.writableDatabase
                .query("SELECT notes, targetWeightsPerSet FROM routine_exercises").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("keep me", cursor.getString(0))
                    assertEquals("", cursor.getString(1))   // new column starts empty
                }
        }
    }

    @Test
    fun `migrate 2 to 3 adds targetRepsPerSet without touching existing data`() {
        createLegacy(2) { db ->
            // targetWeightsPerSet is already present — createLegacy(2) applied the
            // version-1 migration's effect. Just seed a row that uses it.
            db.execSQL("""
                INSERT INTO routine_exercises
                    (routineId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg,
                     restSeconds, notes, targetWeightsPerSet)
                VALUES (1, 1, 0, 4, 12, 55.5, 60, '', '50,55,60')
            """.trimIndent())
        }

        migrate().withDb { db ->
            db.openHelper.writableDatabase
                .query("SELECT targetWeightsPerSet, targetRepsPerSet FROM routine_exercises").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("50,55,60", cursor.getString(0))
                    assertEquals("", cursor.getString(1))
                }
        }
    }

    @Test
    fun `migrate 3 to 4 adds sessionType and both workout tables`() {
        createLegacy(3) { db ->
            db.execSQL("""
                INSERT INTO workout_sessions (routineId, name, startedAt, finishedAt, notes)
                VALUES (NULL, 'Legacy', 1000, NULL, '')
            """.trimIndent())
        }

        migrate().withDb { db ->
            val sql = db.openHelper.writableDatabase
            sql.query("SELECT sessionType FROM workout_sessions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("STRENGTH", cursor.getString(0))
            }
            sql.execSQL("INSERT INTO running_entries (sessionId, distanceKm, durationSeconds) VALUES (1, 5.2, 1800)")
            sql.execSQL("INSERT INTO bouldering_routes (sessionId, grade, isCompleted) VALUES (1, '6A', 1)")
            sql.query("SELECT COUNT(*) FROM running_entries").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            sql.query("SELECT COUNT(*) FROM bouldering_routes").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }

    @Test
    fun `migrate 4 to 5 creates body_weight_entries usable`() {
        createLegacy(4) { /* version 4 already carries everything except body weights */ }

        migrate().withDb { db ->
            val sql = db.openHelper.writableDatabase
            sql.execSQL("INSERT INTO body_weight_entries (date, weightKg) VALUES (1700000000000, 82.5)")
            sql.query("SELECT date, weightKg FROM body_weight_entries").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1700000000000L, cursor.getLong(0))
                assertEquals(82.5f, cursor.getFloat(1))
            }
        }
    }

    @Test
    fun `the full chain 1 to 5 keeps data intact end to end`() {
        createLegacy(1) { db ->
            db.execSQL("""
                INSERT INTO routine_exercises
                    (routineId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds, notes)
                VALUES (1, 1, 0, 3, 8, 100.0, 120, 'survives all five versions')
            """.trimIndent())
        }

        migrate().withDb { db ->
            db.openHelper.writableDatabase
                .query("""
                    SELECT notes, targetSets, targetWeightKg, targetWeightsPerSet, targetRepsPerSet
                    FROM routine_exercises
                """).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("survives all five versions", cursor.getString(0))
                    assertEquals(3, cursor.getInt(1))
                    assertEquals(100.0f, cursor.getFloat(2))
                    assertEquals("", cursor.getString(3))
                    assertEquals("", cursor.getString(4))
                }
        }
    }
}

/** Room's database is Closeable; stdlib use() needs an explicit import-free helper here. */
private inline fun <T : androidx.room.RoomDatabase, R> T.withDb(block: (T) -> R): R =
    try { block(this) } finally { close() }
