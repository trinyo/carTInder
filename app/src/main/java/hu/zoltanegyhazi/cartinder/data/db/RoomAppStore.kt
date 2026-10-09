package hu.zoltanegyhazi.cartinder.data.db

import hu.zoltanegyhazi.cartinder.data.AppSnapshot
import hu.zoltanegyhazi.cartinder.data.AppStore
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.Message
import hu.zoltanegyhazi.cartinder.data.Swipe
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A memóriabeli állapot változásait írja ki az adatbázisba.
 * A [scope] egyszálú dispatcheren fut, így az írások sorrendje megmarad.
 */
class RoomAppStore(private val dao: CarTinderDao, private val scope: CoroutineScope) : AppStore {

    suspend fun load(): AppSnapshot = AppSnapshot(
        swipes = dao.swipes().map { Swipe(it.carId, SwipeDirection.valueOf(it.direction), it.matched) },
        messages = dao.messages().map { Message(it.carId, it.fromMe, it.text, it.sentAt) },
        filter = dao.settings()?.let { s ->
            CarFilter(
                maxPrice = s.maxPrice,
                maxKm = s.maxKm,
                fuels = s.fuels.split(',').filter { it.isNotEmpty() }.map { Fuel.valueOf(it) }.toSet(),
            )
        } ?: CarFilter(),
    )

    override fun swipeAdded(swipe: Swipe) = write {
        insertSwipe(SwipeEntity(carId = swipe.carId, direction = swipe.direction.name, matched = swipe.matched))
    }

    override fun swipeRemoved(carId: Int) = write { removeSwipe(carId) }

    override fun unmatched(carId: Int) = write { unmatch(carId) }

    override fun messageAdded(message: Message) = write {
        insertMessage(MessageEntity(carId = message.carId, fromMe = message.fromMe, text = message.text, sentAt = message.sentAt))
    }

    override fun filterChanged(filter: CarFilter) = write {
        saveSettings(SettingsEntity(maxPrice = filter.maxPrice, maxKm = filter.maxKm, fuels = filter.fuels.joinToString(",") { it.name }))
    }

    override fun cleared() = write { clearAll() }

    private fun write(block: suspend CarTinderDao.() -> Unit) {
        scope.launch { dao.block() }
    }
}
