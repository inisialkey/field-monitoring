package com.inisialkey.fieldmonitoring.core.network

import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.auth.domain.Session
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthInterceptorTest {

    private val sessions = MutableStateFlow<Session?>(
        Session("user-a", "rina", "Rina Wulandari", Role.FIELD_OFFICER, accessToken = "access-a", refreshToken = "refresh-a"),
    )
    private val sessionStore = mockk<SessionStore> { every { session } returns sessions }
    private val interceptor = AuthInterceptor(sessionStore)
    private val sent = slot<Request>()

    private fun chainFor(request: Request): Interceptor.Chain = mockk {
        every { request() } returns request
        every { proceed(capture(sent)) } answers {
            Response.Builder().request(sent.captured).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        }
    }

    private fun syncRequest(ownerId: String) = Request.Builder()
        .url("http://10.0.2.2:3000/api/v1/reports/report-1")
        .tag(ReportOwner::class.java, ReportOwner(ownerId))
        .build()

    @Test
    fun `sync request of the signed-in owner carries the access token`() {
        interceptor.intercept(chainFor(syncRequest("user-a")))
        assertEquals("Bearer access-a", sent.captured.header("Authorization"))
    }

    @Test
    fun `sync request of another owner never leaves the device`() {
        val chain = chainFor(syncRequest("user-b"))
        assertThrows(OwnerMismatchException::class.java) { interceptor.intercept(chain) }
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `sync request after logout never leaves the device`() {
        sessions.value = null
        val chain = chainFor(syncRequest("user-a"))
        assertThrows(OwnerMismatchException::class.java) { interceptor.intercept(chain) }
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `auth endpoints are sent without the access token`() {
        interceptor.intercept(chainFor(Request.Builder().url("http://10.0.2.2:3000/api/v1/auth/refresh").build()))
        assertNull(sent.captured.header("Authorization"))
    }
}
