package com.seuusuario.silentalert.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuusuario.silentalert.domain.model.AlertChannel
import com.seuusuario.silentalert.domain.model.Contact

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var smsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> smsGranted = granted }

    if (state.savedSuccess) {
        LaunchedEffect(Unit) { viewModel.dismissSuccess() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Configurações", style = MaterialTheme.typography.headlineSmall)
        }

        // Banner de permissão SMS
        if (!smsGranted) {
            item {
                SmsPermissionBanner(
                    onRequest = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) }
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.panicPassword,
                onValueChange = viewModel::onPanicPasswordChange,
                label = { Text("Senha de pânico") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = state.alertMessage,
                onValueChange = viewModel::onAlertMessageChange,
                label = { Text("Mensagem de alerta") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Contatos de emergência", style = MaterialTheme.typography.titleMedium)
        }

        items(state.contacts) { contact ->
            ContactItem(
                contact = contact,
                onDelete = { viewModel.deleteContact(contact.id) }
            )
        }

        item {
            AddContactSection(
                smsGranted = smsGranted,
                onRequestSms = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                onAdd = viewModel::addContact
            )
        }

        item {
            Button(
                onClick = viewModel::saveConfig,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "Salvando..." else "Salvar configurações")
            }
        }

        state.error?.let { error ->
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SmsPermissionBanner(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Permissão de SMS necessária para alertas offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Permitir")
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                Text(contact.email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remover")
            }
        }
    }
}

@Composable
private fun AddContactSection(
    smsGranted: Boolean,
    onRequestSms: () -> Unit,
    onAdd: (Contact) -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Adicionar contato", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nome") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth()
            )

            if (!smsGranted) {
                Text(
                    text = "SMS desativado — contato receberá apenas por e-mail e push",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val channels = buildSet {
                            if (smsGranted) add(AlertChannel.SMS)
                            add(AlertChannel.EMAIL)
                        }
                        onAdd(
                            Contact(
                                name     = name.trim(),
                                phone    = phone.trim(),
                                email    = email.trim(),
                                channels = channels
                            )
                        )
                        name = ""; phone = ""; email = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar")
            }
        }
    }
}
