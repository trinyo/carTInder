package hu.zoltanegyhazi.cartinder.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.zoltanegyhazi.cartinder.CarTinderViewModel
import hu.zoltanegyhazi.cartinder.R
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.formatKm
import hu.zoltanegyhazi.cartinder.data.title
import hu.zoltanegyhazi.cartinder.ui.theme.LikeGreen
import hu.zoltanegyhazi.cartinder.ui.theme.NopeRed
import hu.zoltanegyhazi.cartinder.ui.theme.SuperBlue
import hu.zoltanegyhazi.cartinder.ui.theme.TinderGradient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private class SwipeCardState {
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)
    var busy by mutableStateOf(false)

    suspend fun fling(direction: SwipeDirection, width: Float, height: Float) {
        busy = true
        coroutineScope {
            when (direction) {
                SwipeDirection.LEFT -> launch { offsetX.animateTo(-width * 1.5f, tween(280)) }
                SwipeDirection.RIGHT -> launch { offsetX.animateTo(width * 1.5f, tween(280)) }
                SwipeDirection.UP -> launch { offsetY.animateTo(-height * 1.5f, tween(320)) }
            }
        }
    }

    suspend fun reset() = coroutineScope {
        launch { offsetX.animateTo(0f, spring()) }
        launch { offsetY.animateTo(0f, spring()) }
    }
}

@Composable
fun DiscoverScreen(vm: CarTinderViewModel, onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    val deck = vm.feed
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val top = deck.firstOrNull()
        val cardState = remember(top?.id) { SwipeCardState() }
        val swipe by rememberUpdatedState<(SwipeDirection) -> Unit> { dir ->
            if (top != null && !cardState.busy) {
                scope.launch {
                    cardState.fling(dir, width, height)
                    vm.swipe(top, dir)
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Logo(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))

            // Fekvő nézetben és tableten se nyúljon szét a kártya.
            Box(Modifier.weight(1f).widthIn(max = 520.dp).fillMaxWidth().align(Alignment.CenterHorizontally)) {
                if (top == null) {
                    EmptyDeck(
                        loading = vm.feedLoading,
                        error = vm.feedError,
                        onOpenProfile = onOpenProfile,
                        onRefresh = { vm.refreshFeed() },
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    deck.getOrNull(1)?.let { next ->
                        key(next.id) {
                            CarCard(
                                next,
                                Modifier.fillMaxSize().graphicsLayer {
                                    val p = (abs(cardState.offsetX.value) / (width * 0.5f) +
                                        abs(cardState.offsetY.value) / (height * 0.4f)).coerceIn(0f, 1f)
                                    val scale = 0.93f + 0.07f * p
                                    scaleX = scale
                                    scaleY = scale
                                },
                            )
                        }
                    }
                    key(top.id) {
                        CarCard(
                            top,
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = cardState.offsetX.value
                                    translationY = cardState.offsetY.value
                                    rotationZ = cardState.offsetX.value / width * 18f
                                }
                                .pointerInput(top.id) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val x = cardState.offsetX.value
                                            val y = cardState.offsetY.value
                                            when {
                                                x > width * 0.28f -> swipe(SwipeDirection.RIGHT)
                                                x < -width * 0.28f -> swipe(SwipeDirection.LEFT)
                                                y < -height * 0.18f && abs(y) > abs(x) -> swipe(SwipeDirection.UP)
                                                else -> scope.launch { cardState.reset() }
                                            }
                                        },
                                        onDragCancel = { scope.launch { cardState.reset() } },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            scope.launch {
                                                cardState.offsetX.snapTo(cardState.offsetX.value + amount.x)
                                                cardState.offsetY.snapTo(cardState.offsetY.value + amount.y)
                                            }
                                        },
                                    )
                                },
                        ) {
                            SwipeStamps(cardState, width, height)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundAction(R.drawable.ic_undo, "Visszavonás", Color(0xFFFFB300), 48.dp, vm.undoable > 0) { vm.undo() }
                RoundAction(R.drawable.ic_close, "Nem kell", NopeRed, 64.dp, top != null) { swipe(SwipeDirection.LEFT) }
                RoundAction(R.drawable.ic_star, "Szuper like", SuperBlue, 52.dp, top != null) { swipe(SwipeDirection.UP) }
                RoundAction(R.drawable.ic_heart, "Tetszik", LikeGreen, 64.dp, top != null) { swipe(SwipeDirection.RIGHT) }
            }
        }
    }
}

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).background(TinderGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_car), null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            "carTInder",
            style = MaterialTheme.typography.headlineSmall.copy(brush = TinderGradient),
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarCard(car: ListingDto, modifier: Modifier = Modifier, overlay: @Composable BoxScope.() -> Unit = {}) {
    Card(
        modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    CarPhoto(car, Modifier.fillMaxSize(), placeholderPadding = 20.dp, showCredit = true)
                    if (car.distanceKm > 0) {
                        Pill("📍 ${car.distanceKm} km-re", Modifier.align(Alignment.TopEnd).padding(14.dp))
                    }
                }
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            car.title,
                            Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "  ${car.year}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatHuf(car.priceHuf),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpecChip(formatKm(car.km))
                        SpecChip("${car.horsepower} LE")
                        SpecChip(car.fuel.label)
                        SpecChip(car.body.label)
                        SpecChip(car.city)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eladó: ${car.sellerName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        car.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            overlay()
        }
    }
}

@Composable
private fun BoxScope.SwipeStamps(cardState: SwipeCardState, width: Float, height: Float) {
    val x = cardState.offsetX.value
    val y = cardState.offsetY.value
    val like = (x / (width * 0.25f)).coerceIn(0f, 1f)
    val nope = (-x / (width * 0.25f)).coerceIn(0f, 1f)
    val superLike = if (abs(y) > abs(x)) (-y / (height * 0.15f)).coerceIn(0f, 1f) else 0f
    Stamp("TETSZIK", LikeGreen, -18f, like, Modifier.align(Alignment.TopStart).padding(28.dp))
    Stamp("NEM KELL", NopeRed, 18f, nope, Modifier.align(Alignment.TopEnd).padding(28.dp))
    Stamp("SZUPER", SuperBlue, -8f, superLike, Modifier.align(Alignment.BottomCenter).padding(bottom = 160.dp))
}

@Composable
private fun Stamp(text: String, color: Color, angle: Float, alpha: Float, modifier: Modifier) {
    if (alpha <= 0f) return
    Text(
        text,
        modifier
            .graphicsLayer { this.alpha = alpha }
            .rotate(angle)
            .border(4.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = color,
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SpecChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun RoundAction(
    @DrawableRes icon: Int,
    description: String,
    color: Color,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.35f
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = color.copy(alpha = alpha),
        shadowElevation = if (enabled) 6.dp else 1.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f * alpha)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), description, Modifier.size(size * 0.48f))
        }
    }
}

@Composable
private fun EmptyDeck(
    loading: Boolean,
    error: String?,
    onOpenProfile: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            loading -> CircularProgressIndicator()
            error != null -> {
                Text("📡", fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text(error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRefresh) { Text("Újra") }
            }
            else -> {
                Text("🚗💨", fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Elfogytak az autók a környéken",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Lazíts a szűrőkön, vagy nézz vissza később – folyamatosan jönnek az új hirdetések.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onOpenProfile) { Text("Szűrők") }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onRefresh) { Text("Frissítés") }
            }
        }
    }
}
