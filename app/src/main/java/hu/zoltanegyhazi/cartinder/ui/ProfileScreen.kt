package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.zoltanegyhazi.cartinder.CarTinderViewModel
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.api.MeDto
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.formatKm
import hu.zoltanegyhazi.cartinder.data.title
import hu.zoltanegyhazi.cartinder.ui.theme.TinderGradient
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(vm: CarTinderViewModel, onEditListing: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val me = vm.me ?: return
    val filter = vm.filter
    var editingProfile by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Column(Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally)) {
            ProfileHeader(me, onEdit = { editingProfile = true })
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Swipe", me.stats.swipes, Modifier.weight(1f))
                StatCard("Like", me.stats.likes, Modifier.weight(1f))
                StatCard("Match", vm.matches.size, Modifier.weight(1f))
                StatCard("Hirdetés", vm.myListings.size, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hirdetéseim", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { onEditListing(null) }) { Text("+ Új hirdetés") }
            }
            Spacer(Modifier.height(8.dp))
            if (vm.myListings.isEmpty()) {
                Text(
                    "Még nincs hirdetésed. Adj fel egyet, és a vevők elkezdhetik húzni!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            vm.myListings.forEach { listing ->
                MyListingRow(listing, onClick = { onEditListing(listing.id) })
            }

            Spacer(Modifier.height(24.dp))
            Text("Mit keresel?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Label("Maximum ár", if (filter.maxPrice >= CarFilter.MAX_PRICE) "bármennyi" else formatHuf(filter.maxPrice))
            Slider(
                value = filter.maxPrice.toFloat(),
                onValueChange = { vm.updateFilter(filter.copy(maxPrice = roundTo(it, 100_000))) },
                valueRange = 500_000f..CarFilter.MAX_PRICE.toFloat(),
            )

            Label("Maximum futásteljesítmény", if (filter.maxKm >= CarFilter.MAX_KM) "bármennyi" else formatKm(filter.maxKm))
            Slider(
                value = filter.maxKm.toFloat(),
                onValueChange = { vm.updateFilter(filter.copy(maxKm = roundTo(it, 5_000))) },
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
                            val fuels = if (selected) filter.fuels - fuel else filter.fuels + fuel
                            // Legalább egy maradjon, különben üres lenne a feed.
                            if (fuels.isNotEmpty()) vm.updateFilter(filter.copy(fuels = fuels))
                        },
                        label = { Text(fuel.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.updateFilter(CarFilter()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Szűrők alaphelyzetbe")
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) {
                Text("Kijelentkezés")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Szerver: ${vm.serverUrl}\nA demó autók fotói a Wikimedia Commonsról származnak; a szerző és a licenc minden kártyán szerepel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (editingProfile) {
        ProfileEditDialog(
            me = me,
            onDismiss = { editingProfile = false },
            onSave = { name, city, bio, car ->
                vm.updateProfile(name, city, bio, car)
                editingProfile = false
            },
        )
    }
}

@Composable
private fun ProfileHeader(me: MeDto, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(64.dp).background(TinderGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                me.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(me.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                listOf(me.city, me.currentCar.takeIf { it.isNotBlank() }?.let { "🚗 $it" })
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { me.email },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (me.bio.isNotBlank()) {
                Text(me.bio, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick = onEdit) { Text("Szerkeszt") }
    }
}

@Composable
private fun ProfileEditDialog(me: MeDto, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(me.name) }
    var city by rememberSaveable { mutableStateOf(me.city) }
    var bio by rememberSaveable { mutableStateOf(me.bio) }
    var car by rememberSaveable { mutableStateOf(me.currentCar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Név") }, singleLine = true)
                OutlinedTextField(city, { city = it }, label = { Text("Város") }, singleLine = true)
                OutlinedTextField(car, { car = it }, label = { Text("Mivel jársz most?") }, singleLine = true)
                OutlinedTextField(bio, { bio = it }, label = { Text("Bemutatkozás") }, maxLines = 4)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, city, bio, car) }, enabled = name.isNotBlank()) { Text("Mentés") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } },
    )
}

@Composable
private fun MyListingRow(listing: ListingDto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CarPhoto(listing, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), placeholderPadding = 4.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(listing.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatHuf(listing.priceHuf)} · ${listing.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!listing.active) {
                Text("Inaktív", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun roundTo(value: Float, step: Int) = (value / step).roundToInt() * step

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
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
