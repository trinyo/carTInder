package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.resolvePhotoUrl
import hu.zoltanegyhazi.cartinder.data.title

/** A szerver címe a relatív fotó URL-ekhez. */
val LocalServerUrl = compositionLocalOf { "" }

// A Wikimedia elutasítja az általános User-Agenttel érkező kéréseket.
private val photoHeaders = NetworkHeaders.Builder()
    .set("User-Agent", "carTInder/1.0 (Android hobby project)")
    .build()

/**
 * A hirdetés fotója; amíg tölt, vagy ha nincs fotó, a rajzolt karosszéria látszik.
 * [showCredit] esetén a kép aljára kerül a szerző és a licenc.
 */
@Composable
fun CarPhoto(
    listing: ListingDto,
    modifier: Modifier = Modifier,
    placeholderPadding: Dp = 16.dp,
    showCredit: Boolean = false,
) {
    val context = LocalContext.current
    val url = resolvePhotoUrl(LocalServerUrl.current, listing.photoUrl)
    val request = remember(url) {
        url?.let {
            ImageRequest.Builder(context).data(it).httpHeaders(photoHeaders).crossfade(true).build()
        }
    }
    var loaded by remember(url) { mutableStateOf(false) }

    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!loaded) {
            CarArt(listing.body, Modifier.fillMaxWidth().align(Alignment.Center).padding(placeholderPadding))
        }
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = listing.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
            )
        }
        if (showCredit && loaded && !listing.photoCredit.isNullOrBlank()) {
            Text(
                "📷 ${listing.photoCredit}",
                Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
