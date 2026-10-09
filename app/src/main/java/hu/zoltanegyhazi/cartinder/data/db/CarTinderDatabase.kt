package hu.zoltanegyhazi.cartinder.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val maxPrice: Int,
    val maxKm: Int,
    /** Üzemanyag enum nevek vesszővel elválasztva. */
    val fuels: String,
)

/** A szerverről letöltött üzenetek helyi másolata, hogy a chat azonnal megnyíljon. */
@Entity(tableName = "cached_messages", indices = [Index("matchId")])
data class CachedMessageEntity(
    @PrimaryKey val id: Int,
    val matchId: Int,
    val fromMe: Boolean,
    val text: String,
    val createdAt: Long,
)

@Dao
interface CarTinderDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun settings(): SettingsEntity?

    @Upsert
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM cached_messages WHERE matchId = :matchId ORDER BY id")
    suspend fun messages(matchId: Int): List<CachedMessageEntity>

    @Upsert
    suspend fun saveMessages(messages: List<CachedMessageEntity>)

    @Query("DELETE FROM cached_messages WHERE matchId = :matchId")
    suspend fun deleteMessages(matchId: Int)

    @Query("DELETE FROM cached_messages")
    suspend fun deleteAllMessages()
}

@Database(
    entities = [SettingsEntity::class, CachedMessageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CarTinderDatabase : RoomDatabase() {
    abstract fun dao(): CarTinderDao

    companion object {
        /** 1 → 2: a swipe-ok és üzenetek a szerverre költöztek; a szűrők megmaradnak. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `swipes`")
                db.execSQL("DROP TABLE IF EXISTS `messages`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_messages` (`id` INTEGER NOT NULL, `matchId` INTEGER NOT NULL, " +
                        "`fromMe` INTEGER NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_messages_matchId` ON `cached_messages` (`matchId`)")
            }
        }
    }
}
