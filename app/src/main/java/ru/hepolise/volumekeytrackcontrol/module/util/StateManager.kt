package ru.hepolise.volumekeytrackcontrol.module.util

import ru.hepolise.volumekeytrackcontrol.module.MediaEvent

data class PendingEventInfo(val event: MediaEvent, val isPrimary: Boolean)

data class PendingEventInfoHolder(val info: PendingEventInfo?)

class StateManager {
    @Volatile
    var isDownPressed = false

    @Volatile
    var isUpPressed = false

    @Volatile
    var isLongPress = false

    @Volatile
    var pendingEventInfo: PendingEventInfo? = null
        private set

    @Synchronized
    fun update(block: StateBuilder.() -> Unit) {
        val builder = StateBuilder(this)
        builder.block()
        builder.applyChanges()
    }

    override fun toString(): String {
        return "StateManager(isDownPressed=$isDownPressed, isUpPressed=$isUpPressed, isLongPress=$isLongPress, pendingEventInfo=$pendingEventInfo)"
    }

    class StateBuilder(private val target: StateManager) {
        var isDownPressed: Boolean? = null
        var isUpPressed: Boolean? = null
        var isLongPress: Boolean? = null
        var pendingEventInfoHolder: PendingEventInfoHolder? = null

        fun applyChanges() {
            isDownPressed?.let { target.isDownPressed = it }
            isUpPressed?.let { target.isUpPressed = it }
            isLongPress?.let { target.isLongPress = it }
            pendingEventInfoHolder?.let { target.pendingEventInfo = it.info }
        }
    }
}