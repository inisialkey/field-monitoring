package com.inisialkey.fieldmonitoring.auth.domain

import kotlinx.serialization.Serializable

enum class Role { FIELD_OFFICER, EVALUATOR }

@Serializable
data class Session(
    val userId: String,
    val username: String,
    val displayName: String,
    val role: Role,
    val accessToken: String,
    val refreshToken: String,
    val expired: Boolean = false,
)

class DifferentAccountException(val expectedUsername: String) : Exception()
