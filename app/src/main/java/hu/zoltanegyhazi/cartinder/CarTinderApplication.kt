package hu.zoltanegyhazi.cartinder

import android.app.Application
import androidx.room.Room
import hu.zoltanegyhazi.cartinder.data.db.CarTinderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CarTinderApplication : Application() {
    val database: CarTinderDatabase by lazy {
        Room.databaseBuilder(this, CarTinderDatabase::class.java, "cartinder.db").build()
    }

    /** Egyszálú scope az adatbázis-írásokhoz; túléli a ViewModelt. */
    val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
}
