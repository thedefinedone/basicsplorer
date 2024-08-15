package com.example.basicsplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.example.basicsplorer.ui.screens.*
import com.example.basicsplorer.ui.theme.BasicsplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
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
                            val path = backStackEntry.arguments?.getString("path") ?: "/storage/emulated/0"
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