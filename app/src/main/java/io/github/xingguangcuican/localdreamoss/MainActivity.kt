package io.github.xingguangcuican.localdreamoss

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.xingguangcuican.localdreamoss.navigation.Screen
import io.github.xingguangcuican.localdreamoss.ui.theme.DefaultThemePrimaryArgb
import io.github.xingguangcuican.localdreamoss.ui.screens.ModelListScreen
import io.github.xingguangcuican.localdreamoss.ui.screens.ModelRunScreen
import io.github.xingguangcuican.localdreamoss.ui.screens.OpenAIModelRunScreen
import io.github.xingguangcuican.localdreamoss.ui.screens.UpscaleScreen
import io.github.xingguangcuican.localdreamoss.ui.theme.LocalDreamTheme
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Storage permission is required for saving generated images",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission is required for background image generation",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkStoragePermission() {
        // < Android 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // ok
                }

                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    Toast.makeText(
                        this,
                        "Storage permission is needed for saving generated images",
                        Toast.LENGTH_LONG
                    ).show()
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

                else -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        // > Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // ok
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Notification permission is needed for background image generation",
                        Toast.LENGTH_LONG
                    ).show()
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkStoragePermission()
        checkNotificationPermission()

        setContent {
            val prefs = remember { getSharedPreferences("app_prefs", MODE_PRIVATE) }
            var dynamicColorEnabled by remember {
                mutableStateOf(prefs.getBoolean("theme_dynamic_color", true))
            }
            var themePrimaryColor by remember {
                mutableIntStateOf(prefs.getInt("theme_primary_color", DefaultThemePrimaryArgb))
            }
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                    when (key) {
                        "theme_dynamic_color" -> {
                            dynamicColorEnabled = sp.getBoolean("theme_dynamic_color", true)
                        }

                        "theme_primary_color" -> {
                            themePrimaryColor = sp.getInt("theme_primary_color", DefaultThemePrimaryArgb)
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            LocalDreamTheme(
                dynamicColor = dynamicColorEnabled,
                customPrimaryColor = if (dynamicColorEnabled) null else Color(themePrimaryColor)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.ModelList.route
                    ) {
                        composable(Screen.ModelList.route) {
                            ModelListScreen(navController)
                        }
                        composable(
                            route = Screen.ModelRun.route,
                            arguments = listOf(
                                navArgument("modelId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val modelId = backStackEntry.arguments?.getString("modelId") ?: ""

                            ModelRunScreen(
                                modelId = modelId,
                                navController = navController
                            )
                        }
                        composable(Screen.Upscale.route) {
                            UpscaleScreen(navController)
                        }
                        composable(
                            route = Screen.OpenAIModelRun.route,
                            arguments = listOf(
                                navArgument("modelId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val modelId =
                                backStackEntry.arguments?.getString("modelId") ?: ""
                            OpenAIModelRunScreen(
                                modelId = modelId,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}
