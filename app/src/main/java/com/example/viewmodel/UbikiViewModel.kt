package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UbikiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = UbikiRepository(database.ubikiDao())

    // Live state streams from Room persistence
    val triggers: StateFlow<List<TriggerConfig>> = repository.triggersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val gestureActions: StateFlow<List<GestureAction>> = repository.gestureActionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val macros: StateFlow<List<Macro>> = repository.macrosFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Permission check live states
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    // Screen navigation state
    private val _currentTab = MutableStateFlow("TRIGGERS") // "TRIGGERS", "GESTURES", "MACROS", "SANDBOX"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Local sandbox simulation feedback states
    private val _simulationFeedback = MutableStateFlow<String?>(null)
    val simulationFeedback: StateFlow<String?> = _simulationFeedback.asStateFlow()

    init {
        checkPermissions()
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        _isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        _isOverlayEnabled.value = Settings.canDrawOverlays(context)
    }

    // Update individual Trigger parameters dynamically in DB
    fun updateTriggerConfig(config: TriggerConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTrigger(config)
        }
    }

    // Bind action to a gesture
    fun bindGestureAction(triggerId: String, gestureType: String, actionType: String, actionData: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            // Find existing action if any
            val existingList = database.ubikiDao().getAllTriggers() // dummy select
            // Actually find match from memory flow
            val activeList = gestureActions.value
            val match = activeList.firstOrNull { it.triggerId == triggerId && it.gestureType == gestureType }

            if (match != null) {
                val updated = match.copy(actionType = actionType, actionData = actionData)
                repository.updateGestureAction(updated)
            } else {
                val newAction = GestureAction(
                    triggerId = triggerId,
                    gestureType = gestureType,
                    actionType = actionType,
                    actionData = actionData
                )
                repository.insertGestureAction(newAction)
            }
        }
    }

    // Macro manipulations
    fun saveMacro(name: String, stepsJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMacro(Macro(name = name, stepsJson = stepsJson))
        }
    }

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMacro(macro)
        }
    }

    // Local Simulator gesture handler
    fun simulateGesture(triggerId: String, gestureType: String) {
        val mapping = gestureActions.value.firstOrNull { it.triggerId == triggerId && it.gestureType == gestureType }
        val actionType = mapping?.actionType ?: "NONE"
        val actionData = mapping?.actionData ?: ""

        if (actionType == "NONE") {
            _simulationFeedback.value = "Gesture '$gestureType' on $triggerId trigger is not mapped!"
        } else {
            val details = if (actionData.isNotEmpty()) " (Value: $actionData)" else ""
            _simulationFeedback.value = "🚀 Simulated Event Success!\n$triggerId Trigger: $gestureType detected.\nAction executed: $actionType$details"
        }
    }

    fun clearFeedback() {
        _simulationFeedback.value = null
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, "com.example.service.UbikiAccessibilityService")
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && (enabledService == expectedComponentName || enabledService.shortClassName == ".service.UbikiAccessibilityService")) {
                return true
            }
        }
        return false
    }
}
