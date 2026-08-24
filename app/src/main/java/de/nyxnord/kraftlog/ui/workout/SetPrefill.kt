package de.nyxnord.kraftlog.ui.workout

import de.nyxnord.kraftlog.data.local.entity.WorkoutSet

/**
 * Pure helpers behind the set prefill and formatting shown in the active workout.
 *
 * Extracted from [ActiveWorkoutViewModel], where the same three blocks of parsing and
 * formatting used to be pasted at every place a LiveSet was built — the new-session
 * path, the restore path and the add-exercise path could drift apart silently.
 */
object SetPrefill {

    /** Splits a comma-separated per-set list ("60,65,70") into trimmed, non-blank parts. */
    fun parsePerSetList(raw: String): List<String> =
        raw.split(",").filter { it.isNotBlank() }.map { it.trim() }

    /**
     * The value to prefill for set [setNumber] (1-based): the previous session's logged
     * value for that exact set number when there is one, otherwise the routine's per-set
     * target at that index, otherwise the single target for all sets.
     */
    fun <T> prefill(
        setNumber: Int,
        lastSets: List<WorkoutSet>,
        pick: (WorkoutSet) -> T,
        perSetList: List<String>,
        parse: (String) -> T?,
        singleTarget: T?,
    ): T? {
        lastSets.find { it.setNumber == setNumber }?.let { return pick(it) }
        return perSetList.getOrNull(setNumber - 1)?.let(parse) ?: singleTarget
    }

    /**
     * Renders a weight the way the text fields want it: whole kilograms without a
     * decimal tail ("70" not "70.0"), fractional weights as-is.
     */
    fun formatWeight(weightKg: Float): String =
        if (weightKg == weightKg.toLong().toFloat()) weightKg.toLong().toString()
        else weightKg.toString()

    /**
     * Whether a log attempt carries usable values. An empty or malformed field is not a
     * measurement; logging it as zero wrote fake rows into history and poisoned the
     * next workout's prefill. Bodyweight sets carry no weight by definition.
     */
    fun isValidLog(repsText: String, weightText: String, isBodyweight: Boolean): Boolean {
        val reps = repsText.toIntOrNull() ?: return false
        if (reps <= 0) return false
        if (isBodyweight) return true
        val weight = weightText.toFloatOrNull() ?: return false
        return weight >= 0f
    }
}
