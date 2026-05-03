package com.example.skybeacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.skybeacon.data.AppDatabase
import com.example.skybeacon.data.BroadcastLog
import com.example.skybeacon.data.BroadcastMessage
import com.example.skybeacon.data.Offer
import com.example.skybeacon.data.ShopProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(private val database: AppDatabase) : ViewModel() {

    // Shop Profile
    val allShops: Flow<List<ShopProfile>> = database.shopProfileDao().getAllShops()

    fun insertShop(shop: ShopProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.shopProfileDao().insertShop(shop)
        }
    }

    fun updateShop(shop: ShopProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.shopProfileDao().updateShop(shop)
        }
    }

    fun deleteShop(shop: ShopProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.shopProfileDao().deleteShop(shop)
        }
    }

    // Broadcast Messages
    fun getMessagesForShop(shopId: Int): Flow<List<BroadcastMessage>> {
        return database.broadcastMessageDao().getMessagesForShop(shopId)
    }

    // Broadcast Logs
    val recentLogs: Flow<List<BroadcastLog>> = database.broadcastLogDao().getRecentLogs()

    fun insertLog(log: BroadcastLog) {
        viewModelScope.launch(Dispatchers.IO) {
            database.broadcastLogDao().insertLog(log)
        }
    }

    fun insertMessage(message: BroadcastMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            database.broadcastMessageDao().insertMessage(message)
        }
    }

    fun deleteMessage(message: BroadcastMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            database.broadcastMessageDao().deleteMessage(message)
        }
    }

    // Offers
    fun getOffersForShop(shopId: Int): Flow<List<Offer>> {
        return database.offerDao().getOffersForShop(shopId)
    }

    fun insertOffer(offer: Offer) {
        viewModelScope.launch(Dispatchers.IO) {
            database.offerDao().insertOffer(offer)
        }
    }

    fun deleteOffer(offer: Offer) {
        viewModelScope.launch(Dispatchers.IO) {
            database.offerDao().deleteOffer(offer)
        }
    }
}

class MainViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
