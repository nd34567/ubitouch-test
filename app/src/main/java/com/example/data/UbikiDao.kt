package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UbikiDao {

    // Trigger Queries
    @Query("SELECT * FROM trigger_configs")
    fun getAllTriggersFlow(): Flow<List<TriggerConfig>>

    @Query("SELECT * FROM trigger_configs")
    suspend fun getAllTriggers(): List<TriggerConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(config: TriggerConfig)

    // Gesture Mappings Queries
    @Query("SELECT * FROM gesture_actions")
    fun getAllGestureActionsFlow(): Flow<List<GestureAction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestureAction(action: GestureAction)

    @Update
    suspend fun updateGestureAction(action: GestureAction)

    @Delete
    suspend fun deleteGestureAction(action: GestureAction)

    @Query("DELETE FROM gesture_actions WHERE triggerId = :triggerId")
    suspend fun deleteGestureActionsForTrigger(triggerId: String)

    // Macro Queries
    @Query("SELECT * FROM macros")
    fun getAllMacrosFlow(): Flow<List<Macro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: Macro): Long

    @Update
    suspend fun updateMacro(macro: Macro)

    @Delete
    suspend fun deleteMacro(macro: Macro)
}
