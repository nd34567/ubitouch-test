package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_actions")
data class GestureAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerId: String, // "LEFT", "RIGHT", "BOTTOM"
    val gestureType: String, // "SWIPE_IN", "SWIPE_UP", "SWIPE_DOWN", "DOUBLE_TAP", "LONG_PRESS"
    val actionType: String, // "BACK", "HOME", "RECENTS", "NOTIFICATIONS", "FLASHLIGHT", "SCREENSHOT", "SPEAK_TEXT", "RUN_MACRO", "NONE"
    val actionData: String = "" // Hold custom package name, Text-to-Speech message, or Macro ID
)
