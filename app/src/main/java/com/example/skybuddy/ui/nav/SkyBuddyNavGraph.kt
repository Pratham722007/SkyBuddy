package com.example.skybuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skybuddy.ui.chat.ChatScreen
import com.example.skybuddy.ui.home.HomeScreen
import com.example.skybuddy.ui.modelload.ModelLoadScreen
import com.example.skybuddy.ui.onboarding.OnboardingScreen

@Composable
fun SkyBuddyNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onModelReady = {
                    navController.navigate(Routes.MODEL_LOAD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MODEL_LOAD) {
            ModelLoadScreen(
                onReady = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.MODEL_LOAD) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(onOpenChat = { flight -> navController.navigate(Routes.chat(flight)) })
        }
        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber")
            ChatScreen(
                flightNumber = flightNumber,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
