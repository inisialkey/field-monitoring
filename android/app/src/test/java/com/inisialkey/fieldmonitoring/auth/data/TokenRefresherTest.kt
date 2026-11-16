package com.inisialkey.fieldmonitoring.auth.data

import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.auth.domain.Session
import com.inisialkey.fieldmonitoring.core.network.ApiService
import com.inisialkey.fieldmonitoring.core.network.RefreshRequest
import com.inisialkey.fieldmonitoring.core.network.SessionResponse
import com.inisialkey.fieldmonitoring.core.network.UserDto
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class TokenRefresherTest {

    private val signedIn = Session("user-a", "rina", "Rina Wulandari", Role.FIELD_OFFICER, "old-access", "old-refresh")
    private val sessions = MutableStateFlow<Session?>(signedIn)
    private val sessionStore = mockk<SessionStore> {
        every { session } returns sessions
        coEvery { save(any()) } answers { sessions.value = firstArg() }
        coEvery { deviceId() } returns "device-1"
    }
    private val api = mockk<ApiService>()
    private val refresher = TokenRefresher(sessionStore, Lazy { api })

    private val renewed = SessionResponse(
        accessToken = "new-access",
        accessTokenExpiresIn = 300,
        refreshToken = "new-refresh",
        user = UserDto("user-a", "rina", "Rina Wulandari", Role.FIELD_OFFICER),
    )

    @Test
    fun `two concurrent refreshes call the server once`() = runTest {
        coEvery { api.refresh(any()) } coAnswers {
            delay(100)
            renewed
        }

        val results = List(2) { async { refresher.refresh("old-access") } }.awaitAll()

        coVerify(exactly = 1) { api.refresh(RefreshRequest("old-refresh", "device-1")) }
        assertEquals(listOf("new-access", "new-access"), results.map { it?.accessToken })
        assertEquals("new-refresh", sessions.value?.refreshToken)
    }

    @Test
    fun `rejected refresh token marks the session expired and keeps it`() = runTest {
        coEvery { api.refresh(any()) } throws HttpException(Response.error<Any>(401, "{}".toResponseBody()))

        assertNull(refresher.refresh("old-access"))
        assertEquals("user-a", sessions.value?.userId)
        assertEquals(true, sessions.value?.expired)
    }

    @Test
    fun `network failure keeps the session usable`() = runTest {
        coEvery { api.refresh(any()) } throws IOException("offline")

        val error = runCatching { refresher.refresh("old-access") }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(signedIn, sessions.value)
    }

    @Test
    fun `logout during refresh does not bring the session back`() = runTest {
        coEvery { api.refresh(any()) } coAnswers {
            sessions.value = null
            renewed
        }

        assertNull(refresher.refresh("old-access"))
        assertNull(sessions.value)
    }
}
