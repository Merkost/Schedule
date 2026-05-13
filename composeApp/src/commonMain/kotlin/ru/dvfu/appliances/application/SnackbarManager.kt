package ru.dvfu.appliances.application

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

sealed interface Message {
    val id: Long

    data class FromResource(override val id: Long, val resource: StringResource) : Message
    data class Text(override val id: Long, val text: String) : Message
}

object SnackbarManager {

    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
    val messages: StateFlow<List<Message>> get() = _messages.asStateFlow()

    fun showMessage(messageTextId: StringResource) {
        _messages.update { it + Message.FromResource(Random.nextLong(), messageTextId) }
    }

    fun showText(text: String) {
        _messages.update { it + Message.Text(Random.nextLong(), text) }
    }

    fun setMessageShown(messageId: Long) {
        _messages.update { it.filterNot { m -> m.id == messageId } }
    }
}
