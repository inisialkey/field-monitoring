package com.inisialkey.fieldmonitoring.core

import androidx.annotation.StringRes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class UserMessage(
    @StringRes val text: Int,
    val args: List<Any> = emptyList(),
)

@Singleton
class UserMessages @Inject constructor() {
    private val channel = Channel<UserMessage>(Channel.BUFFERED)

    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun show(message: UserMessage) {
        channel.trySend(message)
    }
}
