package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import hu.zoltanegyhazi.cartinder.data.Car

// A Wikimedia elutasítja az általános User-Agenttel érkező kéréseket.
private val wikimediaHeaders = NetworkHeaders.Builder()
    .set("User-Agent", "carTInder/1.0 (Android hobby project)")
    .build()

/** Az autó fotója; amíg tölt (vagy ha nincs net), a rajzolt autó látszik. */
@Composable
fun CarPhoto(car: Car, modifier: Modifier = Modifier, placeholderPadding: Dp = 16.dp) {
    val context = LocalContext.current
    val request = remember(car.id) {
        ImageRequest.Builder(context)
            .data(car.photoUrl)
            .httpHeaders(wikimediaHeaders)
            .crossfade(true)
            .build()
    }
    var loaded by remember(car.id) { mutableStateOf(false) }

    Box(modifier.background(Color(car.background))) {
        if (!loaded) {
            CarArt(car, Modifier.fillMaxWidth().align(Alignment.Center).padding(placeholderPadding))
        }
        AsyncImage(
            model = request,
            contentDescription = car.title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true },
        )
    }
}
