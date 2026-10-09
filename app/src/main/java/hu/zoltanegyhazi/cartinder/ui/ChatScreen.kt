package hu.zoltanegyhazi.cartinder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.zoltanegyhazi.cartinder.R
import hu.zoltanegyhazi.cartinder.data.AppState
import hu.zoltanegyhazi.cartinder.data.Car
import hu.zoltanegyhazi.cartinder.data.Match
import hu.zoltanegyhazi.cartinder.data.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: AppState,
    match: Match,
    onSend: (Car, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val car = match.car
    val messages = state.messages(car.id)
    val typing = state.isTyping(car.id)
    var draft by rememberSaveable(car.id) { mutableStateOf("") }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val send = {
        if (draft.isNotBlank()) {
            onSend(car, draft)
            draft = ""
        }
    }

    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), "Vissza")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CarAvatar(match, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(car.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                if (typing) "gépel…" else "${car.city} · ${car.year}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (typing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDetails = true }) {
                        Icon(painterResource(R.drawable.ic_info), "Adatlap")
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Írj valami kedveset…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = send, enabled = draft.isNotBlank()) {
                        Icon(painterResource(R.drawable.ic_send), "Küldés")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (typing) {
                item(key = "typing") { Bubble("• • •", fromMe = false, italic = true) }
            }
            items(messages.asReversed(), key = { "${it.sentAt}-${it.fromMe}-${it.text.hashCode()}" }) { message ->
                Bubble(message)
            }
            item(key = "header") {
                Text(
                    "Matcheltetek ${car.title} autóval${if (match.superLike) " – szuper like-kal! ★" else "."}",
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showDetails) {
        CarDetailDialog(
            car = car,
            onDismiss = { showDetails = false },
            onUnmatch = {
                showDetails = false
                state.unmatch(car)
                onBack()
            },
        )
    }
}

@Composable
private fun Bubble(message: Message) = Bubble(message.text, message.fromMe)

@Composable
private fun Bubble(text: String, fromMe: Boolean, italic: Boolean = false) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = if (fromMe) {
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            } else {
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            },
            color = if (fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text,
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (italic) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
