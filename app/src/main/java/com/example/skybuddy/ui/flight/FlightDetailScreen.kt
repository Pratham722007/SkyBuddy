package com.example.skybuddy.ui.flight

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skybuddy.ui.theme.BackgroundGray
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

// ── Data models ─────────────────────────────────────────────
private data class ServiceItem(
    val icon: ImageVector,
    val label: String,
    val description: String? = null
)

private data class ServiceGroup(
    val title: String,
    val items: List<ServiceItem>
)

private val tabs = listOf("Travel", "Shop", "Dine", "Facilities")

// ── Service data for Surat Airport ──────────────────────────
private val travelServices = listOf(
    ServiceGroup("Airport Services", listOf(
        ServiceItem(Icons.Filled.Luggage, "Baggage Storage"),
        ServiceItem(Icons.Filled.Accessible, "Wheelchair & Stroller"),
        ServiceItem(Icons.Filled.Info, "Information Desk"),
        ServiceItem(Icons.Filled.Wifi, "Free Wi-Fi")
    )),
    ServiceGroup("Baggage Services", listOf(
        ServiceItem(Icons.Filled.FindInPage, "Track Baggage"),
        ServiceItem(Icons.Filled.Luggage, "Lost & Found")
    )),
    ServiceGroup("Getting to the City", listOf(
        ServiceItem(Icons.Filled.LocalTaxi, "Taxi / Auto", "Pre-paid taxi counter available"),
        ServiceItem(Icons.Filled.DirectionsCar, "Car Rental"),
        ServiceItem(Icons.Filled.DirectionsBus, "Bus, Train, Taxi"),
        ServiceItem(Icons.Filled.LocalParking, "Airport Parking", "475 car parking capacity")
    ))
)

private val shopServices = listOf(
    ServiceGroup("Shopping", listOf(
        ServiceItem(Icons.Filled.ShoppingBag, "Duty Free Shop"),
        ServiceItem(Icons.Filled.Sell, "Kalamandir Jewellers"),
        ServiceItem(Icons.Filled.ShoppingBag, "Avsar Gift Shop"),
        ServiceItem(Icons.Filled.ShoppingBag, "Souvenir Store")
    )),
    ServiceGroup("Financial Services", listOf(
        ServiceItem(Icons.Filled.Atm, "ATM"),
        ServiceItem(Icons.Filled.AccountBalance, "Currency Exchange")
    ))
)

private val dineServices = listOf(
    ServiceGroup("Restaurants & Cafes", listOf(
        ServiceItem(Icons.Filled.Restaurant, "Gujju's Bistro", "Small plates & snacks"),
        ServiceItem(Icons.Filled.LocalCafe, "Chocolate Trails", "Coffee & desserts"),
        ServiceItem(Icons.Filled.Restaurant, "Airport Restaurant"),
        ServiceItem(Icons.Filled.LocalCafe, "Chai Point")
    ))
)

private val facilityServices = listOf(
    ServiceGroup("Comfort & Convenience", listOf(
        ServiceItem(Icons.Filled.Wc, "Restrooms"),
        ServiceItem(Icons.Filled.Mosque, "Prayer Room"),
        ServiceItem(Icons.Filled.ElectricBolt, "Charging Station"),
        ServiceItem(Icons.Filled.ChildFriendly, "Baby Care Room")
    )),
    ServiceGroup("Navigation", listOf(
        ServiceItem(Icons.Filled.Map, "Airport Map"),
        ServiceItem(Icons.Filled.MeetingRoom, "Gate Information"),
        ServiceItem(Icons.Filled.Flight, "Check-in Counters", "20 counters available")
    ))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlightDetailScreen(
    flightNumber: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentGroups = when (selectedTab) {
        0 -> travelServices
        1 -> shopServices
        2 -> dineServices
        3 -> facilityServices
        else -> travelServices
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurfaceDark)
            }

            // Tab chips
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedTab
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryPurple else OnSurfaceDim,
                        animationSpec = tween(200),
                        label = "chipText"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedTab = index }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            tab,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = textColor
                        )
                    }
                }
            }

            IconButton(onClick = { /* Share */ }) {
                Icon(Icons.Filled.Share, "Share", tint = OnSurfaceDim)
            }
        }

        // ── Content ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            items(currentGroups) { group ->
                ServiceGroupCard(group)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceGroupCard(group: ServiceGroup) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Text(
                group.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceDark
            )
            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                group.items.forEach { item ->
                    ServiceItemRow(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceItemRow(
    item: ServiceItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.label, tint = OnSurfaceDark, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                item.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnSurfaceDark
            )
            item.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
        }
    }
}
