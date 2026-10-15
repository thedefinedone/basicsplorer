package com.thedefinedone.basicsplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thedefinedone.basicsplorer.ui.screens.*
import com.thedefinedone.basicsplorer.ui.theme.BasicsplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for "All files access" once on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        val prefs = getSharedPreferences("basicsplorer_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        setContent {
            BasicsplorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (isFirstLaunch) "start" else "dashboard"
                    ) {
                        composable("start") {
                            StartScreen(onStartClicked = {
                                prefs.edit().putBoolean("is_first_launch", false).apply()
                                navController.navigate("dashboard") {
                                    popUpTo("start") { inclusive = true }
                                }
                            })
                        }

                        composable("dashboard") {
                            DashboardScreen(navController = navController)
                        }

                        composable(
                            route = "explorer?path={path}",
                            arguments = listOf(navArgument("path") {
                                type = NavType.StringType
                                defaultValue = "/storage/emulated/0"
                            })
                        ) { backStackEntry ->
                            val path = backStackEntry.arguments?.getString("path")
                                ?: "/storage/emulated/0"
                            FileExplorerScreen(
                                path = path,
                                onBack = { navController.popBackStack() },
                                onFolderClick = { folderPath ->
                                    val encoded = Uri.encode(folderPath)
                                    navController.navigate("explorer?path=$encoded")
                                }
                            )
                        }

                        composable("cleaner") {
                            CleanerScreen(onBack = { navController.popBackStack() })
                        }

                        composable("analyzer") {
                            AnalyserScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}