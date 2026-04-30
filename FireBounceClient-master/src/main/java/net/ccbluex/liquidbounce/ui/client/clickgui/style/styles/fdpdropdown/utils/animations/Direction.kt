package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.animations

enum class Direction {
    FORWARDS,
    BACKWARDS;

    fun reverse(): Direction = if (this == FORWARDS) BACKWARDS else FORWARDS
}
