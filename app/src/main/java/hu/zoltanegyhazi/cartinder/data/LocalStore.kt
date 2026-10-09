package hu.zoltanegyhazi.cartinder.data

import android.content.Context
import androidx.core.content.edit
import hu.zoltanegyhazi.cartinder.data.api.MessageDto
import hu.zoltanegyhazi.cartinder.data.db.CachedMessageEntity
import hu.zoltanegyhazi.cartinder.data.db.CarTinderDao
import hu.zoltanegyhazi.cartinder.data.db.SettingsEntity

/** Bejelentkezési adatok; a tesztekben memóriabeli változattal cserélhető. */
interface SessionStore {
    var token: String?
    var serverUrl: String
}

class PrefsSessionStore(context: Context, private val defaultServerUrl: String) : SessionStore {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    override var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit { putString("token", value) }

    override var serverUrl: String
        get() = prefs.getString("serverUrl", null) ?: defaultServerUrl
        set(value) = prefs.edit { putString("serverUrl", value) }
}

/** Helyi adatok: szűrők és a chat-gyorsítótár. */
interface LocalCache {
    suspend fun loadFilter(): CarFilter
    suspend fun saveFilter(filter: CarFilter)
    suspend fun messages(matchId: Int): List<MessageDto>
    suspend fun saveMessages(messages: List<MessageDto>)
    suspend fun deleteMessages(matchId: Int)
    suspend fun clear()
}

class RoomLocalCache(private val dao: CarTinderDao) : LocalCache {
    override suspend fun loadFilter(): CarFilter = dao.settings()?.let { s ->
        CarFilter(
            maxPrice = s.maxPrice,
            maxKm = s.maxKm,
            fuels = s.fuels.split(',').mapNotNull { name -> Fuel.entries.firstOrNull { it.name == name } }.toSet()
                .ifEmpty { Fuel.entries.toSet() },
        )
    } ?: CarFilter()

    override suspend fun saveFilter(filter: CarFilter) =
        dao.saveSettings(SettingsEntity(maxPrice = filter.maxPrice, maxKm = filter.maxKm, fuels = filter.fuels.joinToString(",") { it.name }))

    override suspend fun messages(matchId: Int) = dao.messages(matchId).map {
        MessageDto(it.id, it.matchId, it.fromMe, it.text, it.createdAt)
    }

    override suspend fun saveMessages(messages: List<MessageDto>) =
        dao.saveMessages(messages.map { CachedMessageEntity(it.id, it.matchId, it.fromMe, it.text, it.createdAt) })

    override suspend fun deleteMessages(matchId: Int) = dao.deleteMessages(matchId)

    override suspend fun clear() = dao.deleteAllMessages()
}
