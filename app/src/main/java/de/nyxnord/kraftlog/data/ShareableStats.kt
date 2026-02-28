package de.nyxnord.kraftlog.data

import org.json.JSONArray
import org.json.JSONObject

data class ExerciseShareRecord(
    val exerciseName: String,
    val maxWeightKg: Float?,
    val bestEstimated1RM: Float?,
    val maxRepsInSet: Int?,
    val totalSessions: Int,
    val totalVolumeKg: Float
)

data class SessionShareRecord(
    val name: String,
    val date: Long,
    val totalVolumeKg: Float,
    val exerciseNames: List<String>
)

data class ShareableStats(
    val displayName: String,
    val exerciseRecords: List<ExerciseShareRecord>,
    val recentSessions: List<SessionShareRecord>
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("displayName", displayName)

        val recordsArray = JSONArray()
        exerciseRecords.forEach { record ->
            val r = JSONObject()
            r.put("exerciseName", record.exerciseName)
            record.maxWeightKg?.let { r.put("maxWeightKg", it.toDouble()) }
            record.bestEstimated1RM?.let { r.put("bestEstimated1RM", it.toDouble()) }
            record.maxRepsInSet?.let { r.put("maxRepsInSet", it) }
            r.put("totalSessions", record.totalSessions)
            r.put("totalVolumeKg", record.totalVolumeKg.toDouble())
            recordsArray.put(r)
        }
        obj.put("exerciseRecords", recordsArray)

        val sessionsArray = JSONArray()
        recentSessions.forEach { session ->
            val s = JSONObject()
            s.put("name", session.name)
            s.put("date", session.date)
            s.put("totalVolumeKg", session.totalVolumeKg.toDouble())
            val namesArray = JSONArray()
            session.exerciseNames.forEach { namesArray.put(it) }
            s.put("exerciseNames", namesArray)
            sessionsArray.put(s)
        }
        obj.put("recentSessions", sessionsArray)

        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): ShareableStats {
            val obj = JSONObject(json)
            val displayName = obj.getString("displayName")

            val recordsArray = obj.getJSONArray("exerciseRecords")
            val exerciseRecords = (0 until recordsArray.length()).map { i ->
                val r = recordsArray.getJSONObject(i)
                ExerciseShareRecord(
                    exerciseName = r.getString("exerciseName"),
                    maxWeightKg = if (r.has("maxWeightKg")) r.getDouble("maxWeightKg").toFloat() else null,
                    bestEstimated1RM = if (r.has("bestEstimated1RM")) r.getDouble("bestEstimated1RM").toFloat() else null,
                    maxRepsInSet = if (r.has("maxRepsInSet")) r.getInt("maxRepsInSet") else null,
                    totalSessions = r.getInt("totalSessions"),
                    totalVolumeKg = r.getDouble("totalVolumeKg").toFloat()
                )
            }

            val sessionsArray = obj.getJSONArray("recentSessions")
            val recentSessions = (0 until sessionsArray.length()).map { i ->
                val s = sessionsArray.getJSONObject(i)
                val namesArray = s.getJSONArray("exerciseNames")
                SessionShareRecord(
                    name = s.getString("name"),
                    date = s.getLong("date"),
                    totalVolumeKg = s.getDouble("totalVolumeKg").toFloat(),
                    exerciseNames = (0 until namesArray.length()).map { j -> namesArray.getString(j) }
                )
            }

            return ShareableStats(displayName, exerciseRecords, recentSessions)
        }
    }
}
