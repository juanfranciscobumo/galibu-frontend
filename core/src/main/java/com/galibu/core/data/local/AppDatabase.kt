package com.galibu.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.galibu.core.data.model.*

@Database(
    entities = [
        UserProfile::class,
        JobRequest::class,
        Bid::class,
        ChatMessage::class,
        AppNotification::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun jobRequestDao(): JobRequestDao
    abstract fun bidDao(): BidDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v12 -> v13: solicitud relacionada en notificaciones (nullable, sin pérdida). */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_notifications ADD COLUMN jobId TEXT")
            }
        }

        /** v13 -> v14: origen de la solicitud (PUBLISH/DIRECTORY, nullable). */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_requests ADD COLUMN source TEXT")
            }
        }

        /** v14 -> v15: descripción del portafolio del profesional (nullable). */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN workDescription TEXT")
            }
        }

        /** v15 -> v16: fotos adjuntas a la solicitud (nullable). */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_requests ADD COLUMN photos TEXT")
            }
        }

        /** v16 -> v17: proveedor de autenticación para saber si la cuenta es Google. */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profiles ADD COLUMN authProvider TEXT NOT NULL DEFAULT 'LOCAL'"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ofreceya_database"
                )
                .addMigrations(
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

