package com.inisialkey.fieldmonitoring.auth.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.data.AuthRepository
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.auth.domain.DifferentAccountException
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.core.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

data class LoginUiState(
    val reauthUsername: String? = null,
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    @StringRes val error: Int? = null,
    val errorArg: String = "",
    val signedInRole: Role? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    sessionStore: SessionStore,
    connectivity: ConnectivityMonitor,
) : ViewModel() {

    private val reauthUsername = sessionStore.session.value?.username

    val username = TextFieldState(reauthUsername.orEmpty())
    val password = TextFieldState()
    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    private val _state = MutableStateFlow(LoginUiState(reauthUsername = reauthUsername))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun signIn() {
        val name = username.text.toString()
        val secret = password.text.toString()
        if (name.isBlank() || secret.isEmpty() || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val session = auth.login(name, secret)
                _state.update { it.copy(loading = false, signedInRole = session.role) }
            } catch (e: DifferentAccountException) {
                fail(R.string.login_different_account, e.expectedUsername)
            } catch (e: HttpException) {
                fail(
                    when (e.code()) {
                        401 -> R.string.login_invalid_credentials
                        403 -> R.string.login_account_revoked
                        else -> R.string.login_connection_failed
                    },
                )
            } catch (e: IOException) {
                fail(R.string.login_connection_failed)
            } catch (e: SerializationException) {
                fail(R.string.login_connection_failed)
            }
        }
    }

    private fun fail(@StringRes message: Int, arg: String = "") {
        _state.update { it.copy(loading = false, error = message, errorArg = arg) }
    }
}
