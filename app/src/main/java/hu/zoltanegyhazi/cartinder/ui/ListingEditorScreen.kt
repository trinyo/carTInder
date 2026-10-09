package hu.zoltanegyhazi.cartinder.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hu.zoltanegyhazi.cartinder.CarTinderViewModel
import hu.zoltanegyhazi.cartinder.R
import hu.zoltanegyhazi.cartinder.data.BodyType
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.api.ListingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListingEditorScreen(
    vm: CarTinderViewModel,
    listing: ListingDto?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var make by rememberSaveable { mutableStateOf(listing?.make.orEmpty()) }
    var model by rememberSaveable { mutableStateOf(listing?.model.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(listing?.year?.toString().orEmpty()) }
    var price by rememberSaveable { mutableStateOf(listing?.priceHuf?.toString().orEmpty()) }
    var km by rememberSaveable { mutableStateOf(listing?.km?.toString().orEmpty()) }
    var hp by rememberSaveable { mutableStateOf(listing?.horsepower?.toString().orEmpty()) }
    var fuel by rememberSaveable { mutableStateOf(listing?.fuel ?: Fuel.BENZIN) }
    var body by rememberSaveable { mutableStateOf(listing?.body ?: BodyType.HATCHBACK) }
    var city by rememberSaveable { mutableStateOf(listing?.city ?: vm.me?.city.orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(listing?.bio.orEmpty()) }
    var opener by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(listing?.active ?: true) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }

    BackHandler(onBack = onClose)

    fun save() {
        val request = buildRequest(make, model, year, price, km, hp, fuel, body, city, bio, opener, active)
            .getOrElse { error = it.message; return }
        error = null
        saving = true
        scope.launch {
            val photo = photoUri?.let { uri ->
                withContext(Dispatchers.IO) { runCatching { compressPhoto(context, uri) }.getOrNull() }
                    ?: run { error = "Nem sikerült beolvasni a fotót."; saving = false; return@launch }
            }
            if (vm.saveListing(listing?.id, request, photo)) onClose()
            saving = false
        }
    }

    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_back), "Vissza") }
                },
                title = { Text(if (listing == null) "Új hirdetés" else "Hirdetés szerkesztése") },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(Modifier.padding(end = 16.dp).height(24.dp))
                    } else {
                        TextButton(onClick = ::save) { Text("Mentés") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column(
                Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp))) {
                    when {
                        photoUri != null -> AsyncImage(
                            model = photoUri,
                            contentDescription = "Kiválasztott fotó",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        listing != null -> CarPhoto(listing, Modifier.fillMaxSize())
                        else -> Box(Modifier.fillMaxSize()) {
                            CarArt(body, Modifier.fillMaxWidth().align(Alignment.Center).padding(24.dp))
                        }
                    }
                }
                OutlinedButton(
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (photoUri == null && listing?.photoUrl == null) "Fotó kiválasztása" else "Másik fotó") }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(make, { make = it }, "Márka", Modifier.weight(1f))
                    Field(model, { model = it }, "Modell", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(year, { year = it.filter(Char::isDigit).take(4) }, "Évjárat", Modifier.weight(1f), number = true)
                    Field(hp, { hp = it.filter(Char::isDigit).take(4) }, "Teljesítmény (LE)", Modifier.weight(1f), number = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(price, { price = it.filter(Char::isDigit).take(10) }, "Ár (Ft)", Modifier.weight(1f), number = true)
                    Field(km, { km = it.filter(Char::isDigit).take(7) }, "Futás (km)", Modifier.weight(1f), number = true)
                }
                Field(city, { city = it }, "Város", Modifier.fillMaxWidth())

                Text("Üzemanyag", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fuel.entries.forEach { FilterChip(fuel == it, { fuel = it }, label = { Text(it.label) }) }
                }
                Text("Karosszéria", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyType.entries.forEach { FilterChip(body == it, { body = it }, label = { Text(it.label) }) }
                }

                OutlinedTextField(
                    bio, { bio = it.take(1000) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Leírás – mutasd be az autót!") },
                    minLines = 3,
                )
                OutlinedTextField(
                    opener, { opener = it.take(300) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Automatikus első üzenet matchkor (nem kötelező)") },
                    supportingText = if (listing != null) {
                        { Text("Üresen hagyva nem változik a korábbi nyitóüzenet.") }
                    } else {
                        null
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aktív", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Inaktív hirdetést nem látnak a vevők.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(active, { active = it })
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (listing == null) "Hirdetés feladása" else "Mentés")
                }
                if (listing != null) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Hirdetés törlése", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete && listing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Törlöd a hirdetést?") },
            text = { Text("A hozzá tartozó matchek és üzenetek is törlődnek. Ez nem vonható vissza.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteListing(listing)
                    onClose()
                }) { Text("Törlés", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Mégse") } },
        )
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    number: Boolean = false,
) {
    OutlinedTextField(
        value, onChange, modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

/** Ellenőrzi a mezőket; hiba esetén magyar nyelvű üzenettel tér vissza. */
fun buildRequest(
    make: String, model: String, year: String, price: String, km: String, hp: String,
    fuel: Fuel, body: BodyType, city: String, bio: String, opener: String, active: Boolean,
): Result<ListingRequest> {
    fun fail(message: String) = Result.failure<ListingRequest>(IllegalArgumentException(message))
    if (make.isBlank()) return fail("Add meg a márkát.")
    if (model.isBlank()) return fail("Add meg a modellt.")
    val y = year.toIntOrNull() ?: return fail("Add meg az évjáratot.")
    val p = price.toIntOrNull() ?: return fail("Add meg az árat.")
    val k = km.toIntOrNull() ?: return fail("Add meg a futásteljesítményt.")
    val h = hp.toIntOrNull()?.takeIf { it > 0 } ?: return fail("Add meg a teljesítményt.")
    if (city.isBlank()) return fail("Add meg a várost.")
    return Result.success(ListingRequest(make.trim(), model.trim(), y, p, k, h, fuel, body, city.trim(), bio.trim(), opener.trim(), active))
}

/** Legfeljebb 1600 px-es JPEG-re kicsinyít, hogy beférjen a szerver 8 MB-os korlátjába. */
private fun compressPhoto(context: Context, uri: Uri): Pair<ByteArray, String> {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
    val bitmap = resolver.openInputStream(uri)!!.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("decode failed")
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    bitmap.recycle()
    return out.toByteArray() to "image/jpeg"
}
