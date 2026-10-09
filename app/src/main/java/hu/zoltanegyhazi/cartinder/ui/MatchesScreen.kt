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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import hu.zoltanegyhazi.cartinder.CarTinderViewModel
import hu.zoltanegyhazi.cartinder.data.api.IncomingLikeDto
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.api.MatchDto
import hu.zoltanegyhazi.cartinder.data.api.Role
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.formatKm
import hu.zoltanegyhazi.cartinder.data.title
import hu.zoltanegyhazi.cartinder.ui.theme.SuperBlue

@Composable
fun MatchesScreen(vm: CarTinderViewModel, onOpenChat: (Int) -> Unit, modifier: Modifier = Modifier) {
    val matches = vm.matches
    val likes = vm.likes

    if (matches.isEmpty() && likes.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("💔", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Még nincs match", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Húzz jobbra pár autót a Felfedezés fülön, vagy adj fel egy hirdetést a Profil fülön!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp)) {
        if (likes.isNotEmpty()) {
            item { SectionTitle("Érdeklődők a hirdetéseidre (${likes.size})") }
            items(likes, key = { "like-${it.id}" }) { like ->
                IncomingLikeRow(like, onAccept = { vm.acceptLike(like) }, onDecline = { vm.declineLike(like) })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        val fresh = matches.filter { it.lastMessage == null || it.lastMessage.fromMe.not() && it.unread > 0 }
        if (fresh.isNotEmpty()) {
            item {
                SectionTitle("Új matchek")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(fresh, key = { "new-${it.id}" }) { match ->
                        Column(
                            Modifier.width(84.dp).clickable { onOpenChat(match.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MatchAvatar(match, 76.dp)
                            Text(
                                if (match.role == Role.BUYER) match.listing.make else match.other.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (matches.isNotEmpty()) {
            item { SectionTitle("Üzenetek") }
            items(matches, key = { "msg-${it.id}" }) { match ->
                ConversationRow(match, isTyping = vm.isTyping(match.id), onClick = { onOpenChat(match.id) })
                HorizontalDivider(Modifier.padding(start = 90.dp))
            }
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
private fun IncomingLikeRow(like: IncomingLikeDto, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CarPhoto(like.listing, Modifier.size(56.dp).clip(CircleShape), placeholderPadding = 4.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(like.buyer.name)
                        if (like.superLike) append(" ★")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (like.superLike) SuperBlue else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${if (like.superLike) "Szuper like" else "Tetszik neki"}: ${like.listing.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val about = listOf(like.buyer.city, like.buyer.currentCar.takeIf { it.isNotBlank() }?.let { "most: $it" })
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                if (about.isNotEmpty()) {
                    Text(about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            OutlinedButton(onClick = onDecline) { Text("Elutasít") }
            FilledTonalButton(onClick = onAccept) { Text("Elfogad") }
        }
    }
}

@Composable
private fun ConversationRow(match: MatchDto, isTyping: Boolean, onClick: () -> Unit) {
    val last = match.lastMessage
    val unread = match.unread > 0
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MatchAvatar(match, 60.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    match.chatTitle,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatMessageTime(last?.createdAt ?: match.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        isTyping -> "gépel…"
                        last == null -> "Írj neki először!"
                        last.fromMe -> "Te: ${last.text}"
                        else -> last.text
                    },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isTyping -> MaterialTheme.colorScheme.primary
                        unread -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Badge { Text("${match.unread}") }
                }
            }
        }
    }
}

/** Vevőként az autó, eladóként a vevő neve a beszélgetés címe. */
val MatchDto.chatTitle get() = if (role == Role.BUYER) listing.title else "${other.name} · ${listing.make}"

@Composable
fun MatchAvatar(match: MatchDto, size: Dp) {
    Box {
        CarPhoto(match.listing, Modifier.size(size).clip(CircleShape), placeholderPadding = 6.dp)
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
fun ListingDetailDialog(
    listing: ListingDto,
    onDismiss: () -> Unit,
    destructiveLabel: String? = null,
    onDestructive: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${listing.title}, ${listing.year}") },
        text = {
            Column {
                CarPhoto(
                    listing,
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp)),
                    placeholderPadding = 12.dp,
                    showCredit = true,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    formatHuf(listing.priceHuf),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpecChip(formatKm(listing.km))
                    SpecChip("${listing.horsepower} LE")
                    SpecChip(listing.fuel.label)
                    SpecChip(listing.body.label)
                    SpecChip("📍 ${listing.city}")
                }
                if (listing.bio.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(listing.bio, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Eladó: ${listing.sellerName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Bezár") } },
        dismissButton = destructiveLabel?.let {
            { TextButton(onClick = onDestructive) { Text(it, color = MaterialTheme.colorScheme.error) } }
        },
    )
}
