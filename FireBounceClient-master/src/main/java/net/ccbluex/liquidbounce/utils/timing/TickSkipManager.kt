package net.ccbluex.liquidbounce.utils.timing

object TickSkipManager {
    private var skipTicks = 0

    fun shouldSkipTick(): Boolean {
        if (skipTicks > 0) {
            skipTicks--
            return true
        }
        return false
    }

    fun skipTicks(count: Int) {
        skipTicks = count
    }

    fun getRemainingSkips(): Int = skipTicks
}