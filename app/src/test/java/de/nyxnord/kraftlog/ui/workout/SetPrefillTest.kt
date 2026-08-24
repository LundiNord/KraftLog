package de.nyxnord.kraftlog.ui.workout

import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prefill and validation rules behind the active workout's set rows.
 *
 * These were pasted three times over in the view model — the new-session path, the
 * restore path and the add-exercise path could drift apart silently. The tests pin the
 * shared behaviour at the extracted home.
 */
class SetPrefillTest {

    private fun set(
        setNumber: Int,
        reps: Int = 10,
        weightKg: Float = 60f,
        isBodyweight: Boolean = false,
    ) = WorkoutSet(
        id = 0, sessionId = 1, exerciseId = 1, exerciseName = "Test",
        setNumber = setNumber, reps = reps, weightKg = weightKg, isBodyweight = isBodyweight,
    )

    // ── parsePerSetList ──────────────────────────────────────────────────────

    @Test
    fun `a per-set list splits on commas`() {
        assertEquals(listOf("60", "65", "70"), SetPrefill.parsePerSetList("60,65,70"))
    }

    @Test
    fun `blank entries are dropped rather than becoming zero targets`() {
        // "60,,70" used to produce a hole that getOrElse silently filled from the wrong
        // place; dropping keeps index arithmetic honest.
        assertEquals(listOf("60", "70"), SetPrefill.parsePerSetList("60,,70"))
    }

    @Test
    fun `whitespace around values is trimmed`() {
        assertEquals(listOf("60", "65"), SetPrefill.parsePerSetList(" 60 , 65 "))
    }

    @Test
    fun `an empty list yields nothing`() {
        assertTrue(SetPrefill.parsePerSetList("").isEmpty())
        assertTrue(SetPrefill.parsePerSetList(",").isEmpty())
    }

    // ── prefill ──────────────────────────────────────────────────────────────

    @Test
    fun `the previous session's logged value wins over routine targets`() {
        val lastSets = listOf(set(setNumber = 2, reps = 8, weightKg = 62.5f))
        val result = SetPrefill.prefill(
            setNumber = 2, lastSets = lastSets,
            pick = { it.reps.toString() },
            perSetList = listOf("12", "10"), parse = { it.toIntOrNull() },
            singleTarget = 10,
        )
        assertEquals("8", result)
    }

    @Test
    fun `without history the per-set target at this index is used`() {
        val result = SetPrefill.prefill(
            setNumber = 2, lastSets = emptyList(),
            pick = { it.reps.toString() },
            perSetList = listOf("12", "10"), parse = { it.toIntOrNull() },
            singleTarget = 10,
        )
        assertEquals(10, result)
    }

    @Test
    fun `beyond the per-set list the single target applies`() {
        val result = SetPrefill.prefill(
            setNumber = 4, lastSets = emptyList(),
            pick = { it.reps.toString() },
            perSetList = listOf("12", "10"), parse = { it.toIntOrNull() },
            singleTarget = 10,
        )
        assertEquals(10, result)
    }

    @Test
    fun `nothing to go on yields null so the field can start empty`() {
        val result: Int? = SetPrefill.prefill(
            setNumber = 1, lastSets = emptyList(),
            pick = { it.reps },
            perSetList = emptyList(), parse = { it.toIntOrNull() },
            singleTarget = null,
        )
        assertNull(result)
    }

    // ── formatWeight ─────────────────────────────────────────────────────────

    @Test
    fun `whole kilograms render without a decimal tail`() {
        assertEquals("70", SetPrefill.formatWeight(70f))
        assertEquals("0", SetPrefill.formatWeight(0f))
    }

    @Test
    fun `fractional weights keep their decimals`() {
        assertEquals("62.5", SetPrefill.formatWeight(62.5f))
    }

    // ── isValidLog ───────────────────────────────────────────────────────────

    @Test
    fun `a plain valid set passes`() {
        assertTrue(SetPrefill.isValidLog("10", "60", isBodyweight = false))
    }

    @Test
    fun `empty reps are refused`() {
        assertFalse(SetPrefill.isValidLog("", "60", isBodyweight = false))
    }

    @Test
    fun `non-numeric reps are refused`() {
        assertFalse(SetPrefill.isValidLog("twelve", "60", isBodyweight = false))
    }

    @Test
    fun `zero or negative reps are refused`() {
        assertFalse(SetPrefill.isValidLog("0", "60", isBodyweight = false))
        assertFalse(SetPrefill.isValidLog("-3", "60", isBodyweight = false))
    }

    @Test
    fun `an empty weight on a weighted set is refused`() {
        assertFalse(SetPrefill.isValidLog("10", "", isBodyweight = false))
    }

    @Test
    fun `a bodyweight set needs no weight`() {
        assertTrue(SetPrefill.isValidLog("10", "", isBodyweight = true))
    }

    @Test
    fun `negative weight is refused even for bodyweight sets`() {
        // Bodyweight ignores the field, but a typed negative still means a typo.
        assertTrue(SetPrefill.isValidLog("10", "-5", isBodyweight = true))
        assertFalse(SetPrefill.isValidLog("10", "-5", isBodyweight = false))
    }
}
