package hu.zoltanegyhazi.cartinder.data

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

enum class SwipeDirection { LEFT, RIGHT, UP }

data class CarFilter(
    val maxPrice: Int = MAX_PRICE,
    val maxKm: Int = MAX_KM,
    val fuels: Set<Fuel> = Fuel.entries.toSet(),
) {
    companion object {
        const val MAX_PRICE = 25_000_000
        const val MAX_KM = 450_000
    }
}

fun Car.passes(filter: CarFilter): Boolean =
    priceHuf <= filter.maxPrice && km <= filter.maxKm && fuel in filter.fuels

data class Swipe(val carId: Int, val direction: SwipeDirection, val matched: Boolean)

data class Match(val car: Car, val superLike: Boolean)

data class Message(val carId: Int, val fromMe: Boolean, val text: String, val sentAt: Long)

/** Az induláskor az adatbázisból betöltött állapot. */
data class AppSnapshot(
    val swipes: List<Swipe> = emptyList(),
    val messages: List<Message> = emptyList(),
    val filter: CarFilter = CarFilter(),
)

/** Minden változásról értesül, hogy elmenthesse. */
interface AppStore {
    fun swipeAdded(swipe: Swipe) {}
    fun swipeRemoved(carId: Int) {}
    fun unmatched(carId: Int) {}
    fun messageAdded(message: Message) {}
    fun filterChanged(filter: CarFilter) {}
    fun cleared() {}
}

class AppState(
    private val cars: List<Car> = sampleCars,
    snapshot: AppSnapshot = AppSnapshot(),
    private val store: AppStore = object : AppStore {},
    private val random: Random = Random.Default,
    private val roll: () -> Float = { random.nextFloat() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val history = mutableStateListOf<Swipe>().apply { addAll(snapshot.swipes) }
    private val messagesByCar = mutableStateMapOf<Int, List<Message>>().apply {
        putAll(snapshot.messages.groupBy { it.carId })
    }
    private val typingCars = mutableStateMapOf<Int, Boolean>()
    private var currentFilter by mutableStateOf(snapshot.filter)

    var filter: CarFilter
        get() = currentFilter
        set(value) {
            if (value == currentFilter) return
            currentFilter = value
            store.filterChanged(value)
        }

    var pendingMatch by mutableStateOf<Match?>(null)

    val deck: List<Car> by derivedStateOf {
        val seen = history.mapTo(HashSet()) { it.carId }
        cars.filter { it.id !in seen && it.passes(currentFilter) }
    }

    /** Legfrissebb match legelöl. */
    val matches: List<Match> by derivedStateOf {
        history.asReversed().filter { it.matched }.mapNotNull { swipe ->
            carById(swipe.carId)?.let { Match(it, swipe.direction == SwipeDirection.UP) }
        }
    }

    val swipeCount get() = history.size
    val likeCount get() = history.count { it.direction != SwipeDirection.LEFT }
    val canUndo get() = history.isNotEmpty()

    fun carById(id: Int) = cars.firstOrNull { it.id == id }

    fun isMatched(carId: Int) = history.any { it.carId == carId && it.matched }

    fun messages(carId: Int): List<Message> = messagesByCar[carId].orEmpty()

    fun isTyping(carId: Int) = typingCars[carId] == true

    fun setTyping(carId: Int, typing: Boolean) {
        if (typing) typingCars[carId] = true else typingCars.remove(carId)
    }

    fun swipe(car: Car, direction: SwipeDirection) {
        val matched = when (direction) {
            SwipeDirection.LEFT -> false
            SwipeDirection.UP -> true
            SwipeDirection.RIGHT -> roll() >= car.pickiness
        }
        val swipe = Swipe(car.id, direction, matched)
        history += swipe
        store.swipeAdded(swipe)
        if (matched) {
            addMessage(Message(car.id, fromMe = false, text = car.opener, sentAt = clock()))
            pendingMatch = Match(car, direction == SwipeDirection.UP)
        }
    }

    fun undo() {
        val last = history.removeLastOrNull() ?: return
        messagesByCar.remove(last.carId)
        typingCars.remove(last.carId)
        store.swipeRemoved(last.carId)
    }

    fun unmatch(car: Car) {
        val index = history.indexOfLast { it.carId == car.id }
        if (index < 0 || !history[index].matched) return
        history[index] = history[index].copy(matched = false)
        messagesByCar.remove(car.id)
        typingCars.remove(car.id)
        store.unmatched(car.id)
    }

    fun sendMessage(car: Car, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !isMatched(car.id)) return
        addMessage(Message(car.id, fromMe = true, text = trimmed, sentAt = clock()))
    }

    /** Az autó válaszol az utolsó üzenetedre. */
    fun receiveReply(car: Car) {
        if (!isMatched(car.id)) return
        val thread = messages(car.id)
        val lastMine = thread.lastOrNull { it.fromMe }?.text.orEmpty()
        val reply = ChatBot.reply(car, lastMine, thread.count { !it.fromMe }, random)
        addMessage(Message(car.id, fromMe = false, text = reply, sentAt = clock()))
    }

    fun reset() {
        history.clear()
        messagesByCar.clear()
        typingCars.clear()
        pendingMatch = null
        store.cleared()
    }

    private fun addMessage(message: Message) {
        messagesByCar[message.carId] = messages(message.carId) + message
        store.messageAdded(message)
    }
}
