package com.lernspace.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lernspace.app.data.local.dao.*
import com.lernspace.app.data.local.entity.*

@Database(
    entities = [
        SubjectEntity::class,
        TopicEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        StudySessionEntity::class,
        ReminderEntity::class,
        AvailabilityWindowEntity::class,
        NoteEntity::class,
        UserSettingsEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        MimoSimulationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LernspaceDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun mimoSimulationDao(): MimoSimulationDao

    companion object {
        @Volatile
        private var INSTANCE: LernspaceDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcard_decks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `subjectId` TEXT,
                        `subjectName` TEXT NOT NULL,
                        `topicId` TEXT,
                        `topicName` TEXT,
                        `sourceNoteId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_subjectId` ON `flashcard_decks` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_topicId` ON `flashcard_decks` (`topicId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_sourceNoteId` ON `flashcard_decks` (`sourceNoteId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` TEXT NOT NULL,
                        `deckId` TEXT NOT NULL,
                        `front` TEXT NOT NULL,
                        `back` TEXT NOT NULL,
                        `hint` TEXT NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        `nextReviewAt` INTEGER NOT NULL,
                        `lastReviewedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`deckId`) REFERENCES `flashcard_decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deckId` ON `flashcards` (`deckId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_nextReviewAt` ON `flashcards` (`nextReviewAt`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimo_simulations` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `conceptTopic` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `htmlContent` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isCommunityOrigin` INTEGER NOT NULL DEFAULT 0,
                        `authorName` TEXT NOT NULL DEFAULT 'Mimo AI',
                        `likesCount` INTEGER NOT NULL DEFAULT 0,
                        `viewsCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mimo_simulations_subject` ON `mimo_simulations` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mimo_simulations_createdAt` ON `mimo_simulations` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mimo_simulations_isFavorite` ON `mimo_simulations` (`isFavorite`)")
            }
        }

        fun getInstance(context: Context): LernspaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LernspaceDatabase::class.java,
                    "lernspace_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Safely closes the active database and nulls out the singleton instance.
         * Essential when restoring database files from backup so a fresh connection is created.
         */
        fun closeAndReset() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {}
                INSTANCE = null
            }
        }
    }
}

