package com.wally.musesick.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wally.musesick.model.AccentColorPresets
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.ColorPreset
import com.wally.musesick.model.PlayerStyle

/**
 * Main Settings Window
 */
@Composable
fun SettingsScreen(
    currentVersion: String,
    playerStyle: PlayerStyle,
    appTheme: AppTheme,
    appThemeVariant: String = "",
    customAccentColor: Color,
    isCheckingUpdate: Boolean,
    onBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAppTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
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

            // 2. Now Playing
            item {
                SettingsMenuCard(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Now Playing",
                    subtitle = playerStyle.displayName,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    onClick = onOpenNowPlaying
                )
            }

            // 3. App Theme
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
                        text = "Version $currentVersion • Clean Aesthetics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Now Playing Settings Sub-Window
 */
@Composable
fun SettingsNowPlayingScreen(
    currentStyle: PlayerStyle,
    currentPlayerTheme: AppTheme?,
    currentPlayerThemeVariant: String?,
    customAccentColor: Color,
    onStyleSelected: (PlayerStyle) -> Unit,
    onPlayerThemeSelected: (AppTheme?, String?) -> Unit,
    onPlayerThemeVariantSelected: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Select visual style & theme for player screen",
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
                    text = "DISPLAY STYLE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Option 1: Full Screen Album Art
            item {
                ThemeOptionSelectCard(
                    title = PlayerStyle.FULLSCREEN_ALBUM_ART.displayName,
                    description = PlayerStyle.FULLSCREEN_ALBUM_ART.description,
                    icon = Icons.Default.Fullscreen,
                    isSelected = currentStyle == PlayerStyle.FULLSCREEN_ALBUM_ART,
                    onClick = { onStyleSelected(PlayerStyle.FULLSCREEN_ALBUM_ART) }
                )
            }

            // Option 2: Non Full Screen Album Art
            item {
                ThemeOptionSelectCard(
                    title = PlayerStyle.NON_FULLSCREEN_ALBUM_ART.displayName,
                    description = PlayerStyle.NON_FULLSCREEN_ALBUM_ART.description,
                    icon = Icons.Default.CropPortrait,
                    isSelected = currentStyle == PlayerStyle.NON_FULLSCREEN_ALBUM_ART,
                    onClick = { onStyleSelected(PlayerStyle.NON_FULLSCREEN_ALBUM_ART) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PLAYER THEME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Text(
                    text = "Styles controls & surfaces when Non Full Screen Album Art is active. Retains Material You player layout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
            }

            // Option 0: Match App Theme
            item {
                ThemeOptionSelectCard(
                    title = "Match App Theme",
                    description = "",
                    icon = Icons.Default.Tune,
                    isSelected = currentPlayerTheme == null,
                    onClick = { onPlayerThemeSelected(null, null) }
                )
            }

            // Themes List
            items(AppTheme.values(), key = { it.name }) { theme ->
                val isThemeSelected = currentPlayerTheme == theme
                val effectiveVariant = if (isThemeSelected) {
                    currentPlayerThemeVariant ?: theme.defaultVariant
                } else {
                    theme.defaultVariant
                }

                ThemeCardWithVariants(
                    theme = theme,
                    isSelected = isThemeSelected,
                    selectedVariant = effectiveVariant,
                    icon = getThemeIcon(theme),
                    onSelectTheme = { t, v -> onPlayerThemeSelected(t, v) },
                    onSelectVariant = { v -> onPlayerThemeVariantSelected(v) },
                    customContent = if (theme == AppTheme.CUSTOM_COLOR && isThemeSelected) {
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
                                            onPlayerThemeSelected(AppTheme.CUSTOM_COLOR, null)
                                        }
                                    )
                                }
                            }
                        }
                    } else null
                )
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
    onThemeSelected: (AppTheme, String) -> Unit,
    onVariantSelected: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
                    onSelectTheme = { t, v -> onThemeSelected(t, v) },
                    onSelectVariant = { v -> onVariantSelected(v) },
                    customContent = if (theme == AppTheme.CUSTOM_COLOR && isThemeSelected) {
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
                    } else null
                )
            }
        }
    }
}

private fun Color.isLight(): Boolean {
    return (0.299f * red + 0.587f * green + 0.114f * blue) >= 0.5f
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
    val isLight = MaterialTheme.colorScheme.surface.isLight()
    val unselectedCardColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedCardColor = if (isLight) {
        blendColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, 0.18f)
    } else {
        blendColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary, 0.22f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectTheme(theme, if (selectedVariant.isNotBlank()) selectedVariant else theme.defaultVariant) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedCardColor else unselectedCardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                RadioButton(
                    selected = isSelected,
                    onClick = { onSelectTheme(theme, if (selectedVariant.isNotBlank()) selectedVariant else theme.defaultVariant) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary,
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
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
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
        AppTheme.MATERIAL_YOU -> Icons.Default.Android
        AppTheme.GRUVBOX -> Icons.Default.Coffee
        AppTheme.EVERFOREST -> Icons.Default.Park
        AppTheme.TOKYO_NIGHT -> Icons.Default.NightsStay
        AppTheme.CATPPUCCIN -> Icons.Default.Pets
        AppTheme.ROSE_PINE -> Icons.Default.Spa
        AppTheme.KANAGAWA -> Icons.Default.Waves
        AppTheme.NORD -> Icons.Default.AcUnit
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
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
    val isLight = MaterialTheme.colorScheme.surface.isLight()
    val unselectedCardColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedCardColor = if (isLight) {
        blendColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, 0.18f)
    } else {
        blendColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary, 0.22f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedCardColor else unselectedCardColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
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
                .clip(CircleShape)
                .background(preset.primaryColor)
                .then(
                    if (isSelected) {
                        Modifier.border(2.5.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
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
