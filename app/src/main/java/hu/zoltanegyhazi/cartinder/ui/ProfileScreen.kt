package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.zoltanegyhazi.cartinder.data.AppState
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.formatKm

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(state: AppState, modifier: Modifier = Modifier) {
    val filter = state.filter
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Logo(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Swipe", state.swipeCount, Modifier.weight(1f))
            StatCard("Like", state.likeCount, Modifier.weight(1f))
            StatCard("Match", state.matches.size, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Mit keresel?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Label("Maximum ár", formatHuf(filter.maxPrice))
        Slider(
            value = filter.maxPrice.toFloat(),
            onValueChange = { state.filter = filter.copy(maxPrice = roundTo(it, 100_000)) },
            valueRange = 500_000f..CarFilter.MAX_PRICE.toFloat(),
        )

        Label("Maximum futásteljesítmény", formatKm(filter.maxKm))
        Slider(
            value = filter.maxKm.toFloat(),
            onValueChange = { state.filter = filter.copy(maxKm = roundTo(it, 5_000)) },
            valueRange = 50_000f..CarFilter.MAX_KM.toFloat(),
        )

        Spacer(Modifier.height(8.dp))
        Text("Üzemanyag", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fuel.entries.forEach { fuel ->
                val selected = fuel in filter.fuels
                FilterChip(
                    selected = selected,
                    onClick = {
                        state.filter = filter.copy(fuels = if (selected) filter.fuels - fuel else filter.fuels + fuel)
                    },
                    label = { Text(fuel.label) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${state.deck.size} autó vár rád a szűrőid alapján.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { state.filter = CarFilter() }, modifier = Modifier.fillMaxWidth()) {
            Text("Szűrők alaphelyzetbe")
        }
        OutlinedButton(onClick = state::reset, modifier = Modifier.fillMaxWidth()) {
            Text("Minden swipe és match törlése")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Fotók: Wikimedia Commons (CC BY / CC BY-SA / közkincs). A szerzők és licencek a fotók Commons-oldalán találhatók.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun roundTo(value: Float, step: Int) = (Math.round(value / step) * step)

@Composable
private fun Label(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
