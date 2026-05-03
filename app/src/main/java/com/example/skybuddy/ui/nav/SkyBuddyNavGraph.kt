package com.example.skybuddy.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skybuddy.ui.chat.ChatScreen
import com.example.skybuddy.ui.flight.FlightDetailScreen
import com.example.skybuddy.ui.flight.FlightInfoScreen
import com.example.skybuddy.ui.home.HomeScreen
import com.example.skybuddy.ui.map.IndoorMapScreen
import com.example.skybuddy.ui.modelload.ModelLoadScreen
import com.example.skybuddy.ui.onboarding.OnboardingScreen
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

// ── Bottom nav tab definition ───────────────────────────────
private data class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomNavTab("Fly", Icons.Filled.Flight, Icons.Outlined.Flight),
    BottomNavTab("Map", Icons.Filled.Map, Icons.Outlined.Map),
    BottomNavTab("Chat", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    BottomNavTab("Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
)

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
                    navController.navigate(Routes.MAIN_SHELL) {
                        popUpTo(Routes.MODEL_LOAD) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN_SHELL) {
            MainShell(navController = navController)
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
        composable(
            route = Routes.FLIGHT_INFO_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber") ?: ""
            FlightInfoScreen(
                flightNumber = flightNumber,
                onBack = { navController.popBackStack() },
                onOpenChat = { fn ->
                    navController.navigate(Routes.chat(fn))
                },
                onOpenServices = { fn ->
                    navController.navigate(Routes.flightDetail(fn))
                }
            )
        }
        composable(
            route = Routes.FLIGHT_DETAIL_PATTERN,
            arguments = listOf(navArgument("flightNumber") { type = NavType.StringType })
        ) { entry ->
            val flightNumber = entry.arguments?.getString("flightNumber") ?: ""
            FlightDetailScreen(
                flightNumber = flightNumber,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainShell(navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryPurple,
                            selectedTextColor = PrimaryPurple,
                            unselectedIconColor = OnSurfaceDim,
                            unselectedTextColor = OnSurfaceDim,
                            indicatorColor = PrimaryPurple.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundGray)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onOpenChat = { flight ->
                        navController.navigate(Routes.chat(flight ?: "timeline"))
                    },
                    onOpenFlightInfo = { flightNumber ->
                        navController.navigate(Routes.flightInfo(flightNumber))
                    }
                )
                1 -> IndoorMapScreen(
                    onChatClicked = { navController.navigate(Routes.chat("timeline")) },
                    onHelpClicked = { navController.navigate(Routes.chat("help")) }
                )
                2 -> ChatScreen(
                    flightNumber = null,
                    onBack = { selectedTab = 0 }
                )
                3 -> FlightDetailScreen(
                    flightNumber = "explore",
                    onBack = { selectedTab = 0 }
                )
            }
        }
    }
}
