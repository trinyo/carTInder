package hu.zoltanegyhazi.cartinder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.zoltanegyhazi.cartinder.data.AppState
import hu.zoltanegyhazi.cartinder.data.Car
import hu.zoltanegyhazi.cartinder.data.db.RoomAppStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class CarTinderViewModel(application: Application) : AndroidViewModel(application) {
    /** null, amíg az adatbázis betöltődik. */
    var state by mutableStateOf<AppState?>(null)
        private set

    private val pendingReplies = mutableSetOf<Int>()

    init {
        val app = application as CarTinderApplication
        viewModelScope.launch {
            val store = RoomAppStore(app.database.dao(), app.writeScope)
            state = AppState(snapshot = store.load(), store = store)
        }
    }

    fun sendMessage(car: Car, text: String) {
        val state = state ?: return
        state.sendMessage(car, text)
        if (!pendingReplies.add(car.id)) return
        viewModelScope.launch {
            delay(Random.nextLong(600, 1400))
            state.setTyping(car.id, true)
            delay(Random.nextLong(1200, 2600))
            state.setTyping(car.id, false)
            state.receiveReply(car)
            pendingReplies.remove(car.id)
        }
    }
}
