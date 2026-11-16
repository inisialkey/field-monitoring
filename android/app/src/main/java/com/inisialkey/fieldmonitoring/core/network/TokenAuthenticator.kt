package com.inisialkey.fieldmonitoring.core.network

import com.inisialkey.fieldmonitoring.auth.data.TokenRefresher
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator @Inject constructor(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        val session = runBlocking { refresher.refresh(sentToken) } ?: return null
        val owner = response.request.tag(ReportOwner::class.java)
        if (owner != null && owner.userId != session.userId) return null
        return response.request.newBuilder().header("Authorization", "Bearer ${session.accessToken}").build()
    }
}
