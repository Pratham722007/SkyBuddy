package com.example.skysecurity.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun SecurityNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            SecurityMapScreen(
                onAlertsClicked = { navController.navigate("alerts") }
            )
        }
        composable("alerts") {
            AlertListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAlert = { alert ->
                    navController.popBackStack()
                    // ViewModel handles navigation after pop
                }
            )
        }
    }
}
