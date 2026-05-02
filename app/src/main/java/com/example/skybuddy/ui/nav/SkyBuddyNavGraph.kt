package com.example.skybuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skybuddy.ui.chat.ChatScreen
import com.example.skybuddy.ui.home.HomeScreen
import com.example.skybuddy.ui.journey.JourneyPhase
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.map.IndoorMapScreen
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
            MainRoute(
                onOpenChat = { flight -> navController.navigate(Routes.chat(flight ?: "timeline")) }
            )
        }
        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber")
            val flight = if (flightNumber == "timeline") null else flightNumber
            ChatScreen(
                flightNumber = flight,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainRoute(
    onOpenChat: (String?) -> Unit,
    journeyViewModel: JourneyViewModel = hiltViewModel()
) {
    val phase by journeyViewModel.currentPhase.collectAsState()
    
    if (phase == JourneyPhase.HOME) {
        HomeScreen(onOpenChat = { onOpenChat(it) })
    } else {
        IndoorMapScreen(
            onChatClicked = { onOpenChat(null) },
            onHelpClicked = { onOpenChat("help") } // Magic string to trigger visual help routing
        )
    }
}
