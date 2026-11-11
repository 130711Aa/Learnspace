package com.lernspace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lernspace.app.data.local.entity.FlashcardDeckEntity
import com.lernspace.app.data.local.entity.FlashcardEntity

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_decks ORDER BY updatedAt DESC")
    suspend fun getAllDecks(): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    suspend fun getAllCards(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt ASC")
    suspend fun getCardsForDeck(deckId: String): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Query("DELETE FROM flashcard_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: String)

    @Query("DELETE FROM flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: String)
}
