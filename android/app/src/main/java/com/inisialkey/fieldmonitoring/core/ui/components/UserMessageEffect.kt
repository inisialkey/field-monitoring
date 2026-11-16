package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inisialkey.fieldmonitoring.core.UserMessage
import kotlinx.coroutines.flow.Flow

@Composable
fun UserMessageEffect(messages: Flow<UserMessage>, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(messages, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            messages.collect { snackbarHostState.showSnackbar(resources.getString(it.text, *it.args.toTypedArray())) }
        }
    }
}
