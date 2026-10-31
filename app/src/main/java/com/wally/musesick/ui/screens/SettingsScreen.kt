package com.wally.musesick.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.wally.musesick.R
import com.wally.musesick.ui.components.GlassBackButton
import com.wally.musesick.ui.components.GlassBorderBrush
import com.wally.musesick.ui.components.GlassBorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import com.wally.musesick.ui.theme.TimeThemeHelper
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.wally.musesick.model.AccentColorPresets
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.ColorPreset
import com.wally.musesick.repository.SettingsRepository
import com.wally.musesick.repository.YtAccountInfo
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Main Settings Window
 */
@Composable
fun SettingsScreen(
    currentVersion: String,
    appTheme: AppTheme,
    appThemeVariant: String = "",
    customAccentColor: Color,
    isCheckingUpdate: Boolean,
    ytAccountInfo: YtAccountInfo? = null,
    isSyncingYtAccount: Boolean = false,
    autoSyncInterval: SettingsRepository.AutoSyncInterval = SettingsRepository.AutoSyncInterval.DAILY,
    onLoginYtMusic: () -> Unit = {},
    onSyncYtMusic: () -> Unit = {},
    onLogoutYtMusic: () -> Unit = {},
    onOpenAutoSync: () -> Unit = {},
    onBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenAppTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAccountOptionsDialog by remember { mutableStateOf(false) }

    if (showAccountOptionsDialog && ytAccountInfo != null) {
        AlertDialog(
            onDismissRequest = { showAccountOptionsDialog = false },
            title = {
                Text(
                    text = ytAccountInfo.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your YouTube Music account is connected. You can re-sync your playlists or sign out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountOptionsDialog = false
                        onSyncYtMusic()
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sync Playlists")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccountOptionsDialog = false
                        onLogoutYtMusic()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))

            // Profile Avatar (Google Profile Picture when logged in, Default Profile Logo when Guest)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        GlassBorderBrush,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!ytAccountInfo?.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ytAccountInfo!!.avatarUrl,
                        contentDescription = ytAccountInfo.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = ytAccountInfo?.name?.ifBlank { "Guest" } ?: "Guest",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "APPLICATION & UPDATES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // 1. Check for Updates
            item {
                SettingsMenuCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = if (isCheckingUpdate) "Checking for new releases..." else "Current version: v$currentVersion",
                    isLoading = isCheckingUpdate,
                    onClick = onCheckForUpdates
                )
            }

            // 2. Login with YouTube Music
            item {
                val isLoggedIn = ytAccountInfo != null
                SettingsMenuCard(
                    icon = Icons.Default.AccountCircle,
                    title = if (isLoggedIn) "YouTube Music (${ytAccountInfo!!.name})" else "Login with YouTube Music",
                    subtitle = when {
                        isSyncingYtAccount -> "Syncing your YouTube Music playlists..."
                        isLoggedIn -> "Connected • Tap to sync playlists or log out"
                        else -> "Sign in with Google to sync your playlists & profile"
                    },
                    isLoading = isSyncingYtAccount,
                    onClick = {
                        if (isLoggedIn) {
                            showAccountOptionsDialog = true
                        } else {
                            onLoginYtMusic()
                        }
                    }
                )
            }

            // 3. Automatic Sync
            item {
                SettingsMenuCard(
                    icon = Icons.Default.Sync,
                    title = "Automatic Sync",
                    subtitle = autoSyncInterval.label,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    isLoading = isSyncingYtAccount,
                    onClick = onOpenAutoSync
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "APPEARANCE & PLAYBACK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // App Theme
            item {
                val themeSubtitle = if (appTheme == AppTheme.CUSTOM_COLOR) {
                    val match = AccentColorPresets.find { it.primaryColor == customAccentColor }
                    "Choose Color (${match?.name ?: "Custom"})"
                } else if (appTheme.availableVariants.isNotEmpty()) {
                    val variantName = appTheme.availableVariants.find { it.id == appThemeVariant }?.displayName ?: appThemeVariant
                    "${appTheme.displayName} ($variantName)"
                } else {
                    appTheme.displayName
                }

                SettingsMenuCard(
                    icon = Icons.Default.Palette,
                    title = "App Theme",
                    subtitle = themeSubtitle,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    onClick = onOpenAppTheme
                )
            }

            // About / Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Musesick",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version $currentVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dedicated App Theme Settings Sub-Window
 */
@Composable
fun SettingsAppThemeScreen(
    currentTheme: AppTheme,
    currentVariant: String,
    customAccentColor: Color,
    customThemeImagePath: String? = null,
    onThemeSelected: (AppTheme, String) -> Unit,
    onVariantSelected: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onSelectCustomImage: (Uri) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectCustomImage(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Select appearance style & color palette for app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "SELECT APP THEME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // All Themes List
            items(AppTheme.values(), key = { it.name }) { theme ->
                val isThemeSelected = currentTheme == theme
                val effectiveVariant = if (isThemeSelected) {
                    if (currentVariant.isNotBlank()) currentVariant else theme.defaultVariant
                } else {
                    theme.defaultVariant
                }

                ThemeCardWithVariants(
                    theme = theme,
                    isSelected = isThemeSelected,
                    selectedVariant = effectiveVariant,
                    icon = getThemeIcon(theme),
                    onSelectTheme = { t, v ->
                        if (t == AppTheme.CUSTOM_IMAGE && customThemeImagePath.isNullOrEmpty()) {
                            photoPickerLauncher.launch("image/*")
                        } else {
                            onThemeSelected(t, v)
                        }
                    },
                    onSelectVariant = { v -> onVariantSelected(v) },
                    customContent = when {
                        theme == AppTheme.DEFAULT && isThemeSelected -> {
                            {
                                Spacer(modifier = Modifier.height(14.dp))
                                val currentHour = TimeThemeHelper.getCurrentHour()
                                val period = TimeThemeHelper.getTimePeriodName(currentHour)
                                val colors = TimeThemeHelper.getGradientColors(currentHour)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, GlassBorderBrush, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(colors)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Active Time: $period",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Gradient shifts automatically based on device local time",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        theme == AppTheme.CUSTOM_COLOR && isThemeSelected -> {
                            {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "SELECT ACCENT COLOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(AccentColorPresets, key = { it.id }) { preset ->
                                        val isColorChosen = preset.primaryColor == customAccentColor
                                        ColorPresetItem(
                                            preset = preset,
                                            isSelected = isColorChosen,
                                            onClick = {
                                                onColorSelected(preset.primaryColor)
                                                onThemeSelected(AppTheme.CUSTOM_COLOR, "")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        theme == AppTheme.CUSTOM_IMAGE && isThemeSelected -> {
                            {
                                Spacer(modifier = Modifier.height(14.dp))
                                if (!customThemeImagePath.isNullOrEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, GlassBorderBrush, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, GlassBorderBrush, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = File(customThemeImagePath),
                                                contentDescription = "Selected Background",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Wallpaper Active",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Darkened & blurred behind app",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { photoPickerLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, GlassBorderBrush),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Change", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                            .clickable { photoPickerLauncher.launch("image/*") }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Select Wallpaper Image",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> null
                    }
                )
            }
        }
    }
}

private fun Color.isLight(): Boolean {
    val effectiveLuminance = (0.299f * red + 0.587f * green + 0.114f * blue) * alpha
    return effectiveLuminance >= 0.5f
}

private fun blendColors(base: Color, overlay: Color, amount: Float): Color {
    val factor = amount.coerceIn(0f, 1f)
    val r = base.red + (overlay.red - base.red) * factor
    val g = base.green + (overlay.green - base.green) * factor
    val b = base.blue + (overlay.blue - base.blue) * factor
    return Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = 1f
    )
}

/**
 * Reusable card for themes that includes selectable variant chips when active.
 */
@Composable
private fun ThemeCardWithVariants(
    theme: AppTheme,
    isSelected: Boolean,
    selectedVariant: String,
    icon: ImageVector,
    onSelectTheme: (AppTheme, String) -> Unit,
    onSelectVariant: (String) -> Unit,
    customContent: (@Composable () -> Unit)? = null
) {
    val isAquaSelected = theme == AppTheme.AQUA && isSelected
    val primaryColor = MaterialTheme.colorScheme.primary

    val cardBorder = if (isAquaSelected) {
        BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF40C4FF),
                    Color.White.copy(alpha = 0.6f),
                    Color(0xFF40C4FF).copy(alpha = 0.85f)
                )
            )
        )
    } else if (isSelected) {
        BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    primaryColor,
                    Color.White.copy(alpha = 0.5f),
                    primaryColor.copy(alpha = 0.75f)
                )
            )
        )
    } else {
        BorderStroke(1.dp, GlassBorderBrush)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isAquaSelected) {
                    Modifier
                } else if (isSelected) {
                    Modifier.background(primaryColor.copy(alpha = 0.18f))
                } else {
                    Modifier.background(Color(0x381A1A1A))
                }
            )
            .border(cardBorder, RoundedCornerShape(16.dp))
            .clickable {
                onSelectTheme(
                    theme,
                    if (selectedVariant.isNotBlank()) selectedVariant else theme.defaultVariant
                )
            }
    ) {
        // If Aqua theme is selected, the photo itself is the card background with gradient overlay
        if (isAquaSelected) {
            Image(
                painter = painterResource(id = R.drawable.damegami),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Left-to-right gradient to keep title and icons crisp and readable
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.88f),
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.20f)
                            )
                        )
                    )
            )
            // Top-to-bottom vignette gradient with Aqua cyan tint
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF001F3F).copy(alpha = 0.50f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glassmorphism Circular Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isAquaSelected -> Color(0xFF40C4FF).copy(alpha = 0.30f)
                                isSelected -> primaryColor.copy(alpha = 0.25f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isAquaSelected -> Brush.linearGradient(
                                    listOf(
                                        Color(0xFF40C4FF),
                                        Color.White.copy(alpha = 0.60f)
                                    )
                                )
                                isSelected -> Brush.linearGradient(
                                    listOf(
                                        primaryColor,
                                        Color.White.copy(alpha = 0.50f)
                                    )
                                )
                                else -> Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.20f)
                                    )
                                )
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (theme == AppTheme.AQUA) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_theme_aqua),
                            contentDescription = null,
                            tint = if (isSelected) {
                                Color(0xFF40C4FF)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                    if (isAquaSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Damegami Artwork & Cyan Glass",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF40C4FF).copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                RadioButton(
                    selected = isSelected,
                    onClick = {
                        onSelectTheme(
                            theme,
                            if (selectedVariant.isNotBlank()) selectedVariant else theme.defaultVariant
                        )
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = if (isAquaSelected) Color(0xFF40C4FF) else primaryColor,
                        unselectedColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }

            // Variant Chips if this theme has variants and is selected
            if (isSelected && theme.availableVariants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SELECT PALETTE VARIANT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(theme.availableVariants, key = { it.id }) { variantOpt ->
                        val isVariantActive = (selectedVariant == variantOpt.id) ||
                                (selectedVariant.isBlank() && variantOpt.id == theme.defaultVariant)
                        FilterChip(
                            selected = isVariantActive,
                            onClick = { onSelectVariant(variantOpt.id) },
                            label = {
                                Text(
                                    text = variantOpt.displayName,
                                    fontWeight = if (isVariantActive) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            customContent?.invoke()
        }
    }
}

private fun getThemeIcon(theme: AppTheme): ImageVector {
    return when (theme) {
        AppTheme.DEFAULT -> Icons.Default.Schedule
        AppTheme.AQUA -> Icons.Default.WaterDrop
        AppTheme.CUSTOM_IMAGE -> Icons.Default.Wallpaper
        AppTheme.CUSTOM_COLOR -> Icons.Default.Palette
    }
}

/**
 * Reusable Settings menu item card
 */
@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x381A1A1A))
            .border(
                1.dp,
                GlassBorderBrush,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glassmorphism Menu Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.25f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!badgeText.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Reusable Theme/Style selectable option card
 */
@Composable
private fun ThemeOptionSelectCard(
    title: String,
    description: String = "",
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val cardBorder = if (isSelected) {
        BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    primaryColor,
                    Color.White.copy(alpha = 0.5f),
                    primaryColor.copy(alpha = 0.75f)
                )
            )
        )
    } else {
        BorderStroke(1.dp, GlassBorderBrush)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) primaryColor.copy(alpha = 0.18f) else Color(0x381A1A1A)
            )
            .border(cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glassmorphism Circular Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            primaryColor.copy(alpha = 0.25f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) {
                            Brush.linearGradient(
                                listOf(
                                    primaryColor,
                                    Color.White.copy(alpha = 0.5f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            )
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        primaryColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            if (description.isNotBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = primaryColor,
                    unselectedColor = Color.White.copy(alpha = 0.4f)
                )
            )
        }
    }
}

/**
 * Individual circular swatch item for custom accent color picker
 */
@Composable
private fun ColorPresetItem(
    preset: ColorPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(preset.primaryColor)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                Color.White.copy(alpha = 0.7f)
                            )
                        )
                    } else {
                        GlassBorderBrush
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (preset.primaryColor.isLight()) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Automatic Sync Sub-Screen:
 * Allows choosing between Daily, Weekly, Monthly, and When I Choose to Sync,
 * with a bottom-centered "Sync Now" button for immediate two-way syncing.
 */
@Composable
fun SettingsAutoSyncScreen(
    selectedInterval: SettingsRepository.AutoSyncInterval,
    isSyncing: Boolean,
    isLoggedIn: Boolean,
    onSelectInterval: (SettingsRepository.AutoSyncInterval) -> Unit,
    onSyncNow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Automatic Sync",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "CHOOSE SYNC FREQUENCY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                items(SettingsRepository.AutoSyncInterval.values().toList()) { interval ->
                    ThemeOptionSelectCard(
                        title = interval.label,
                        description = interval.subtitle,
                        icon = Icons.Default.Sync,
                        isSelected = selectedInterval == interval,
                        onClick = { onSelectInterval(interval) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLoggedIn) {
                            "Any playlists you create or edit in Musesick are automatically reflected on your YouTube Music playlists. Existing local playlists are also uploaded and linked when syncing."
                        } else {
                            "Log in with YouTube Music in Settings to enable two-way playlist syncing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }

        // Bottom-Centered Glassmorphic "Sync Now" Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color(0x332A2A2A),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.50f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .clickable(
                    enabled = !isSyncing,
                    onClick = onSyncNow
                )
                .padding(horizontal = 36.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Syncing...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Now",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sync Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
