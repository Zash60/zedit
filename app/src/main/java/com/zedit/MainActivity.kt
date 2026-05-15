package com.zedit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavArgument
import androidx.navigation.compose.rememberNavController
import com.zedit.ui.editor.EditorScreen
import com.zedit.ui.navigation.Screen
import com.zedit.ui.projects.ProjectListScreen
import com.zedit.ui.theme.ZeditTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZeditTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.ProjectList.route
                    ) {
                        composable(Screen.ProjectList.route) {
                            ProjectListScreen(
                                onProjectClicked = { projectId ->
                                    navController.navigate(Screen.Editor.createRoute(projectId))
                                }
                            )
                        }
                        composable(
                            route = Screen.Editor.route,
                            arguments = listOf(
                                NamedNavArgument("projectId", NavArgument.Builder().setType(NavType.LongType).build())
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments
                                ?.getLong("projectId") ?: return@composable
                            EditorScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
