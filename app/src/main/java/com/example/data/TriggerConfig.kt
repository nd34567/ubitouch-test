package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trigger_configs")
data class TriggerConfig(
    @PrimaryKey val id: String, // "LEFT", "RIGHT", "BOTTOM"
    val enabled: Boolean,
    val color: Int, // Hex ARGB color e.g. 0x8000FF00
    val sizeDp: Int = 12, // Width for Left/Right, Height for Bottom (Portrait)
    val heightPercent: Int = 60, // For Left/Right edge coverage (Portrait)
    val positionPercent: Int = 50, // Y position percent for Left/Right, X position for Bottom (Portrait)
    val opacityPercent: Int = 30, // Display visibility opacity (0-100)
    val landscapeSizeDp: Int = 12, // (Landscape)
    val landscapeHeightPercent: Int = 60, // (Landscape)
    val landscapePositionPercent: Int = 50 // (Landscape)
)
