#!/bin/bash

# =============================================================
#  Silent Alert App — Script de criação automática da estrutura
#  Uso: bash setup_silent_alert.sh
#  Execute dentro da pasta raiz do projeto Android Studio
# =============================================================

set -e
PKG_PATH="app/src/main/java/com/seuusuario/silentalert"
RES_PATH="app/src/main/res"

echo ""
echo "🚀 Criando estrutura do Silent Alert App..."
echo ""

# ── Pastas ────────────────────────────────────────────────────
mkdir -p \
  $PKG_PATH/core/di \
  $PKG_PATH/core/security \
  $PKG_PATH/domain/model \
  $PKG_PATH/domain/repository \
  $PKG_PATH/domain/usecase \
  $PKG_PATH/data/local/db \
  $PKG_PATH/data/local/prefs \
  $PKG_PATH/data/remote/api \
  $PKG_PATH/data/remote/dto \
  $PKG_PATH/data/repository \
  $PKG_PATH/presentation/lock \
  $PKG_PATH/presentation/settings \
  $RES_PATH/values

echo "✅ Pastas criadas"

# ── DOMAIN — Modelos ──────────────────────────────────────────
cat > $PKG_PATH/domain/model/AlertChannel.kt << 'EOF'
package com.seuusuario.silentalert.domain.model

enum class AlertChannel {
    SMS,
    EMAIL,
    PUSH
}
EOF

cat > $PKG_PATH/domain/model/Contact.kt << 'EOF'
package com.seuusuario.silentalert.domain.model

data class Contact(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val channels: Set<AlertChannel> = setOf(AlertChannel.SMS)
)
EOF

cat > $PKG_PATH/domain/model/AlertConfig.kt << 'EOF'
package com.seuusuario.silentalert.domain.model

data class AlertConfig(
    val panicPassword: String,
    val message: String = "ALERTA: Estou em perigo. Esta mensagem foi enviada automaticamente.",
    val contacts: List<Contact> = emptyList()
)
EOF

echo "✅ Domain/model criado"

# ── DOMAIN — Repositório (interface) ──────────────────────────
cat > $PKG_PATH/domain/repository/AlertRepository.kt << 'EOF'
package com.seuusuario.silentalert.domain.repository

import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact

interface AlertRepository {
    suspend fun getAlertConfig(): AlertConfig
    suspend fun saveAlertConfig(config: AlertConfig)
    suspend fun saveContact(contact: Contact)
    suspend fun deleteContact(contactId: Long)
    suspend fun dispatchAlert(config: AlertConfig): Result<Unit>
}
EOF

echo "✅ Domain/repository criado"

# ── DOMAIN — Use Cases ────────────────────────────────────────
cat > $PKG_PATH/domain/usecase/DetectPanicPasswordUseCase.kt << 'EOF'
package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class DetectPanicPasswordUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(enteredPassword: String): Boolean {
        val config = repository.getAlertConfig()
        return enteredPassword.isNotBlank() &&
               enteredPassword == config.panicPassword
    }
}
EOF

cat > $PKG_PATH/domain/usecase/TriggerSilentAlertUseCase.kt << 'EOF'
package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class TriggerSilentAlertUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val config = repository.getAlertConfig()
        if (config.contacts.isEmpty()) {
            return Result.failure(IllegalStateException("Nenhum contato cadastrado"))
        }
        return repository.dispatchAlert(config)
    }
}
EOF

cat > $PKG_PATH/domain/usecase/SaveAlertConfigUseCase.kt << 'EOF'
package com.seuusuario.silentalert.domain.usecase

import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.repository.AlertRepository
import javax.inject.Inject

class SaveAlertConfigUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(config: AlertConfig) =
        repository.saveAlertConfig(config)
}
EOF

echo "✅ Domain/usecase criado"

# ── DATA — Room Entity ────────────────────────────────────────
cat > $PKG_PATH/data/local/db/ContactEntity.kt << 'EOF'
package com.seuusuario.silentalert.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val channels: String  // JSON serializado ex: "SMS,EMAIL"
)
EOF

cat > $PKG_PATH/data/local/db/ContactDao.kt << 'EOF'
package com.seuusuario.silentalert.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}
EOF

cat > $PKG_PATH/data/local/db/AlertDatabase.kt << 'EOF'
package com.seuusuario.silentalert.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlertDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
EOF

echo "✅ Data/local/db criado"

# ── DATA — SecurePreferences ──────────────────────────────────
cat > $PKG_PATH/data/local/prefs/SecurePreferencesDataSource.kt << 'EOF'
package com.seuusuario.silentalert.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "silent_alert_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePanicPassword(password: String) =
        prefs.edit().putString(KEY_PANIC_PASSWORD, password).apply()

    fun getPanicPassword(): String =
        prefs.getString(KEY_PANIC_PASSWORD, "") ?: ""

    fun saveAlertMessage(message: String) =
        prefs.edit().putString(KEY_ALERT_MESSAGE, message).apply()

    fun getAlertMessage(): String =
        prefs.getString(KEY_ALERT_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE

    companion object {
        private const val KEY_PANIC_PASSWORD = "panic_password"
        private const val KEY_ALERT_MESSAGE  = "alert_message"
        private const val DEFAULT_MESSAGE    =
            "ALERTA: Estou em perigo. Esta mensagem foi enviada automaticamente."
    }
}
EOF

echo "✅ Data/local/prefs criado"

# ── DATA — Remote DTO e API ───────────────────────────────────
cat > $PKG_PATH/data/remote/dto/AlertPayloadDto.kt << 'EOF'
package com.seuusuario.silentalert.data.remote.dto

data class AlertPayloadDto(
    val message: String,
    val recipients: List<RecipientDto>
)

data class RecipientDto(
    val name: String,
    val phone: String,
    val email: String,
    val channels: List<String>
)
EOF

cat > $PKG_PATH/data/remote/api/NotificationApi.kt << 'EOF'
package com.seuusuario.silentalert.data.remote.api

import com.seuusuario.silentalert.data.remote.dto.AlertPayloadDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationApi {
    @POST("api/v1/alert/dispatch")
    suspend fun dispatchAlert(@Body payload: AlertPayloadDto): Response<Unit>
}
EOF

echo "✅ Data/remote criado"

# ── DATA — Repository Impl ────────────────────────────────────
cat > $PKG_PATH/data/repository/AlertRepositoryImpl.kt << 'EOF'
package com.seuusuario.silentalert.data.repository

import com.seuusuario.silentalert.data.local.db.AlertDatabase
import com.seuusuario.silentalert.data.local.db.ContactEntity
import com.seuusuario.silentalert.data.local.prefs.SecurePreferencesDataSource
import com.seuusuario.silentalert.data.remote.api.NotificationApi
import com.seuusuario.silentalert.data.remote.dto.AlertPayloadDto
import com.seuusuario.silentalert.data.remote.dto.RecipientDto
import com.seuusuario.silentalert.domain.model.AlertChannel
import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact
import com.seuusuario.silentalert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val db: AlertDatabase,
    private val prefs: SecurePreferencesDataSource,
    private val api: NotificationApi
) : AlertRepository {

    override suspend fun getAlertConfig(): AlertConfig {
        val contacts = db.contactDao().getAllContacts().first().map { it.toDomain() }
        return AlertConfig(
            panicPassword = prefs.getPanicPassword(),
            message       = prefs.getAlertMessage(),
            contacts      = contacts
        )
    }

    override suspend fun saveAlertConfig(config: AlertConfig) {
        prefs.savePanicPassword(config.panicPassword)
        prefs.saveAlertMessage(config.message)
    }

    override suspend fun saveContact(contact: Contact) {
        db.contactDao().insertContact(contact.toEntity())
    }

    override suspend fun deleteContact(contactId: Long) {
        db.contactDao().deleteContactById(contactId)
    }

    override suspend fun dispatchAlert(config: AlertConfig): Result<Unit> = runCatching {
        val payload = AlertPayloadDto(
            message    = config.message,
            recipients = config.contacts.map { it.toDto() }
        )
        val response = api.dispatchAlert(payload)
        if (!response.isSuccessful) {
            error("Falha ao disparar alerta: ${response.code()}")
        }
    }

    private fun ContactEntity.toDomain() = Contact(
        id       = id,
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.split(",")
            .mapNotNull { runCatching { AlertChannel.valueOf(it) }.getOrNull() }
            .toSet()
    )

    private fun Contact.toEntity() = ContactEntity(
        id       = id,
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.joinToString(",") { it.name }
    )

    private fun Contact.toDto() = RecipientDto(
        name     = name,
        phone    = phone,
        email    = email,
        channels = channels.map { it.name }
    )
}
EOF

echo "✅ Data/repository criado"

# ── CORE — Hilt Modules ───────────────────────────────────────
cat > $PKG_PATH/core/di/DatabaseModule.kt << 'EOF'
package com.seuusuario.silentalert.core.di

import android.content.Context
import androidx.room.Room
import com.seuusuario.silentalert.data.local.db.AlertDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlertDatabase =
        Room.databaseBuilder(context, AlertDatabase::class.java, "alert_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactDao(db: AlertDatabase) = db.contactDao()
}
EOF

cat > $PKG_PATH/core/di/NetworkModule.kt << 'EOF'
package com.seuusuario.silentalert.core.di

import com.seuusuario.silentalert.BuildConfig
import com.seuusuario.silentalert.data.remote.api.NotificationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)
}
EOF

cat > $PKG_PATH/core/di/RepositoryModule.kt << 'EOF'
package com.seuusuario.silentalert.core.di

import com.seuusuario.silentalert.data.repository.AlertRepositoryImpl
import com.seuusuario.silentalert.domain.repository.AlertRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository
}
EOF

echo "✅ Core/di criado"

# ── PRESENTATION — Lock ───────────────────────────────────────
cat > $PKG_PATH/presentation/lock/LockUiState.kt << 'EOF'
package com.seuusuario.silentalert.presentation.lock

sealed class LockUiState {
    object Idle : LockUiState()
    object Unlocking : LockUiState()
    object UnlockNormal : LockUiState()
}
EOF

cat > $PKG_PATH/presentation/lock/LockViewModel.kt << 'EOF'
package com.seuusuario.silentalert.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuusuario.silentalert.domain.usecase.DetectPanicPasswordUseCase
import com.seuusuario.silentalert.domain.usecase.TriggerSilentAlertUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val detectPanic: DetectPanicPasswordUseCase,
    private val triggerAlert: TriggerSilentAlertUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockUiState>(LockUiState.Idle)
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun onPasswordSubmitted(password: String) {
        _uiState.value = LockUiState.Unlocking
        viewModelScope.launch {
            val isPanic = detectPanic(password)
            if (isPanic) {
                // Dispara silenciosamente — sem alertar o usuário
                triggerAlert()
            }
            // Sempre desbloqueia normalmente, seja senha normal ou de pânico
            _uiState.value = LockUiState.UnlockNormal
        }
    }

    fun resetState() {
        _uiState.value = LockUiState.Idle
    }
}
EOF

cat > $PKG_PATH/presentation/lock/LockScreen.kt << 'EOF'
package com.seuusuario.silentalert.presentation.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is LockUiState.UnlockNormal) {
            onUnlocked()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Desbloqueie seu dispositivo",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (password.isNotBlank()) viewModel.onPasswordSubmitted(password)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (password.isNotBlank()) viewModel.onPasswordSubmitted(password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is LockUiState.Unlocking
        ) {
            if (uiState is LockUiState.Unlocking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Desbloquear")
            }
        }
    }
}
EOF

echo "✅ Presentation/lock criado"

# ── PRESENTATION — Settings ───────────────────────────────────
cat > $PKG_PATH/presentation/settings/SettingsUiState.kt << 'EOF'
package com.seuusuario.silentalert.presentation.settings

import com.seuusuario.silentalert.domain.model.Contact

data class SettingsUiState(
    val panicPassword: String = "",
    val alertMessage: String = "",
    val contacts: List<Contact> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)
EOF

cat > $PKG_PATH/presentation/settings/SettingsViewModel.kt << 'EOF'
package com.seuusuario.silentalert.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seuusuario.silentalert.domain.model.AlertConfig
import com.seuusuario.silentalert.domain.model.Contact
import com.seuusuario.silentalert.domain.repository.AlertRepository
import com.seuusuario.silentalert.domain.usecase.SaveAlertConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AlertRepository,
    private val saveConfig: SaveAlertConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadConfig() }

    private fun loadConfig() {
        viewModelScope.launch {
            val config = repository.getAlertConfig()
            _uiState.update {
                it.copy(
                    panicPassword = config.panicPassword,
                    alertMessage  = config.message,
                    contacts      = config.contacts
                )
            }
        }
    }

    fun onPanicPasswordChange(value: String) =
        _uiState.update { it.copy(panicPassword = value) }

    fun onAlertMessageChange(value: String) =
        _uiState.update { it.copy(alertMessage = value) }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                saveConfig(
                    AlertConfig(
                        panicPassword = _uiState.value.panicPassword,
                        message       = _uiState.value.alertMessage,
                        contacts      = _uiState.value.contacts
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            repository.saveContact(contact)
            loadConfig()
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            loadConfig()
        }
    }

    fun dismissSuccess() = _uiState.update { it.copy(savedSuccess = false) }
    fun dismissError()   = _uiState.update { it.copy(error = null) }
}
EOF

cat > $PKG_PATH/presentation/settings/SettingsScreen.kt << 'EOF'
package com.seuusuario.silentalert.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuusuario.silentalert.domain.model.AlertChannel
import com.seuusuario.silentalert.domain.model.Contact

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            AddContactSection(onAdd = viewModel::addContact)
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
private fun AddContactSection(onAdd: (Contact) -> Unit) {
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
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAdd(
                            Contact(
                                name     = name.trim(),
                                phone    = phone.trim(),
                                email    = email.trim(),
                                channels = setOf(AlertChannel.SMS, AlertChannel.EMAIL)
                            )
                        )
                        name = ""; phone = ""; email = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar") }
        }
    }
}
EOF

echo "✅ Presentation/settings criado"

# ── Application class ─────────────────────────────────────────
cat > $PKG_PATH/SilentAlertApp.kt << 'EOF'
package com.seuusuario.silentalert

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SilentAlertApp : Application()
EOF

echo "✅ Application class criada"

# ── Resumo final ──────────────────────────────────────────────
echo ""
echo "============================================"
echo "  ✅ Estrutura criada com sucesso!"
echo "============================================"
echo ""
echo "Próximos passos:"
echo "  1. No Android Studio: Sync Project with Gradle Files"
echo "  2. No AndroidManifest.xml, adicione:"
echo "     android:name=\".SilentAlertApp\""
echo "  3. Build → Make Project"
echo "  4. git add . && git commit -m 'feat: estrutura completa do app' && git push"
echo ""
