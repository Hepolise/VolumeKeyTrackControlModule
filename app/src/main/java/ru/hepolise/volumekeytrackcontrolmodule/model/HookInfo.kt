package ru.hepolise.volumekeytrackcontrolmodule.model

data class HookInfo(val params: Array<Any>, val logMessage: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HookInfo

        if (!params.contentEquals(other.params)) return false
        if (logMessage != other.logMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = params.contentHashCode()
        result = 31 * result + logMessage.hashCode()
        return result
    }
}
