package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.zoltanegyhazi.cartinder.data.AppState
import hu.zoltanegyhazi.cartinder.data.Car
import hu.zoltanegyhazi.cartinder.data.Match
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.formatKm
import hu.zoltanegyhazi.cartinder.ui.theme.SuperBlue

@Composable
fun MatchesScreen(state: AppState, onOpenChat: (Int) -> Unit, modifier: Modifier = Modifier) {
    val matches = state.matches

    if (matches.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("💔", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Még nincs match", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Húzz jobbra pár autót a Felfedezés fülön!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            SectionTitle("Új matchek")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(matches, key = { it.car.id }) { match ->
                    Column(
                        Modifier.width(84.dp).clickable { onOpenChat(match.car.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CarAvatar(match, 76.dp)
                        Text(
                            match.car.make,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionTitle("Üzenetek")
        }
        items(matches, key = { "msg-${it.car.id}" }) { match ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenChat(match.car.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CarAvatar(match, 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(match.car.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val last = state.messages(match.car.id).lastOrNull()
                    Text(
                        when {
                            state.isTyping(match.car.id) -> "gépel…"
                            last == null -> "Írj neki először!"
                            last.fromMe -> "Te: ${last.text}"
                            else -> last.text
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (last?.fromMe == false) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (last?.fromMe == false) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(start = 90.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun CarAvatar(match: Match, size: Dp) {
    Box {
        CarPhoto(match.car, Modifier.size(size).clip(CircleShape), placeholderPadding = 6.dp)
        if (match.superLike) {
            Text(
                "★",
                Modifier
                    .align(Alignment.BottomEnd)
                    .background(SuperBlue, CircleShape)
                    .padding(horizontal = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarDetailDialog(car: Car, onDismiss: () -> Unit, onUnmatch: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${car.title}, ${car.year}") },
        text = {
            Column {
                CarPhoto(
                    car,
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp)),
                    placeholderPadding = 12.dp,
                )
                Spacer(Modifier.height(12.dp))
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
                    SpecChip("📍 ${car.city}")
                }
                Spacer(Modifier.height(12.dp))
                Text(car.bio, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                ) {
                    Text(car.opener, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Bezár") } },
        dismissButton = {
            TextButton(onClick = onUnmatch) { Text("Unmatch", color = MaterialTheme.colorScheme.error) }
        },
    )
}
