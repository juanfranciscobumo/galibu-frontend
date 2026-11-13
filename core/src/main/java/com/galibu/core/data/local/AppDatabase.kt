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
    version = 21,
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

        /** v17 -> v18: trabajos gratuitos de cortesía restantes para profesionales. */
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN freeAcceptancesRemaining INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v18 -> v19: suscripción Pro del profesional.
         *
         * La tabla se reconstruye (no basta con ALTER) porque la versión 18
         * conserva la columna legada `freeAcceptancesRemaining` y Room rechaza
         * cualquier columna que no esté declarada en el entity. Recrear la
         * tabla deja el esquema idéntico al que crea una instalación nueva.
         */
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `user_profiles_new`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_profiles_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `isVerified` INTEGER NOT NULL,
                        `rating` REAL NOT NULL,
                        `completedJobs` INTEGER NOT NULL,
                        `balance` REAL NOT NULL,
                        `subscriptionPlan` TEXT NOT NULL,
                        `subscriptionStatus` TEXT NOT NULL,
                        `subscriptionExpiresAt` INTEGER,
                        `documentType` TEXT,
                        `documentPhotoSimulatedUrl` TEXT,
                        `workCategory` TEXT,
                        `verificationStatus` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `profilePhotoUrl` TEXT,
                        `department` TEXT,
                        `municipality` TEXT,
                        `address` TEXT,
                        `workPhotos` TEXT,
                        `workDescription` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `hourlyRate` REAL,
                        `acceptedTerms` INTEGER NOT NULL,
                        `authProvider` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `user_profiles_new` (
                        `id`, `name`, `email`, `phone`, `role`, `isVerified`, `rating`,
                        `completedJobs`, `balance`, `subscriptionPlan`, `subscriptionStatus`,
                        `subscriptionExpiresAt`, `documentType`, `documentPhotoSimulatedUrl`,
                        `workCategory`, `verificationStatus`, `password`, `profilePhotoUrl`,
                        `department`, `municipality`, `address`, `workPhotos`, `workDescription`,
                        `createdAt`, `hourlyRate`, `acceptedTerms`, `authProvider`
                    )
                    SELECT
                        `id`, `name`, `email`, `phone`, `role`, `isVerified`, `rating`,
                        `completedJobs`, `balance`, 'FREE', 'NONE', NULL, `documentType`,
                        `documentPhotoSimulatedUrl`, `workCategory`, `verificationStatus`,
                        `password`, `profilePhotoUrl`, `department`, `municipality`, `address`,
                        `workPhotos`, `workDescription`, `createdAt`, `hourlyRate`,
                        `acceptedTerms`, `authProvider`
                    FROM `user_profiles`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `user_profiles`")
                db.execSQL("ALTER TABLE `user_profiles_new` RENAME TO `user_profiles`")
            }
        }

        /**
         * v19 -> v20: detalle de la cotizacion del profesional (Fase 2b).
         * Todos los campos tienen default, asi que la tabla existente se migra
         * sin perder ofertas ya guardadas.
         */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bids ADD COLUMN quoteType TEXT NOT NULL DEFAULT 'ESTIMATED'")
                db.execSQL("ALTER TABLE bids ADD COLUMN visitRequired INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bids ADD COLUMN visitFee REAL")
                db.execSQL("ALTER TABLE bids ADD COLUMN materialsIncluded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bids ADD COLUMN materialsCost REAL")
                db.execSQL("ALTER TABLE bids ADD COLUMN counterRound INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v20 -> v21: fecha/hora agendada de la visita tecnica (nullable).
         */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bids ADD COLUMN visitScheduledAt INTEGER")
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
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

