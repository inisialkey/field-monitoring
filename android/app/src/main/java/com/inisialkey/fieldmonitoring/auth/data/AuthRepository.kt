package com.inisialkey.fieldmonitoring.auth.data

import android.content.Context
import coil3.SingletonImageLoader
import com.inisialkey.fieldmonitoring.auth.domain.DifferentAccountException
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.auth.domain.Session
import com.inisialkey.fieldmonitoring.core.ApplicationScope
import com.inisialkey.fieldmonitoring.core.network.ApiService
import com.inisialkey.fieldmonitoring.core.network.LoginRequest
import com.inisialkey.fieldmonitoring.core.network.LogoutRequest
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val sessionStore: SessionStore,
    private val reports: ReportRepository,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    suspend fun login(username: String, password: String): Session {
        val previous = sessionStore.session.value
        val response = api.login(LoginRequest(username.trim(), password, sessionStore.deviceId()))
        if (previous != null && previous.userId != response.user.id) {
            revoke(response.refreshToken)
            throw DifferentAccountException(previous.username)
        }
        val session = Session(
            userId = response.user.id,
            username = response.user.username,
            displayName = response.user.displayName,
            role = response.user.role,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        sessionStore.save(session)
        if (session.role == Role.FIELD_OFFICER) syncScheduler.enqueue(session.userId)
        return session
    }

    suspend fun logout() {
        val session = sessionStore.session.value ?: return
        sessionStore.clear()
        syncScheduler.cancel(session.userId)
        reports.deleteSynced(session.userId)
        withContext(Dispatchers.IO) {
            SingletonImageLoader.get(context).run {
                memoryCache?.clear()
                diskCache?.clear()
            }
        }
        revoke(session.refreshToken)
    }

    private fun revoke(refreshToken: String) {
        scope.launch {
            try {
                api.logout(LogoutRequest(refreshToken))
            } catch (e: IOException) {
                return@launch
            } catch (e: HttpException) {
                return@launch
            }
        }
    }
}
