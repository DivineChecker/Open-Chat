package com.yuvraj.openchatai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yuvraj.openchatai.ui.AppViewModel
import com.yuvraj.openchatai.ui.screens.ChatScreen
import com.yuvraj.openchatai.ui.screens.ProvidersScreen
import com.yuvraj.openchatai.ui.screens.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "chat",
    ) {
        composable("chat") {
            ChatScreen(
                viewModel = appViewModel,
                onOpenProviders = { navController.navigate("providers") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("providers") {
            ProvidersScreen(
                viewModel = appViewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = appViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
