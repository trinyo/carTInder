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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.zoltanegyhazi.cartinder.ui.AuthScreen
import hu.zoltanegyhazi.cartinder.ui.ChatScreen
import hu.zoltanegyhazi.cartinder.ui.DiscoverScreen
import hu.zoltanegyhazi.cartinder.ui.ListingEditorScreen
import hu.zoltanegyhazi.cartinder.ui.LocalServerUrl
import hu.zoltanegyhazi.cartinder.ui.Logo
import hu.zoltanegyhazi.cartinder.ui.MatchOverlay
import hu.zoltanegyhazi.cartinder.ui.MatchesScreen
import hu.zoltanegyhazi.cartinder.ui.ProfileScreen
import hu.zoltanegyhazi.cartinder.ui.rememberLocalNetworkGate
import hu.zoltanegyhazi.cartinder.ui.theme.CarTInderTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = (application as CarTinderApplication).viewModelFactory
        setContent {
            CarTInderTheme {
                CarTInderApp(viewModel(factory = factory))
            }
        }
    }
}

@Composable
fun CarTInderApp(vm: CarTinderViewModel) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.notice) {
        vm.notice?.let {
            vm.notice = null
            snackbar.showSnackbar(it)
        }
    }

    // Mentett belépésnél, helyi szerverrel: engedély kérése, majd újrapróbálás.
    val localNetworkGate = rememberLocalNetworkGate()
    LaunchedEffect(Unit) {
        if (vm.hasSavedLogin) localNetworkGate(vm.serverUrl) { vm.retrySavedLogin() }
    }

    CompositionLocalProvider(LocalServerUrl provides vm.serverUrl) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (vm.state) {
                Session.Loading -> Logo(Modifier.align(Alignment.Center))
                Session.LoggedOut -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    AuthScreen(vm, Modifier.padding(padding))
                }
                is Session.LoggedIn -> MainContent(vm, snackbar)
            }
        }
    }
}

@Composable
private fun MainContent(vm: CarTinderViewModel, snackbar: SnackbarHostState) {
    var destination by rememberSaveable { mutableStateOf(AppDestinations.DISCOVER) }
    var chatId by rememberSaveable { mutableStateOf<Int?>(null) }
    // null: nincs nyitva, -1: új hirdetés, egyébként a szerkesztett hirdetés azonosítója
    var editingListingId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Matchek, kedvelések és új üzenetek frissítése, amíg az app előtérben van.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                vm.refreshInbox()
                delay(8_000)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            val count = when (it) {
                                AppDestinations.MATCHES -> vm.matches.sumOf { m -> m.unread } + vm.likes.size
                                else -> 0
                            }
                            BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) {
                                Icon(painterResource(it.icon), contentDescription = it.label, modifier = Modifier.size(24.dp))
                            }
                        },
                        label = { Text(it.label) },
                        selected = it == destination,
                        onClick = { destination = it },
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)
                when (destination) {
                    AppDestinations.DISCOVER -> DiscoverScreen(
                        vm,
                        onOpenProfile = { destination = AppDestinations.PROFILE },
                        modifier = modifier,
                    )
                    AppDestinations.MATCHES -> MatchesScreen(vm, onOpenChat = { chatId = it }, modifier = modifier)
                    AppDestinations.PROFILE -> ProfileScreen(
                        vm,
                        onEditListing = { editingListingId = it ?: -1 },
                        modifier = modifier,
                    )
                }
            }
        }

        vm.matches.firstOrNull { it.id == chatId }?.let { match ->
            ChatScreen(
                vm,
                match,
                onBack = { chatId = null },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        }

        editingListingId?.let { id ->
            ListingEditorScreen(
                vm,
                listing = vm.myListings.firstOrNull { it.id == id },
                onClose = { editingListingId = null },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        }

        vm.pendingMatch?.let { match ->
            MatchOverlay(
                match = match,
                onMessage = {
                    vm.pendingMatch = null
                    destination = AppDestinations.MATCHES
                    chatId = match.id
                },
                onDismiss = { vm.pendingMatch = null },
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
