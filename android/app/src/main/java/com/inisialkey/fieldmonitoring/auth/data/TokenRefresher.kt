package com.inisialkey.fieldmonitoring.auth.data

import com.inisialkey.fieldmonitoring.auth.domain.Session
import com.inisialkey.fieldmonitoring.core.network.ApiService
import com.inisialkey.fieldmonitoring.core.network.RefreshRequest
import dagger.Lazy
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@Singleton
class TokenRefresher @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: Lazy<ApiService>,
) {
    private val mutex = Mutex()

    suspend fun refresh(rejectedAccessToken: String): Session? = mutex.withLock {
        val session = sessionStore.session.value ?: return null
        if (session.accessToken != rejectedAccessToken) return session
        if (session.expired) return null
        val response = try {
            api.get().refresh(RefreshRequest(session.refreshToken, sessionStore.deviceId()))
        } catch (e: HttpException) {
            if (e.code() !in 400..499) throw IOException("Token refresh failed with ${e.code()}", e)
            if (sessionStore.session.value == session) sessionStore.save(session.copy(expired = true))
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Token refresh failed", e)
        }
        if (sessionStore.session.value != session) return null
        session.copy(accessToken = response.accessToken, refreshToken = response.refreshToken)
            .also { sessionStore.save(it) }
    }
}
