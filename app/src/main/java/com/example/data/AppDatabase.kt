package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TriggerConfig::class, GestureAction::class, Macro::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ubikiDao(): UbikiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ubikitouch_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.ubikiDao())
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.ubikiDao())
                }
            }
        }

        private suspend fun populateDatabase(dao: UbikiDao) {
            // Seeding default triggers with ARGB alpha, e.g. 0x60000000 | rgb
            // Left trigger (neon cyan tint)
            dao.insertTrigger(TriggerConfig("LEFT", true, 0x6000E5FF, 16, 50, 50, 60, 16, 50, 50))
            // Right trigger (neon coral pink tint)
            dao.insertTrigger(TriggerConfig("RIGHT", true, 0x60FF1744, 16, 50, 50, 60, 16, 50, 50))
            // Bottom trigger (neon violet tint)
            dao.insertTrigger(TriggerConfig("BOTTOM", true, 0x607C4DFF, 16, 70, 50, 60, 16, 70, 50))

            // Seeding default gesture action mappings
            dao.insertGestureAction(GestureAction(triggerId = "LEFT", gestureType = "SWIPE_IN", actionType = "BACK"))
            dao.insertGestureAction(GestureAction(triggerId = "LEFT", gestureType = "DOUBLE_TAP", actionType = "NOTIFICATIONS"))
            dao.insertGestureAction(GestureAction(triggerId = "LEFT", gestureType = "LONG_PRESS", actionType = "RECENTS"))
            dao.insertGestureAction(GestureAction(triggerId = "LEFT", gestureType = "SWIPE_UP", actionType = "NONE"))
            dao.insertGestureAction(GestureAction(triggerId = "LEFT", gestureType = "SWIPE_DOWN", actionType = "NONE"))

            dao.insertGestureAction(GestureAction(triggerId = "RIGHT", gestureType = "SWIPE_IN", actionType = "BACK"))
            dao.insertGestureAction(GestureAction(triggerId = "RIGHT", gestureType = "DOUBLE_TAP", actionType = "RECENTS"))
            dao.insertGestureAction(GestureAction(triggerId = "RIGHT", gestureType = "LONG_PRESS", actionType = "HOME"))
            dao.insertGestureAction(GestureAction(triggerId = "RIGHT", gestureType = "SWIPE_UP", actionType = "NONE"))
            dao.insertGestureAction(GestureAction(triggerId = "RIGHT", gestureType = "SWIPE_DOWN", actionType = "NONE"))

            dao.insertGestureAction(GestureAction(triggerId = "BOTTOM", gestureType = "SWIPE_IN", actionType = "HOME"))
            dao.insertGestureAction(GestureAction(triggerId = "BOTTOM", gestureType = "DOUBLE_TAP", actionType = "FLASHLIGHT"))
            dao.insertGestureAction(GestureAction(triggerId = "BOTTOM", gestureType = "LONG_PRESS", actionType = "SCREENSHOT"))
            dao.insertGestureAction(GestureAction(triggerId = "BOTTOM", gestureType = "SWIPE_UP", actionType = "NONE"))
            dao.insertGestureAction(GestureAction(triggerId = "BOTTOM", gestureType = "SWIPE_DOWN", actionType = "NONE"))

            // Seeding sample Macros
            dao.insertMacro(Macro(
                name = "Silent Meditate",
                stepsJson = """[{"type":"SYS_HOME","arg":""},{"type":"SPEAK_TEXT","arg":"Meditate loaded"}]"""
            ))
            dao.insertMacro(Macro(
                name = "Quick Double Back",
                stepsJson = """[{"type":"SYS_BACK","arg":""},{"type":"DELAY","arg":"300"},{"type":"SYS_BACK","arg":""}]"""
            ))
        }
    }
}
