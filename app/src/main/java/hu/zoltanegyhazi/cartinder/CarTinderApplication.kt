package hu.zoltanegyhazi.cartinder

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import hu.zoltanegyhazi.cartinder.data.PrefsSessionStore
import hu.zoltanegyhazi.cartinder.data.RoomLocalCache
import hu.zoltanegyhazi.cartinder.data.api.ApiClient
import hu.zoltanegyhazi.cartinder.data.db.CarTinderDatabase
import io.ktor.client.engine.okhttp.OkHttp

class CarTinderApplication : Application() {
    val database: CarTinderDatabase by lazy {
        Room.databaseBuilder(this, CarTinderDatabase::class.java, "cartinder.db")
            .addMigrations(CarTinderDatabase.MIGRATION_1_2)
            .build()
    }

    val session by lazy { PrefsSessionStore(this, BuildConfig.API_BASE_URL) }

    val backend by lazy {
        ApiClient(
            OkHttp.create(),
            baseUrl = { session.serverUrl },
            token = { session.token },
            onNetworkError = { Log.w("CarTinder", "Hálózati hiba", it) },
        )
    }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer { CarTinderViewModel(backend, session, RoomLocalCache(database.dao())) }
        }
    }
}
