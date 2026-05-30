package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.UbikiViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// High Density Warm Theme Palette
val WarmBg = Color(0xFFFDF8F6)
val DarkText = Color(0xFF1D1B1E)
val MediumText = Color(0xFF49454F)
val BorderColor = Color(0xFFCAC4D0)

val BrandBrown = Color(0xFF7C533C)   // #7C533C
val WarmPeach = Color(0xFFFADCCB)    // #FADCCB
val PeachAccent = Color(0xFFFFDBCB)  // #FFDBCB
val SoftLavender = Color(0xFFE8DEF8) // #E8DEF8
val LavenderAccent = Color(0xFFD0BCFF)// #D0BCFF
val SoftSkyBlue = Color(0xFFC2E7FF)  // #C2E7FF
val DarkEspresso = Color(0xFF2B170B)  // #2B170B

// Custom Theme Colors for compatibility with original code
val NeonCyan = BrandBrown
val NeonRed = Color(0xFFB13824)
val NeonPurple = Color(0xFF74519A)
val NeonGreen = Color(0xFF1E6C44)
val ObsidianBg = WarmBg
val DarkCardBg = Color(0xFFFFFFFF)
val DarkCardBorder = BorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbikiTouchScreen(
    viewModel: UbikiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reactive database states
    val triggers by viewModel.triggers.collectAsStateWithLifecycle()
    val gestureActions by viewModel.gestureActions.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()

    // Permission and Tab states
    val isAccessEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.simulationFeedback.collectAsStateWithLifecycle()

    // Re-check permissions every time user resumes the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-clear simulated gesture feedback after 3.5s
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(3500)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            UbikiBottomBar(currentTab = currentTab, onTabSelected = { viewModel.setTab(it) })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .drawBehind {
                    // Soft warm decorative background spots
                    drawCircle(
                        color = LavenderAccent.copy(alpha = 0.06f),
                        radius = 350.dp.toPx(),
                        center = Offset(size.width * 0.15f, size.height * 0.25f)
                    )
                    drawCircle(
                        color = PeachAccent.copy(alpha = 0.06f),
                        radius = 400.dp.toPx(),
                        center = Offset(size.width * 0.85f, size.height * 0.75f)
                    )
                }
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with app icon and title
                HeaderSection()

                // Diagnostic / Permissions section
                PermissionHubSection(
                    context = context,
                    isAccessEnabled = isAccessEnabled,
                    isOverlayEnabled = isOverlayEnabled
                )

                // Simulated Event Banner feedback styled with Warm High Density values
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    feedbackMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BrandBrown, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SoftLavender),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Success",
                                    tint = BrandBrown,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = msg,
                                    color = DarkEspresso,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Main workspaces according to screen selection
                when (currentTab) {
                    "TRIGGERS" -> TriggerSetupWorkspace(
                        triggers = triggers,
                        onConfigChanged = { viewModel.updateTriggerConfig(it) }
                    )
                    "GESTURES" -> GestureMappingWorkspace(
                        triggers = triggers,
                        gestureActions = gestureActions,
                        macros = macros,
                        onActionChanged = { triggerId, gestureType, actionType, actionData ->
                            viewModel.bindGestureAction(triggerId, gestureType, actionType, actionData)
                        }
                    )
                    "MACROS" -> MacroStudioWorkspace(
                        macros = macros,
                        onSaveMacro = { name, steps -> viewModel.saveMacro(name, steps) },
                        onDeleteMacro = { viewModel.deleteMacro(it) }
                    )
                    "SANDBOX" -> InAppSandboxWorkspace(
                        triggers = triggers,
                        gestureActions = gestureActions,
                        macros = macros,
                        onGestureTriggered = { triggerId, gestureType ->
                            viewModel.simulateGesture(triggerId, gestureType)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SoftLavender, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = "UbikiTouch Gesture Logo",
                tint = DarkText,
                modifier = Modifier.size(22.dp)
            )
        }

        Column {
            Text(
                text = "UbikiTouch",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = DarkText
            )
            Text(
                text = "Studio & Automation Engine",
                fontSize = 12.sp,
                color = MediumText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PermissionHubSection(
    context: Context,
    isAccessEnabled: Boolean,
    isOverlayEnabled: Boolean
) {
    val isSystemFullyActive = isAccessEnabled && isOverlayEnabled

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Density Style - Warm peach card from design mockup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarmPeach),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Service Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkEspresso
                    )
                    Text(
                        text = if (isSystemFullyActive) "Active" else "Needs Setup",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkEspresso
                    )
                }

                // Animated toggle representing HTML custom switch
                val toggleOffsetInDp by animateDpAsState(
                    targetValue = if (isSystemFullyActive) 24.dp else 4.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )

                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 30.dp)
                        .background(if (isSystemFullyActive) BrandBrown else BorderColor, CircleShape)
                        .padding(3.dp)
                        .clickable {
                            if (!isAccessEnabled) {
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            } else if (!isOverlayEnabled) {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = toggleOffsetInDp)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Section header info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "REQUIRED PERMISSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MediumText,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Real System Integration",
                fontSize = 10.sp,
                color = BrandBrown,
                fontWeight = FontWeight.Bold
            )
        }

        PermissionStateRow(
            title = "Accessibility Engine",
            subtitle = "Enables system swipes & background overlay detection",
            icon = Icons.Outlined.SettingsAccessibility,
            isActive = isAccessEnabled,
            onGrantClick = {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    ToastUtils.show(context, "Could not open settings automatically")
                }
            }
        )

        PermissionStateRow(
            title = "Draw Over Other Apps",
            subtitle = "Allows visual slide-in edge overlays to intercept ticks",
            icon = Icons.Outlined.Layers,
            isActive = isOverlayEnabled,
            onGrantClick = {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        )
    }
}

@Composable
fun PermissionStateRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isActive) PeachAccent else BorderColor.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) DarkEspresso else MediumText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = DarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = MediumText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) BorderColor.copy(alpha = 0.1f) else BrandBrown,
                contentColor = if (isActive) MediumText else Color.White
            ),
            border = if (isActive) BorderStroke(1.dp, BorderColor) else null,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .height(34.dp)
                .testTag("grant_button_${title.replace(" ", "_").lowercase()}")
        ) {
            Text(
                text = if (isActive) "Active" else "Grant",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TriggerSetupWorkspace(
    triggers: List<TriggerConfig>,
    onConfigChanged: (TriggerConfig) -> Unit
) {
    var selectedTriggerId by remember { mutableStateOf("LEFT") }
    val currentTrigger = triggers.firstOrNull { it.id == selectedTriggerId }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Trigger edge selector buttons in high density warm styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LEFT", "RIGHT", "BOTTOM").forEach { id ->
                val isSelected = selectedTriggerId == id
                val edgeColors = when (id) {
                    "LEFT" -> PeachAccent to Color(0xFF2B170B)
                    "RIGHT" -> SoftSkyBlue to Color(0xFF001D35)
                    else -> LavenderAccent to Color(0xFF21005D)
                }

                Button(
                    onClick = { selectedTriggerId = id },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("trigger_tab_$id"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) edgeColors.first else Color.White,
                        contentColor = if (isSelected) edgeColors.second else MediumText
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) edgeColors.first else BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$id Edge",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (currentTrigger != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeColor = when (currentTrigger.id) {
                                "LEFT" -> Color(0xFFFFDBCB)
                                "RIGHT" -> Color(0xFFC2E7FF)
                                else -> Color(0xFFD0BCFF)
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(activeColor, CircleShape)
                                    .border(1.dp, BrandBrown, CircleShape)
                            )
                            Text(
                                text = "Customize ${currentTrigger.id} Trigger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DarkText
                            )
                        }

                        // Enable toggle
                        Switch(
                            checked = currentTrigger.enabled,
                            onCheckedChange = { onConfigChanged(currentTrigger.copy(enabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandBrown,
                                uncheckedThumbColor = MediumText,
                                uncheckedTrackColor = BorderColor.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("enable_trigger_switch")
                        )
                    }

                    if (currentTrigger.enabled) {
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                        // Common Configuration Settings (Opacity & Color)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Opacity visual percent
                            SliderSetting(
                                title = "Visibility Transparency Opacity (${currentTrigger.opacityPercent}%)",
                                value = currentTrigger.opacityPercent.toFloat(),
                                valueRange = 10f..100f,
                                onValueChange = { onConfigChanged(currentTrigger.copy(opacityPercent = it.toInt())) }
                            )

                            // 2. Light color selection palette
                            Text(
                                text = "Aesthetic Overlay Glow Color",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MediumText
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    0xFFFADCCB to "Warm Peach",
                                    0xFFD0BCFF to "Soft Lavender",
                                    0xFFC2E7FF to "Soft Sky",
                                    0xFFE8DEF8 to "Muted Prune",
                                    0xFF7C533C to "Rich Cocoa"
                                ).forEach { (colorHex, name) ->
                                    val color = Color(colorHex)
                                    val isSelected = currentTrigger.color == colorHex.toInt()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                2.dp,
                                                if (isSelected) BrandBrown else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { onConfigChanged(currentTrigger.copy(color = colorHex.toInt())) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = if (colorHex == 0xFF7C533C) Color.White else DarkText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                        // Portrait Layout Subsystem Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = WarmBg.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.StayCurrentPortrait,
                                        contentDescription = "Portrait Mode Settings",
                                        tint = BrandBrown,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Portrait Mode Settings",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                }

                                // Portrait Thickness
                                SliderSetting(
                                    title = "Trigger Strip Thickness (${currentTrigger.sizeDp} dp)",
                                    value = currentTrigger.sizeDp.toFloat(),
                                    valueRange = 8f..36f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(sizeDp = it.toInt())) }
                                )

                                // Portrait Coverage Length
                                SliderSetting(
                                    title = "Coverage Screen Length (${currentTrigger.heightPercent}%)",
                                    value = currentTrigger.heightPercent.toFloat(),
                                    valueRange = 15f..100f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(heightPercent = it.toInt())) }
                                )

                                // Portrait Position Offset
                                val offsetTitle = if (currentTrigger.id == "BOTTOM") {
                                    "Horizontal Position Center (${currentTrigger.positionPercent}%)"
                                } else {
                                    "Vertical Position Offset (${currentTrigger.positionPercent}%)"
                                }
                                SliderSetting(
                                    title = offsetTitle,
                                    value = currentTrigger.positionPercent.toFloat(),
                                    valueRange = 10f..90f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(positionPercent = it.toInt())) }
                                )
                            }
                        }

                        // Landscape Layout Subsystem Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = WarmBg.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.StayCurrentLandscape,
                                        contentDescription = "Landscape Mode Settings",
                                        tint = BrandBrown,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Landscape Mode Settings",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                }

                                // Landscape Thickness
                                SliderSetting(
                                    title = "Trigger Strip Thickness (${currentTrigger.landscapeSizeDp} dp)",
                                    value = currentTrigger.landscapeSizeDp.toFloat(),
                                    valueRange = 8f..36f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(landscapeSizeDp = it.toInt())) }
                                )

                                // Landscape Coverage Length
                                SliderSetting(
                                    title = "Coverage Screen Length (${currentTrigger.landscapeHeightPercent}%)",
                                    value = currentTrigger.landscapeHeightPercent.toFloat(),
                                    valueRange = 15f..100f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(landscapeHeightPercent = it.toInt())) }
                                )

                                // Landscape Position Offset
                                val landscapeOffsetTitle = if (currentTrigger.id == "BOTTOM") {
                                    "Horizontal Position Center (${currentTrigger.landscapePositionPercent}%)"
                                } else {
                                    "Vertical Position Offset (${currentTrigger.landscapePositionPercent}%)"
                                }
                                SliderSetting(
                                    title = landscapeOffsetTitle,
                                    value = currentTrigger.landscapePositionPercent.toFloat(),
                                    valueRange = 10f..90f,
                                    onValueChange = { onConfigChanged(currentTrigger.copy(landscapePositionPercent = it.toInt())) }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "This trigger is disabled. Enable it above to manage its parameters.",
                                color = MediumText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MediumText
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = BrandBrown,
                activeTrackColor = BrandBrown.copy(alpha = 0.5f),
                inactiveTrackColor = BorderColor.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun GestureMappingWorkspace(
    triggers: List<TriggerConfig>,
    gestureActions: List<GestureAction>,
    macros: List<Macro>,
    onActionChanged: (triggerId: String, gestureType: String, actionType: String, actionData: String) -> Unit
) {
    var selectedTriggerId by remember { mutableStateOf("LEFT") }
    var mappingToEdit by remember { mutableStateOf<Pair<String, String>?>(null) } // triggerId, gestureType

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Trigger select switches mapped to respective warm edge accents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LEFT", "RIGHT", "BOTTOM").forEach { id ->
                val isSelected = selectedTriggerId == id
                val edgeColors = when (id) {
                    "LEFT" -> PeachAccent to Color(0xFF2B170B)
                    "RIGHT" -> SoftSkyBlue to Color(0xFF001D35)
                    else -> LavenderAccent to Color(0xFF21005D)
                }

                Button(
                    onClick = { selectedTriggerId = id },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) edgeColors.first else Color.White,
                        contentColor = if (isSelected) edgeColors.second else MediumText
                    ),
                    border = BorderStroke(1.dp, if (isSelected) edgeColors.first else BorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$id Bindings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List gestures and mappings
        val gestures = listOf("SWIPE_IN", "SWIPE_UP", "SWIPE_DOWN", "DOUBLE_TAP", "LONG_PRESS")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assign Action Triggers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText
                )
                Text(
                    text = "Associate actions to gestures on the selected edge trigger.",
                    fontSize = 11.sp,
                    color = MediumText
                )
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

                gestures.forEach { gesture ->
                    val mapping = gestureActions.firstOrNull { it.triggerId == selectedTriggerId && it.gestureType == gesture }
                    val currentAction = mapping?.actionType ?: "NONE"
                    val currentData = mapping?.actionData ?: ""
                    val isMapped = currentAction != "NONE"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { mappingToEdit = selectedTriggerId to gesture }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gesture.replace("_", " "),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DarkText
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(if (isMapped) BrandBrown else BorderColor, CircleShape)
                                )
                                Text(
                                    text = if (!isMapped) "Direct Action: Unmapped" else "Action: $currentAction ${if (currentData.isNotEmpty()) "($currentData)" else ""}",
                                    fontSize = 11.sp,
                                    color = if (isMapped) BrandBrown else MediumText,
                                    fontWeight = if (isMapped) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Map Action",
                            tint = BrandBrown,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal dialogue mapping bind choices
    mappingToEdit?.let { (triggerId, gestureType) ->
        ActionConfigDialog(
            triggerId = triggerId,
            gestureType = gestureType,
            macros = macros,
            currentBinding = gestureActions.firstOrNull { it.triggerId == triggerId && it.gestureType == gestureType },
            onDismiss = { mappingToEdit = null },
            onConfirm = { actionType, actionData ->
                onActionChanged(triggerId, gestureType, actionType, actionData)
                mappingToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfigDialog(
    triggerId: String,
    gestureType: String,
    macros: List<Macro>,
    currentBinding: GestureAction?,
    onDismiss: () -> Unit,
    onConfirm: (actionType: String, actionData: String) -> Unit
) {
    var selectedActionType by remember { mutableStateOf(currentBinding?.actionType ?: "NONE") }
    var speakTextInput by remember { mutableStateOf(if (currentBinding?.actionType == "SPEAK_TEXT") currentBinding.actionData else "") }
    var selectedMacroId by remember { mutableStateOf(if (currentBinding?.actionType == "RUN_MACRO") currentBinding.actionData else "") }

    val cleanActionList = listOf(
        "NONE" to "Unmapped",
        "BACK" to "System Back Arrow",
        "HOME" to "System Home Button",
        "RECENTS" to "Multitasking Recents",
        "NOTIFICATIONS" to "Notification Shade",
        "FLASHLIGHT" to "Camera Flashlight Toggle",
        "SCREENSHOT" to "Instant Screenshot",
        "SPEAK_TEXT" to "Text-to-Speech Feedback",
        "RUN_MACRO" to "Automate Macro Sequence"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Map Gesture Action",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Bound Trigger: $triggerId -> ${gestureType.replace("_", " ")}",
                    fontSize = 12.sp,
                    color = MediumText,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

                cleanActionList.forEach { (actionKey, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedActionType == actionKey) WarmPeach else Color.Transparent)
                            .clickable { selectedActionType = actionKey }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedActionType == actionKey,
                            onClick = { selectedActionType = actionKey },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandBrown, unselectedColor = MediumText)
                        )
                        Text(label, color = DarkText, fontSize = 13.sp)
                    }
                }

                // Conditional inputs depending on selections
                if (selectedActionType == "SPEAK_TEXT") {
                    OutlinedTextField(
                        value = speakTextInput,
                        onValueChange = { speakTextInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_text_field")
                            .padding(top = 8.dp),
                        label = { Text("TTS Text to Synthesize") },
                        placeholder = { Text("e.g., Welcome back human!") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBrown,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = BrandBrown,
                            unfocusedLabelColor = MediumText
                        )
                    )
                }

                if (selectedActionType == "RUN_MACRO") {
                    if (macros.isEmpty()) {
                        Text(
                            "No macros constructed yet. Create one inside the Macros tab first!",
                            color = NeonRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text("Choose Macro Sequence:", fontSize = 11.sp, color = MediumText, modifier = Modifier.padding(top = 8.dp))
                        macros.forEach { macro ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedMacroId == macro.id.toString()) SoftSkyBlue else Color.Transparent)
                                    .clickable { selectedMacroId = macro.id.toString() }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedMacroId == macro.id.toString(),
                                    onClick = { selectedMacroId = macro.id.toString() },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandBrown, unselectedColor = MediumText)
                                )
                                Text(macro.name, color = DarkText, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalData = when (selectedActionType) {
                        "SPEAK_TEXT" -> speakTextInput
                        "RUN_MACRO" -> selectedMacroId
                        else -> ""
                    }
                    onConfirm(selectedActionType, finalData)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBrown, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Binding", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroStudioWorkspace(
    macros: List<Macro>,
    onSaveMacro: (name: String, stepsJson: String) -> Unit,
    onDeleteMacro: (Macro) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Automation Macros Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText
                )
                Text(
                    text = "Automate long chains of operations in one tick",
                    fontSize = 11.sp,
                    color = MediumText
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBrown, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("create_macro_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (macros.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoMode,
                            contentDescription = "Empty",
                            tint = MediumText,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Macros Available",
                            color = DarkText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap 'Create New' above to write automated scripts.",
                            color = MediumText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            macros.forEach { macro ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = macro.name,
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Steps Json: ${macro.stepsJson}",
                                maxLines = 1,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = BrandBrown,
                                modifier = Modifier.widthIn(max = 240.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteMacro(macro) },
                            modifier = Modifier.testTag("delete_macro_${macro.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = NeonRed
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateMacroDialog(
            onDismiss = { showCreateDialog = false },
            onConfirmSave = { name, stepsJson ->
                onSaveMacro(name, stepsJson)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMacroDialog(
    onDismiss: () -> Unit,
    onConfirmSave: (name: String, stepsJson: String) -> Unit
) {
    var macroName by remember { mutableStateOf("") }
    val stepList = remember { mutableStateListOf<Pair<String, String>>() } // Type to Argument

    val possiblePresets = listOf(
        "SYS_HOME" to "Go Home Screen",
        "SYS_BACK" to "Press Back Key",
        "SYS_RECENTS" to "Open Recents Window",
        "TOGGLE_FLASH" to "Flashlight Flutter",
        "SPEAK_TEXT" to "Pronounce TTS Sentence",
        "DELAY" to "Wait Sleep Time ms"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Assemble Macro Process", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro Name") },
                    placeholder = { Text("e.g., Silent Mode") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBrown,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        focusedLabelColor = BrandBrown,
                        unfocusedLabelColor = MediumText
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("macro_name_input")
                )

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                Text("Steps Sequence Flow:", fontSize = 12.sp, color = DarkText, fontWeight = FontWeight.Bold)

                if (stepList.isEmpty()) {
                    Text(
                        "No steps registered. Capture process components below.",
                        fontSize = 11.sp,
                        color = MediumText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    stepList.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarmPeach.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Step ${index + 1}: ${step.first}",
                                    color = BrandBrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                if (step.second.isNotEmpty()) {
                                    Text("Arg: ${step.second}", color = DarkEspresso, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { stepList.removeAt(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete Step", tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                // Appender buttons
                Text("Append Steps Preset Component:", fontSize = 11.sp, color = MediumText)

                possiblePresets.forEach { (type, description) ->
                    Button(
                        onClick = {
                            val argValue = when (type) {
                                "SPEAK_TEXT" -> "Script action executed"
                                "DELAY" -> "500"
                                else -> ""
                            }
                            stepList.add(type to argValue)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkText),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("+ Add: $description", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (macroName.isNotEmpty() && stepList.isNotEmpty()) {
                        val jsonBuilder = StringBuilder()
                        jsonBuilder.append("[")
                        stepList.forEachIndexed { index, pair ->
                            jsonBuilder.append("{\"type\":\"${pair.first}\",\"arg\":\"${pair.second}\"}")
                            if (index < stepList.size - 1) jsonBuilder.append(",")
                        }
                        jsonBuilder.append("]")
                        onConfirmSave(macroName, jsonBuilder.toString())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBrown, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Macro Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MediumText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun InAppSandboxWorkspace(
    triggers: List<TriggerConfig>,
    gestureActions: List<GestureAction>,
    macros: List<Macro>,
    onGestureTriggered: (triggerId: String, gestureType: String) -> Unit
) {
    var detectedSwipeInfo by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var sandboxOrientation by remember { mutableStateOf("PORTRAIT") } // "PORTRAIT" or "LANDSCAPE"

    // Interactive custom phone simulator drawing with High Density Theme styling
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = "Live Interactive Sandbox Studio",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkText
            )
            Text(
                text = "Swipe, hold, or double-tap inside the device simulator below to test your gesture bindings in real-time.",
                fontSize = 11.sp,
                color = MediumText
            )
        }

        // Orientation Swapper
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview Mode: ${sandboxOrientation.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MediumText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("PORTRAIT", "LANDSCAPE").forEach { mode ->
                    val isSelected = sandboxOrientation == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { sandboxOrientation = mode },
                        label = { Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBrown,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = MediumText
                        ),
                        border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                            selected = false,
                            enabled = true,
                            borderColor = BorderColor,
                            borderWidth = 1.dp
                        )
                    )
                }
            }
        }

        Text(
            text = "💡 Drag/scroll inside lists/margins (not on the device frame) to scroll this view.",
            fontSize = 10.sp,
            color = MediumText,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Beautifully framed simulated phone container
        val mockHeight = if (sandboxOrientation == "PORTRAIT") 370.dp else 220.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mockHeight)
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(4.dp, BrandBrown, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Interactive glass canvas inside Simulator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Soft grid grid lines
                        drawRect(
                            color = BorderColor.copy(alpha = 0.15f),
                            size = size
                        )
                    }
            ) {
                // Render simulated configured overlay lines
                triggers.forEach { config ->
                    if (config.enabled) {
                        val activeColor = Color(config.color)
                        val opacity = config.opacityPercent / 100f

                        val alignment = when (config.id) {
                            "LEFT" -> Alignment.CenterStart
                            "RIGHT" -> Alignment.CenterEnd
                            "BOTTOM" -> Alignment.BottomCenter
                            else -> Alignment.CenterStart
                        }

                        val isInboxLandscape = sandboxOrientation == "LANDSCAPE"
                        val currentSizeDp = if (isInboxLandscape) config.landscapeSizeDp else config.sizeDp
                        val currentHeightPercent = if (isInboxLandscape) config.landscapeHeightPercent else config.heightPercent

                        val w = if (config.id == "LEFT" || config.id == "RIGHT") {
                            currentSizeDp.dp
                        } else {
                            (currentHeightPercent * 3).dp
                        }

                        val h = if (config.id == "LEFT" || config.id == "RIGHT") {
                            (currentHeightPercent * 3.2).dp
                        } else {
                            currentSizeDp.dp
                        }

                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .size(w, h)
                                .background(activeColor.copy(alpha = opacity), RoundedCornerShape(10.dp))
                                .border(1.5.dp, activeColor, RoundedCornerShape(10.dp))
                                .pointerInput(config.id) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            onGestureTriggered(config.id, "DOUBLE_TAP")
                                            detectedSwipeInfo = "Double Tap on ${config.id} edge!"
                                        },
                                        onLongPress = {
                                            onGestureTriggered(config.id, "LONG_PRESS")
                                            detectedSwipeInfo = "Long Press on ${config.id} edge!"
                                        },
                                        onTap = {
                                            onGestureTriggered(config.id, "TAP")
                                            detectedSwipeInfo = "Single Tap on ${config.id} edge!"
                                        }
                                    )
                                }
                                .pointerInput(config.id) {
                                    var totalX = 0f
                                    var totalY = 0f
                                    detectDragGestures(
                                        onDragStart = {
                                            totalX = 0f
                                            totalY = 0f
                                        },
                                        onDrag = { _, dragAmount ->
                                            totalX += dragAmount.x
                                            totalY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            val threshold = 50f
                                            if (Math.abs(totalX) > threshold || Math.abs(totalY) > threshold) {
                                                val swipe = interpretSimulatedSwipe(config.id, totalX, totalY, threshold)
                                                onGestureTriggered(config.id, swipe)
                                                detectedSwipeInfo = "Swiped '$swipe' on ${config.id} edge!"
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }

                // Inner content layout of smartphone device
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Smartphone,
                        tint = BrandBrown,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "Phone Display Canvas",
                        color = DarkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = detectedSwipeInfo ?: "Awaiting interactive testing gestures...",
                        color = if (detectedSwipeInfo != null) DarkEspresso else MediumText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(WarmBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

// Logic coordinates mapping matching in the simulated view box
fun getSimulatedTriggerForCoords(offset: Offset, screenW: Float, screenH: Float): String? {
    val marginThreshold = 60 // inner pixel target touch band
    return when {
        offset.x < marginThreshold -> "LEFT"
        offset.x > (screenW - marginThreshold) -> "RIGHT"
        offset.y > (screenH - marginThreshold) -> "BOTTOM"
        else -> null
    }
}

fun interpretSimulatedSwipe(triggerId: String, totalX: Float, totalY: Float, threshold: Float): String {
    return when (triggerId) {
        "LEFT" -> {
            if (totalX > threshold) "SWIPE_IN"
            else if (totalY < -threshold) "SWIPE_UP"
            else "SWIPE_DOWN"
        }
        "RIGHT" -> {
            if (totalX < -threshold) "SWIPE_IN"
            else if (totalY < -threshold) "SWIPE_UP"
            else "SWIPE_DOWN"
        }
        "BOTTOM" -> {
            if (totalY < -threshold) "SWIPE_IN"
            else if (totalX < -threshold) "SWIPE_UP"
            else "SWIPE_DOWN"
        }
        else -> "SWIPE_IN"
    }
}

@Composable
fun UbikiBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = Color(0xFFF3EDF7),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == "TRIGGERS",
            onClick = { onTabSelected("TRIGGERS") },
            icon = { Icon(Icons.Outlined.LinearScale, contentDescription = "Triggers") },
            label = { Text("Triggers", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkEspresso,
                selectedTextColor = DarkEspresso,
                indicatorColor = WarmPeach,
                unselectedIconColor = MediumText,
                unselectedTextColor = MediumText
            ),
            modifier = Modifier.testTag("nav_tab_triggers")
        )

        NavigationBarItem(
            selected = currentTab == "GESTURES",
            onClick = { onTabSelected("GESTURES") },
            icon = { Icon(Icons.Outlined.Swipe, contentDescription = "Gestures") },
            label = { Text("Gestures", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkEspresso,
                selectedTextColor = DarkEspresso,
                indicatorColor = WarmPeach,
                unselectedIconColor = MediumText,
                unselectedTextColor = MediumText
            ),
            modifier = Modifier.testTag("nav_tab_gestures")
        )

        NavigationBarItem(
            selected = currentTab == "MACROS",
            onClick = { onTabSelected("MACROS") },
            icon = { Icon(Icons.Outlined.Bolt, contentDescription = "Macros") },
            label = { Text("Macros", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkEspresso,
                selectedTextColor = DarkEspresso,
                indicatorColor = WarmPeach,
                unselectedIconColor = MediumText,
                unselectedTextColor = MediumText
            ),
            modifier = Modifier.testTag("nav_tab_macros")
        )

        NavigationBarItem(
            selected = currentTab == "SANDBOX",
            onClick = { onTabSelected("SANDBOX") },
            icon = { Icon(Icons.Outlined.Smartphone, contentDescription = "Sandbox") },
            label = { Text("Sandbox", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkEspresso,
                selectedTextColor = DarkEspresso,
                indicatorColor = WarmPeach,
                unselectedIconColor = MediumText,
                unselectedTextColor = MediumText
            ),
            modifier = Modifier.testTag("nav_tab_sandbox")
        )
    }
}

// Global short utility toast
object ToastUtils {
    fun show(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
