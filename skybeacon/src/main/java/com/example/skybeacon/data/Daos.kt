package com.example.skybeacon.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {
    @Query("SELECT * FROM shop_profiles")
    fun getAllShops(): Flow<List<ShopProfile>>

    @Query("SELECT * FROM shop_profiles WHERE id = :shopId")
    suspend fun getShopById(shopId: Int): ShopProfile?

    @Insert
    suspend fun insertShop(shop: ShopProfile)

    @Update
    suspend fun updateShop(shop: ShopProfile)

    @Delete
    suspend fun deleteShop(shop: ShopProfile)
}

@Dao
interface BroadcastMessageDao {
    @Query("SELECT * FROM broadcast_messages WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getMessagesForShop(shopId: Int): Flow<List<BroadcastMessage>>

    @Query("SELECT * FROM broadcast_messages ORDER BY createdAt DESC LIMIT 5")
    fun getRecentMessages(): Flow<List<BroadcastMessage>>

    @Insert
    suspend fun insertMessage(message: BroadcastMessage)

    @Delete
    suspend fun deleteMessage(message: BroadcastMessage)
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers WHERE shopId = :shopId")
    fun getOffersForShop(shopId: Int): Flow<List<Offer>>

    @Insert
    suspend fun insertOffer(offer: Offer)

    @Update
    suspend fun updateOffer(offer: Offer)

    @Delete
    suspend fun deleteOffer(offer: Offer)
}

@Dao
interface BroadcastLogDao {
    @Query("SELECT * FROM broadcast_logs ORDER BY createdAt DESC LIMIT 10")
    fun getRecentLogs(): Flow<List<BroadcastLog>>

    @Insert
    suspend fun insertLog(log: BroadcastLog)
}
