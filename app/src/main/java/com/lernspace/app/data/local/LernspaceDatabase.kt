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
        FlashcardEntity::class
    ],
    version = 3,
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

        fun getInstance(context: Context): LernspaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LernspaceDatabase::class.java,
                    "lernspace_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

