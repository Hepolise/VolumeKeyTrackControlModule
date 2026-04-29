package ru.hepolise.volumekeytrackcontrol.module.util

import ru.hepolise.volumekeytrackcontrol.module.VolumeKeyControlModuleHandlers

data class State(
    val isLongPress: Boolean = false,
    val isDownPressed: Boolean = false,
    val isUpPressed: Boolean = false,
    val pendingEvent: VolumeKeyControlModuleHandlers.MediaEvent? = null
) {

    class Builder(state: State) {
        var isLongPress: Boolean = state.isLongPress
        var isDownPressed: Boolean = state.isDownPressed
        var isUpPressed: Boolean = state.isUpPressed
        var pendingEvent: VolumeKeyControlModuleHandlers.MediaEvent? = state.pendingEvent

        fun build(): State = State(
            isLongPress = isLongPress,
            isDownPressed = isDownPressed,
            isUpPressed = isUpPressed,
            pendingEvent = pendingEvent
        )
    }
}