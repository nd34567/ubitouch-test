package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class Macro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val stepsJson: String // Serialized list of steps, e.g. [{"type": "SPEAK", "arg": "Engaging Silent Mode"}, {"type": "DELAY", "arg": "500"}, {"type": "HOME"}]
)
