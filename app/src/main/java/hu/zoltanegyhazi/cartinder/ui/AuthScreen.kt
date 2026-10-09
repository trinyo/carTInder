package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.zoltanegyhazi.cartinder.CarTinderViewModel

@Composable
fun AuthScreen(vm: CarTinderViewModel, modifier: Modifier = Modifier) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf(vm.serverUrl) }
    var showServer by rememberSaveable { mutableStateOf(false) }

    val canSubmit = email.isNotBlank() && password.isNotBlank() && (!registering || name.isNotBlank()) && !vm.authBusy
    val localNetworkGate = rememberLocalNetworkGate()
    val submit = {
        if (canSubmit) {
            localNetworkGate(serverUrl) {
                if (registering) vm.register(email, password, name, city, serverUrl) else vm.login(email, password, serverUrl)
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Logo()
        Text(
            "Húzd jobbra álmaid autóját.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryTabRow(selectedTabIndex = if (registering) 1 else 0) {
                Tab(!registering, { registering = false }, text = { Text("Bejelentkezés") })
                Tab(registering, { registering = true }, text = { Text("Regisztráció") })
            }
            if (registering) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Név") }, singleLine = true)
                OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("Város (nem kötelező)") }, singleLine = true)
            }
            OutlinedTextField(
                email, { email = it }, Modifier.fillMaxWidth(),
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                password, { password = it }, Modifier.fillMaxWidth(),
                label = { Text(if (registering) "Jelszó (min. 8 karakter)" else "Jelszó") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            vm.authError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (vm.authBusy) {
                    CircularProgressIndicator(Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (registering) "Fiók létrehozása" else "Belépés")
                }
            }

            TextButton(onClick = { showServer = !showServer }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (showServer) "Szerver beállítás elrejtése" else "Szerver beállítása")
            }
            if (showServer) {
                OutlinedTextField(
                    serverUrl, { serverUrl = it }, Modifier.fillMaxWidth(),
                    label = { Text("Szerver címe") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { Text("Emulátorból: http://10.0.2.2:8080 · telefonról a gép helyi IP-címe") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "A demó autók eladói botok: azonnal döntenek és visszaírnak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
