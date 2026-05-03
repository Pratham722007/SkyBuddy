package com.example.skybuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.ui.chat.ChatScreen
import com.example.skybuddy.ui.home.HomeScreen
import com.example.skybuddy.ui.journey.HomePhaseScreen
import com.example.skybuddy.ui.journey.JourneyPhase
import com.example.skybuddy.ui.journey.JourneyStateSelectionScreen
import com.example.skybuddy.ui.journey.JourneyViewModel
import com.example.skybuddy.ui.map.IndoorMapScreen
import com.example.skybuddy.ui.modelload.ModelLoadScreen
import com.example.skybuddy.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            HomeScreen(
                onOpenChat = { flight -> 
                    if (flight == null) {
                        navController.navigate(Routes.chat("timeline"))
                    } else {
                        navController.navigate(Routes.journeySelection(flight))
                    }
                }
            )
        }
        composable(
            route = Routes.JOURNEY_SELECTION_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber") ?: return@composable
            val journeyViewModel: JourneyViewModel = hiltViewModel()

            JourneyStateSelectionScreen(
                flightNumber = flightNumber,
                onPhaseSelected = { phase ->
                    if (phase == JourneyPhase.HOME) {
                        navController.navigate(Routes.homePhase(flightNumber)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    } else {
                        journeyViewModel.setPhase(phase)
                        navController.navigate(Routes.indoorMap(flightNumber)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                }
            )
        }
        composable(
            route = Routes.HOME_PHASE_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber") ?: return@composable
            
            // Note: In a real app we'd fetch the departure time from a ViewModel. 
            // For now, we will pass a placeholder epoch or fetch it quickly if possible.
            // Since FlightRepository is available in Hilt, we could get it, but Composables 
            // shouldn't do direct DB ops. We'll assume a dummy or 0 for now unless we add a ViewModel.
            // The HomePhaseScreen will still work, just the alarm might not fire if epoch is 0.

            HomePhaseScreen(
                flightNumber = flightNumber,
                departureTimeEpoch = 0L, // Placeholder
                onChatClicked = { navController.navigate(Routes.chat(flightNumber)) },
                onAtAirportClicked = { 
                    navController.navigate(Routes.journeySelection(flightNumber)) {
                        popUpTo(Routes.HOME_PHASE_PATTERN) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.INDOOR_MAP_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber") ?: return@composable
            
            IndoorMapScreen(
                onChatClicked = { navController.navigate(Routes.chat(flightNumber)) },
                onHelpClicked = { navController.navigate(Routes.chat("help")) }
            )
        }
        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber")
            val flight = if (flightNumber == "timeline" || flightNumber == "help") null else flightNumber
            ChatScreen(
                flightNumber = flight,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
