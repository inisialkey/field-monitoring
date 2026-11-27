package com.inisialkey.fieldmonitoring.core.network

import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

data class ReportOwner(val userId: String)

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): SessionResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): SessionResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @GET("api/v1/me/reports")
    suspend fun myReports(@Tag owner: ReportOwner): List<ReportDto>

    @PUT("api/v1/reports/{id}")
    suspend fun putReport(@Path("id") id: String, @Body body: CreateReportRequest, @Tag owner: ReportOwner): ReportDto

    @Multipart
    @PUT("api/v1/reports/{id}/attachments/{attachmentId}")
    suspend fun putAttachment(
        @Path("id") id: String,
        @Path("attachmentId") attachmentId: String,
        @Part file: MultipartBody.Part,
        @Tag owner: ReportOwner,
    ): AttachmentDto

    @POST("api/v1/reports/{id}/finalize")
    suspend fun finalize(@Path("id") id: String, @Body body: FinalizeRequest, @Tag owner: ReportOwner): ReportDto

    @GET("api/v1/evaluations/queue")
    suspend fun evaluationQueue(@Query("cursor") cursor: String?, @Query("category") category: Category?): QueuePage

    @GET("api/v1/reports/{id}")
    suspend fun report(@Path("id") id: String): ReportDto

    @POST("api/v1/reports/{id}/evaluation")
    suspend fun evaluate(@Path("id") id: String, @Body body: EvaluationRequest): EvaluationDto
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val role: Role,
)

@Serializable
data class SessionResponse(
    val accessToken: String,
    val accessTokenExpiresIn: Int,
    val refreshToken: String,
    val user: UserDto,
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val capturedAt: String,
    val name: String? = null,
)

@Serializable
data class CreateReportRequest(
    val ownerUserId: String,
    val title: String,
    val category: Category,
    val description: String,
    val incidentAt: String,
    val incidentOffsetMinutes: Int,
    val location: LocationDto?,
    val locationNote: String?,
    val createdAt: String,
)

@Serializable
data class AttachmentDto(
    val id: String,
    val kind: String,
)

@Serializable
data class EvaluationDto(
    val result: EvaluationResult,
    val notes: String?,
    val evaluatorName: String,
    val createdAt: String,
)

@Serializable
data class ReportDto(
    val id: String,
    val ownerUserId: String,
    val reporterName: String,
    val title: String,
    val category: Category,
    val description: String,
    val incidentAt: String,
    val incidentOffsetMinutes: Int,
    val location: LocationDto?,
    val locationNote: String?,
    val createdAt: String,
    val receivedAt: String,
    val finalizedAt: String?,
    val attachments: List<AttachmentDto>,
    val evaluation: EvaluationDto?,
)

@Serializable
data class FinalizeRequest(val attachmentIds: List<String>)

@Serializable
data class QueuePage(
    val items: List<ReportDto>,
    val nextCursor: String?,
)

@Serializable
data class EvaluationRequest(
    val result: EvaluationResult,
    val notes: String?,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: List<Map<String, String>> = emptyList(),
)
