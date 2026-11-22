package com.lernspace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lernspace.app.data.local.entity.MimoSimulationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MimoSimulationDao {

    @Query("SELECT * FROM mimo_simulations ORDER BY createdAt DESC")
    fun getAllSimulations(): Flow<List<MimoSimulationEntity>>

    @Query("SELECT * FROM mimo_simulations ORDER BY createdAt DESC")
    suspend fun getAllSimulationsList(): List<MimoSimulationEntity>

    @Query("SELECT * FROM mimo_simulations WHERE subject = :subject ORDER BY createdAt DESC")
    fun getSimulationsBySubject(subject: String): Flow<List<MimoSimulationEntity>>

    @Query("SELECT * FROM mimo_simulations WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSimulations(): Flow<List<MimoSimulationEntity>>

    @Query("SELECT * FROM mimo_simulations WHERE id = :id LIMIT 1")
    suspend fun getSimulationById(id: String): MimoSimulationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: MimoSimulationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulations(simulations: List<MimoSimulationEntity>)

    @Query("UPDATE mimo_simulations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM mimo_simulations WHERE id = :id")
    suspend fun deleteSimulationById(id: String)

    @Delete
    suspend fun deleteSimulation(simulation: MimoSimulationEntity)
}
