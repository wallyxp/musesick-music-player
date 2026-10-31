package com.wally.musesick

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.wally.musesick.ui.MusicViewModel
import com.wally.musesick.ui.screens.MainScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wally.musesick.ui.theme.MusesickAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            viewModel.loadLocalTracks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestNeededPermissions()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val appThemeVariant by viewModel.appThemeVariant.collectAsState()
            val customAccentColor by viewModel.customAccentColor.collectAsState()

            MusesickAppTheme(
                appTheme = appTheme,
                appThemeVariant = appThemeVariant,
                customAccentColor = customAccentColor
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()

        // Storage permissions for local .mp3, .m4a, .flac files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

