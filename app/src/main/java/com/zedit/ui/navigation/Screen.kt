package com.zedit.ui.navigation

sealed class Screen(val route: String) {
    data object ProjectList : Screen("projects")
    data object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: Long) = "editor/$projectId"
    }
}
