package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.data.*
import kotlinx.coroutines.*
import java.util.Locale

class UbikiAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

    private var windowManager: WindowManager? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    // Maintain map of active overlays to remove them during updates
    private val activeViews = HashMap<String, View>()

    private var triggerConfigs: List<TriggerConfig> = emptyList()
    private var gestureActions: List<GestureAction> = emptyList()
    private var macroList: List<Macro> = emptyList()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        textToSpeech = TextToSpeech(this, this)
        Log.d("UbikiAccess", "Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("UbikiAccess", "Service connected! Listening to database changes")

        val database = AppDatabase.getDatabase(applicationContext, serviceScope)
        val dao = database.ubikiDao()

        // Combine DB flows and update overlays reactively
        serviceScope.launch {
            launch {
                dao.getAllTriggersFlow().collect { configs ->
                    triggerConfigs = configs
                    updatePhysicalOverlays()
                }
            }
            launch {
                dao.getAllGestureActionsFlow().collect { actions ->
                    gestureActions = actions
                }
            }
            launch {
                dao.getAllMacrosFlow().collect { macros ->
                    macroList = macros
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility service requires implementing this, but gesture capture occurs on overlay Views
    }

    override fun onInterrupt() {
        Log.d("UbikiAccess", "Service Interrupted")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (ttsReady && !text.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ubiki_tts")
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        removeAllOverlays()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        Log.d("UbikiAccess", "Service destroyed")
    }

    private fun removeAllOverlays() {
        activeViews.forEach { (_, view) ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e("UbikiAccess", "Error removing view", e)
            }
        }
        activeViews.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updatePhysicalOverlays() {
        removeAllOverlays()

        // Check if drawing overlay permission is active
        if (!Settings.canDrawOverlays(this)) {
            Log.w("UbikiAccess", "Cannot draw overlays, permission missing!")
            return
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        for (config in triggerConfigs) {
            if (!config.enabled) continue

            val overlayView = View(this).apply {
                // Determine display color
                val baseColor = config.color
                // Inject opacity alpha based on opacityPercent
                val alpha = (config.opacityPercent * 2.55).toInt().coerceIn(0, 255)
                val resolvedColor = Color.argb(
                    alpha,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                )
                setBackgroundColor(resolvedColor)
            }

            val isVertical = config.id == "LEFT" || config.id == "RIGHT"

            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val currentSizeDp = if (isLandscape) config.landscapeSizeDp else config.sizeDp
            val currentHeightPercent = if (isLandscape) config.landscapeHeightPercent else config.heightPercent
            val currentPositionPercent = if (isLandscape) config.landscapePositionPercent else config.positionPercent

            // Compute size parameters
            val triggerSize = (currentSizeDp * displayMetrics.density).toInt()
            val coverageLength = if (isVertical) {
                (screenHeight * (currentHeightPercent / 100f)).toInt()
            } else {
                (screenWidth * (currentHeightPercent / 100f)).toInt()
            }

            // Window manager layout params setup
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val gravity = when (config.id) {
                "LEFT" -> Gravity.START or Gravity.TOP
                "RIGHT" -> Gravity.END or Gravity.TOP
                "BOTTOM" -> Gravity.BOTTOM or Gravity.START
                else -> Gravity.START or Gravity.TOP
            }

            val w = if (isVertical) triggerSize else coverageLength
            val h = if (isVertical) coverageLength else triggerSize

            val params = WindowManager.LayoutParams(
                w,
                h,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
                // Position offsets
                if (isVertical) {
                    val centerYOffset = (screenHeight * (currentPositionPercent / 100f)).toInt() - (coverageLength / 2)
                    y = centerYOffset.coerceIn(0, screenHeight - coverageLength)
                    x = 0
                } else {
                    val centerXOffset = (screenWidth * (currentPositionPercent / 100f)).toInt() - (coverageLength / 2)
                    x = centerXOffset.coerceIn(0, screenWidth - coverageLength)
                    y = 0
                }
            }

            // Bind Touch Interpreter
            bindTouchGestureListener(overlayView, config)

            try {
                windowManager?.addView(overlayView, params)
                activeViews[config.id] = overlayView
            } catch (e: Exception) {
                Log.e("UbikiAccess", "Failed to add window overlay for ${config.id}", e)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("UbikiAccess", "Orientation configuration changed, refreshing physical overlays")
        updatePhysicalOverlays()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouchGestureListener(view: View, config: TriggerConfig) {
        var startX = 0f
        var startY = 0f
        var downTime = 0L
        var lastUpTime = 0L
        val swipeThreshold = 60f // Pixels

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    val currentTime = System.currentTimeMillis()
                    downTime = currentTime

                    // Double tap verification: consecutive taps within 320ms
                    if (currentTime - lastUpTime < 320) {
                        executeAction(config.id, "DOUBLE_TAP")
                        lastUpTime = 0L // reset
                        return@setOnTouchListener true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val currentTime = System.currentTimeMillis()
                    val duration = currentTime - downTime
                    val diffX = event.x - startX
                    val diffY = event.y - startY

                    lastUpTime = currentTime

                    if (duration > 500 && Math.abs(diffX) < swipeThreshold && Math.abs(diffY) < swipeThreshold) {
                        // Long press trigger
                        executeAction(config.id, "LONG_PRESS")
                    } else if (Math.abs(diffX) > swipeThreshold || Math.abs(diffY) > swipeThreshold) {
                        // Interpret direction
                        interpretSwipe(config.id, diffX, diffY, swipeThreshold)
                    } else {
                        // Standard Single Tap
                        // Wait active for double tap window, or execute tap if desired
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Only trigger tap if another touch down didn't invalidate it
                            if (lastUpTime == currentTime) {
                                executeAction(config.id, "TAP")
                            }
                        }, 320)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun interpretSwipe(triggerId: String, diffX: Float, diffY: Float, threshold: Float) {
        when (triggerId) {
            "LEFT" -> {
                if (diffX > threshold) {
                    executeAction(triggerId, "SWIPE_IN")
                } else if (diffY < -threshold) {
                    executeAction(triggerId, "SWIPE_UP")
                } else if (diffY > threshold) {
                    executeAction(triggerId, "SWIPE_DOWN")
                }
            }
            "RIGHT" -> {
                if (diffX < -threshold) {
                    executeAction(triggerId, "SWIPE_IN")
                } else if (diffY < -threshold) {
                    executeAction(triggerId, "SWIPE_UP")
                } else if (diffY > threshold) {
                    executeAction(triggerId, "SWIPE_DOWN")
                }
            }
            "BOTTOM" -> {
                if (diffY < -threshold) {
                    executeAction(triggerId, "SWIPE_IN") // Swipe upwards
                } else if (diffX < -threshold) {
                    executeAction(triggerId, "SWIPE_UP") // Swipe leftwards
                } else if (diffX > threshold) {
                    executeAction(triggerId, "SWIPE_DOWN") // Swipe rightwards
                }
            }
        }
    }

    private fun executeAction(triggerId: String, gestureType: String) {
        val mapping = gestureActions.firstOrNull { it.triggerId == triggerId && it.gestureType == gestureType }
        val actionType = mapping?.actionType ?: "NONE"
        val actionData = mapping?.actionData ?: ""

        if (actionType == "NONE") {
            Log.d("UbikiAccess", "Gesture unmapped: $triggerId -> $gestureType")
            return
        }

        Log.i("UbikiAccess", "Executing action: $actionType for $triggerId -> $gestureType")
        
        // Provide visual notification Toast in a safe UI manner
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Ubiki: Triggered $actionType", Toast.LENGTH_SHORT).show()
        }

        when (actionType) {
            "BACK" -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            "HOME" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            "RECENTS" -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
            "NOTIFICATIONS" -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            "FLASHLIGHT" -> {
                FlashlightManager.toggleFlashlight(this)
            }
            "SCREENSHOT" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else {
                    speak("Screenshot action is unsupported below Android Pie")
                }
            }
            "SPEAK_TEXT" -> {
                if (actionData.isNotEmpty()) {
                    speak(actionData)
                } else {
                    speak("Ubiki trigger activated")
                }
            }
            "RUN_MACRO" -> {
                val macroId = actionData.toIntOrNull() ?: return
                val macro = macroList.firstOrNull { it.id == macroId }
                if (macro != null) {
                    runMacroSequence(macro)
                }
            }
        }
    }

    private fun runMacroSequence(macro: Macro) {
        serviceScope.launch {
            // Very simple sequential parse of step lists
            // stepsJson form: [{"type":"SYS_HOME"},{"type":"DELAY","arg":"500"},{"type":"SPEAK_TEXT","arg":"Done"}]
            try {
                // Since we don't have Moshi fully reflection-linked in accessibility loop on distinct JVM background registers,
                // we'll implement a fast, robust direct token search string parser to ensure no parsing library conflicts
                val steps = parseMacroSteps(macro.stepsJson)
                for (step in steps) {
                    when (step.type) {
                        "SYS_BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
                        "SYS_HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
                        "SYS_RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                        "SYS_NOTIFICATION" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                        "TOGGLE_FLASH" -> FlashlightManager.toggleFlashlight(this@UbikiAccessibilityService)
                        "SPEAK_TEXT" -> speak(step.arg)
                        "DELAY" -> {
                            val time = step.arg.toLongOrNull() ?: 500L
                            delay(time)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UbikiAccess", "Failed running macro ${macro.name}", e)
            }
        }
    }

    private class MacroStep(val type: String, val arg: String)

    private fun parseMacroSteps(json: String): List<MacroStep> {
        val list = ArrayList<MacroStep>()
        // Very basic JSON tokenizer for string matching
        var index = 0
        while (index < json.length) {
            val typeIndex = json.indexOf("\"type\":", index)
            if (typeIndex == -1) break

            val startQuoteType = json.indexOf("\"", typeIndex + 7)
            if (startQuoteType == -1) break
            val endQuoteType = json.indexOf("\"", startQuoteType + 1)
            if (endQuoteType == -1) break
            val typeStr = json.substring(startQuoteType + 1, endQuoteType)

            // Parse optional action argument
            var argStr = ""
            val argIndex = json.indexOf("\"arg\":", endQuoteType)
            val nextObjectBrace = json.indexOf("}", endQuoteType)
            if (argIndex != -1 && argIndex < nextObjectBrace) {
                val startQuoteArg = json.indexOf("\"", argIndex + 6)
                if (startQuoteArg != -1) {
                    val endQuoteArg = json.indexOf("\"", startQuoteArg + 1)
                    if (endQuoteArg != -1) {
                        argStr = json.substring(startQuoteArg + 1, endQuoteArg)
                    }
                }
            }
            list.add(MacroStep(typeStr, argStr))
            index = nextObjectBrace + 1
        }
        return list
    }
}

object FlashlightManager {
    private var isFlashlightOn = false

    fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            Log.e("UbikiFlashlight", "Failed to toggle flashlight", e)
        }
    }
}
