package fm.mrc.cloudassignment.data

import java.util.*

data class ClientActivity(
    val userId: String,
    val userName: String,
    val email: String,
    val date: Date,
    val activityType: ActivityType,
    val hasPhoto: Boolean = false,
    val hasVoice: Boolean = false,
    val mood: Float,
    val notes: String = ""
)

enum class ActivityType {
    MOOD_UPDATE,
    CHAT_SESSION,
    REPORT_VIEW
}

data class ClientStats(
    val userId: String,
    val userName: String,
    val email: String,
    val totalActivities: Int,
    val moodUpdates: Int,
    val chatSessions: Int,
    val reportViews: Int,
    val photoUploads: Int,
    val voiceRecordings: Int,
    val averageMood: Float,
    val lastActive: Date
) 