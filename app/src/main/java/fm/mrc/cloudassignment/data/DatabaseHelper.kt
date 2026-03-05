package fm.mrc.cloudassignment.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mood_tracker.db"
        const val DATABASE_VERSION = 2
        
        // Tables
        private const val TABLE_MOOD_ENTRIES = "mood_entries"
        private const val TABLE_CLIENT_ACTIVITIES = "client_activities"
        private const val TABLE_USERS = "users"
        
        // Columns for users
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"
        
        // Columns for mood_entries
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID_FK = "user_id_fk"
        private const val COLUMN_MOOD_SCORE = "mood_score"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_HAS_PHOTO = "has_photo"
        private const val COLUMN_HAS_VOICE = "has_voice"
        private const val COLUMN_PHOTO_PATH = "photo_path"
        private const val COLUMN_VOICE_PATH = "voice_path"
        
        // Columns for client_activities
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_ACTIVITY_TYPE = "activity_type"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create mood entries table
        val createMoodTable = """
            CREATE TABLE $TABLE_MOOD_ENTRIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID_FK TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_MOOD_SCORE REAL NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_HAS_PHOTO INTEGER DEFAULT 0,
                $COLUMN_HAS_VOICE INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        // Create client activities table
        val createActivitiesTable = """
            CREATE TABLE $TABLE_CLIENT_ACTIVITIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_ACTIVITY_TYPE TEXT NOT NULL,
                $COLUMN_MOOD_SCORE REAL,
                $COLUMN_NOTES TEXT,
                $COLUMN_HAS_PHOTO INTEGER DEFAULT 0,
                $COLUMN_HAS_VOICE INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createMoodTable)
        db.execSQL(createActivitiesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add the new columns to the existing table instead of recreating it
            db.execSQL("ALTER TABLE $TABLE_MOOD_ENTRIES ADD COLUMN $COLUMN_HAS_PHOTO INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_MOOD_ENTRIES ADD COLUMN $COLUMN_HAS_VOICE INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_MOOD_ENTRIES ADD COLUMN $COLUMN_PHOTO_PATH TEXT")
            db.execSQL("ALTER TABLE $TABLE_MOOD_ENTRIES ADD COLUMN $COLUMN_VOICE_PATH TEXT")
        }
    }

    fun addUser(username: String, password: String, email: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password) // In a real app, this should be hashed
            put(COLUMN_EMAIL, email)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun addMoodEntry(userId: Long, moodScore: Float, notes: String?, hasPhoto: Boolean = false, hasVoice: Boolean = false, photoPath: String? = null, voicePath: String? = null): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID_FK, userId)
            put(COLUMN_MOOD_SCORE, moodScore)
            put(COLUMN_NOTES, notes)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_HAS_PHOTO, if (hasPhoto) 1 else 0)
            put(COLUMN_HAS_VOICE, if (hasVoice) 1 else 0)
            put(COLUMN_PHOTO_PATH, photoPath)
            put(COLUMN_VOICE_PATH, voicePath)
        }
        return db.insert(TABLE_MOOD_ENTRIES, null, values)
    }

    fun getMoodEntries(userId: Long, startTime: Long, endTime: Long): List<MoodEntry> {
        val entries = mutableListOf<MoodEntry>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MOOD_ENTRIES,
            arrayOf(COLUMN_ID, COLUMN_MOOD_SCORE, COLUMN_NOTES, COLUMN_TIMESTAMP, COLUMN_HAS_PHOTO, COLUMN_HAS_VOICE, COLUMN_PHOTO_PATH, COLUMN_VOICE_PATH),
            "$COLUMN_USER_ID_FK = ? AND $COLUMN_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(userId.toString(), startTime.toString(), endTime.toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                entries.add(
                    MoodEntry(
                        id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                        moodScore = getFloat(getColumnIndexOrThrow(COLUMN_MOOD_SCORE)),
                        notes = getString(getColumnIndexOrThrow(COLUMN_NOTES)),
                        timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        hasPhoto = getInt(getColumnIndexOrThrow(COLUMN_HAS_PHOTO)) == 1,
                        hasVoice = getInt(getColumnIndexOrThrow(COLUMN_HAS_VOICE)) == 1,
                        photoPath = getString(getColumnIndexOrThrow(COLUMN_PHOTO_PATH)),
                        voicePath = getString(getColumnIndexOrThrow(COLUMN_VOICE_PATH))
                    )
                )
            }
        }
        cursor.close()
        return entries
    }

    fun validateUser(username: String, password: String): Long? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    // Add a new client activity
    fun addClientActivity(activity: ClientActivity) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, activity.userId)
            put(COLUMN_USER_NAME, activity.userName)
            put(COLUMN_EMAIL, activity.email)
            put(COLUMN_TIMESTAMP, activity.date.time)
            put(COLUMN_ACTIVITY_TYPE, activity.activityType.name)
            put(COLUMN_MOOD_SCORE, activity.mood)
            put(COLUMN_NOTES, activity.notes)
            put(COLUMN_HAS_PHOTO, if (activity.hasPhoto) 1 else 0)
            put(COLUMN_HAS_VOICE, if (activity.hasVoice) 1 else 0)
        }
        db.insert(TABLE_CLIENT_ACTIVITIES, null, values)
        db.close()
    }
    
    // Get statistics for a specific client
    fun getClientStats(userId: String): ClientStats {
        val db = this.readableDatabase
        
        // Get user info from the most recent activity
        val userInfoQuery = """
            SELECT $COLUMN_USER_NAME, $COLUMN_EMAIL 
            FROM $TABLE_CLIENT_ACTIVITIES 
            WHERE $COLUMN_USER_ID = ? 
            ORDER BY $COLUMN_TIMESTAMP DESC 
            LIMIT 1
        """.trimIndent()
        
        val userCursor = db.rawQuery(userInfoQuery, arrayOf(userId))
        var userName = ""
        var email = ""
        userCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                userName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME))
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            }
        }
        
        // Get activity counts
        val countQuery = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN $COLUMN_ACTIVITY_TYPE = '${ActivityType.MOOD_UPDATE}' THEN 1 ELSE 0 END) as mood_updates,
                SUM(CASE WHEN $COLUMN_ACTIVITY_TYPE = '${ActivityType.CHAT_SESSION}' THEN 1 ELSE 0 END) as chat_sessions,
                SUM(CASE WHEN $COLUMN_ACTIVITY_TYPE = '${ActivityType.REPORT_VIEW}' THEN 1 ELSE 0 END) as report_views,
                SUM(CASE WHEN $COLUMN_HAS_PHOTO = 1 THEN 1 ELSE 0 END) as photo_uploads,
                SUM(CASE WHEN $COLUMN_HAS_VOICE = 1 THEN 1 ELSE 0 END) as voice_recordings,
                AVG(CASE WHEN $COLUMN_ACTIVITY_TYPE = '${ActivityType.MOOD_UPDATE}' THEN $COLUMN_MOOD_SCORE ELSE NULL END) as avg_mood,
                MAX($COLUMN_TIMESTAMP) as last_active
            FROM $TABLE_CLIENT_ACTIVITIES
            WHERE $COLUMN_USER_ID = ?
        """.trimIndent()
        
        val cursor = db.rawQuery(countQuery, arrayOf(userId))
        
        return cursor.use { c ->
            if (c.moveToFirst()) {
                ClientStats(
                    userId = userId,
                    userName = userName,
                    email = email,
                    totalActivities = c.getInt(c.getColumnIndexOrThrow("total")),
                    moodUpdates = c.getInt(c.getColumnIndexOrThrow("mood_updates")),
                    chatSessions = c.getInt(c.getColumnIndexOrThrow("chat_sessions")),
                    reportViews = c.getInt(c.getColumnIndexOrThrow("report_views")),
                    photoUploads = c.getInt(c.getColumnIndexOrThrow("photo_uploads")),
                    voiceRecordings = c.getInt(c.getColumnIndexOrThrow("voice_recordings")),
                    averageMood = c.getFloat(c.getColumnIndexOrThrow("avg_mood")),
                    lastActive = Date(c.getLong(c.getColumnIndexOrThrow("last_active")))
                )
            } else {
                ClientStats(
                    userId = userId,
                    userName = userName,
                    email = email,
                    totalActivities = 0,
                    moodUpdates = 0,
                    chatSessions = 0,
                    reportViews = 0,
                    photoUploads = 0,
                    voiceRecordings = 0,
                    averageMood = 0f,
                    lastActive = Date()
                )
            }
        }
    }
    
    // Get all clients with their activity statistics
    fun getAllClientsStats(): List<ClientStats> {
        val db = this.readableDatabase
        
        // First get all unique user IDs
        val userIdsQuery = """
            SELECT DISTINCT $COLUMN_USER_ID 
            FROM $TABLE_CLIENT_ACTIVITIES
        """.trimIndent()
        
        val userIds = mutableListOf<String>()
        db.rawQuery(userIdsQuery, null).use { cursor ->
            while (cursor.moveToNext()) {
                userIds.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)))
            }
        }
        
        // Get stats for each user
        return userIds.map { userId ->
            getClientStats(userId)
        }
    }
    
    // Track a new mood entry with activity
    fun addMoodEntryWithActivity(userId: Long, userName: String, email: String, mood: Float, notes: String = "", hasPhoto: Boolean = false, hasVoice: Boolean = false) {
        // Add mood entry
        addMoodEntry(userId, mood, notes, hasPhoto, hasVoice)
        
        // Track activity
        addClientActivity(
            ClientActivity(
                userId = userId.toString(),
                userName = userName,
                email = email,
                date = Date(),
                activityType = ActivityType.MOOD_UPDATE,
                hasPhoto = hasPhoto,
                hasVoice = hasVoice,
                mood = mood,
                notes = notes
            )
        )
    }
}

data class MoodEntry(
    val id: Long,
    val moodScore: Float,
    val notes: String?,
    val timestamp: Long,
    val hasPhoto: Boolean = false,
    val hasVoice: Boolean = false,
    val photoPath: String? = null,
    val voicePath: String? = null
) 