package com.inisialkey.fieldmonitoring.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.inisialkey.fieldmonitoring.auth.domain.Session
import com.inisialkey.fieldmonitoring.core.ApplicationScope
import java.security.GeneralSecurityException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    @ApplicationScope scope: CoroutineScope,
) {
    private val current = MutableStateFlow<Session?>(null)
    private val loaded = MutableStateFlow(false)

    val session: StateFlow<Session?> = current.asStateFlow()
    val isLoaded: StateFlow<Boolean> = loaded.asStateFlow()

    init {
        scope.launch {
            current.value = dataStore.data.first()[SESSION]?.let(::decode)
            loaded.value = true
        }
    }

    suspend fun awaitLoaded() {
        loaded.first { it }
    }

    suspend fun save(session: Session) {
        current.value = session
        dataStore.edit { it[SESSION] = cipher.encrypt(Json.encodeToString(session)) }
    }

    suspend fun clear() {
        current.value = null
        dataStore.edit { it.remove(SESSION) }
    }

    suspend fun deviceId(): String {
        val preferences = dataStore.edit { if (it[DEVICE_ID] == null) it[DEVICE_ID] = UUID.randomUUID().toString() }
        return checkNotNull(preferences[DEVICE_ID])
    }

    private fun decode(value: String): Session? = try {
        Json.decodeFromString<Session>(cipher.decrypt(value))
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        val SESSION = stringPreferencesKey("session")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }
}
