package hu.zoltanegyhazi.cartinder.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Entity(tableName = "swipes")
data class SwipeEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val carId: Int,
    val direction: String,
    val matched: Boolean,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Int,
    val fromMe: Boolean,
    val text: String,
    val sentAt: Long,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val maxPrice: Int,
    val maxKm: Int,
    /** Üzemanyag enum nevek vesszővel elválasztva. */
    val fuels: String,
)

@Dao
interface CarTinderDao {
    @Query("SELECT * FROM swipes ORDER BY seq")
    suspend fun swipes(): List<SwipeEntity>

    @Query("SELECT * FROM messages ORDER BY id")
    suspend fun messages(): List<MessageEntity>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun settings(): SettingsEntity?

    @Insert
    suspend fun insertSwipe(swipe: SwipeEntity)

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Upsert
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("DELETE FROM swipes WHERE carId = :carId")
    suspend fun deleteSwipes(carId: Int)

    @Query("UPDATE swipes SET matched = 0 WHERE carId = :carId")
    suspend fun clearMatch(carId: Int)

    @Query("DELETE FROM messages WHERE carId = :carId")
    suspend fun deleteMessages(carId: Int)

    @Query("DELETE FROM swipes")
    suspend fun deleteAllSwipes()

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Transaction
    suspend fun removeSwipe(carId: Int) {
        deleteSwipes(carId)
        deleteMessages(carId)
    }

    @Transaction
    suspend fun unmatch(carId: Int) {
        clearMatch(carId)
        deleteMessages(carId)
    }

    @Transaction
    suspend fun clearAll() {
        deleteAllSwipes()
        deleteAllMessages()
    }
}

@Database(
    entities = [SwipeEntity::class, MessageEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CarTinderDatabase : RoomDatabase() {
    abstract fun dao(): CarTinderDao
}
