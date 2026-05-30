package com.example.data

import kotlinx.coroutines.flow.Flow

class UbikiRepository(private val dao: UbikiDao) {
    val triggersFlow: Flow<List<TriggerConfig>> = dao.getAllTriggersFlow()
    val gestureActionsFlow: Flow<List<GestureAction>> = dao.getAllGestureActionsFlow()
    val macrosFlow: Flow<List<Macro>> = dao.getAllMacrosFlow()

    suspend fun getAllTriggers(): List<TriggerConfig> {
        return dao.getAllTriggers()
    }

    suspend fun insertTrigger(config: TriggerConfig) {
        dao.insertTrigger(config)
    }

    suspend fun insertGestureAction(action: GestureAction) {
        dao.insertGestureAction(action)
    }

    suspend fun updateGestureAction(action: GestureAction) {
        dao.updateGestureAction(action)
    }

    suspend fun deleteGestureAction(action: GestureAction) {
        dao.deleteGestureAction(action)
    }

    suspend fun insertMacro(macro: Macro): Long {
        return dao.insertMacro(macro)
    }

    suspend fun updateMacro(macro: Macro) {
        dao.updateMacro(macro)
    }

    suspend fun deleteMacro(macro: Macro) {
        dao.deleteMacro(macro)
    }
}
