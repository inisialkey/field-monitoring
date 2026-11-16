package com.inisialkey.fieldmonitoring.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

val ApiJson = Json { ignoreUnknownKeys = true }

fun HttpException.apiError(): ApiError? = try {
    response()?.errorBody()?.string()?.let { ApiJson.decodeFromString<ApiError>(it) }
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}
