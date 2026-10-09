package hu.zoltanegyhazi.cartinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.zoltanegyhazi.cartinder.ui.ChatScreen
import hu.zoltanegyhazi.cartinder.ui.DiscoverScreen
import hu.zoltanegyhazi.cartinder.ui.Logo
import hu.zoltanegyhazi.cartinder.ui.MatchOverlay
import hu.zoltanegyhazi.cartinder.ui.MatchesScreen
import hu.zoltanegyhazi.cartinder.ui.ProfileScreen
import hu.zoltanegyhazi.cartinder.ui.theme.CarTInderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarTInderTheme {
                CarTInderApp()
            }
        }
    }
}

@Composable
fun CarTInderApp(viewModel: CarTinderViewModel = viewModel()) {
    val state = viewModel.state
    if (state == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Logo()
        }
        return
    }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DISCOVER) }
    var chatCarId by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            val count = state.matches.size
                            if (it == AppDestinations.MATCHES && count > 0) {
                                BadgedBox(badge = { Badge { Text("$count") } }) {
                                    Icon(painterResource(it.icon), contentDescription = it.label, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Icon(painterResource(it.icon), contentDescription = it.label, modifier = Modifier.size(24.dp))
                            }
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)
                when (currentDestination) {
                    AppDestinations.DISCOVER -> DiscoverScreen(
                        state,
                        onOpenProfile = { currentDestination = AppDestinations.PROFILE },
                        modifier = modifier,
                    )
                    AppDestinations.MATCHES -> MatchesScreen(state, onOpenChat = { chatCarId = it }, modifier = modifier)
                    AppDestinations.PROFILE -> ProfileScreen(state, modifier)
                }
            }
        }

        state.matches.firstOrNull { it.car.id == chatCarId }?.let { match ->
            ChatScreen(
                state,
                match,
                onSend = viewModel::sendMessage,
                onBack = { chatCarId = null },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        }

        state.pendingMatch?.let { match ->
            MatchOverlay(
                match = match,
                onMessage = {
                    state.pendingMatch = null
                    currentDestination = AppDestinations.MATCHES
                    chatCarId = match.car.id
                },
                onDismiss = { state.pendingMatch = null },
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    @param:DrawableRes val icon: Int,
) {
    DISCOVER("Felfedezés", R.drawable.ic_car),
    MATCHES("Matchek", R.drawable.ic_favorite),
    PROFILE("Profil", R.drawable.ic_account_box),
}
