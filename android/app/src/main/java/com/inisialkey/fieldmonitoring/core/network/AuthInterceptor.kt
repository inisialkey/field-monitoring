package com.inisialkey.fieldmonitoring.core.network

import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import java.io.IOException
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class OwnerMismatchException : IOException("The report owner is not the signed-in user")

class AuthInterceptor @Inject constructor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val session = sessionStore.session.value
        val owner = request.tag(ReportOwner::class.java)
        if (owner != null && owner.userId != session?.userId) throw OwnerMismatchException()
        if (session == null || request.url.encodedPath.contains("/api/v1/auth/")) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer ${session.accessToken}").build())
    }
}
