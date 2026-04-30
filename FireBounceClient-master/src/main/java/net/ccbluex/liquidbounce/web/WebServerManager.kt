/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.web

import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER

object WebServerManager {
    private var server: HttpServer? = null
    private var port = 1337

    val isRunning: Boolean
        get() = server?.wasStarted() ?: false

    fun start(): Boolean {
        if (isRunning) {
            return false
        }

        try {
            server = HttpServer(port)
            server?.start()
            LOGGER.info("[WebUI] Started web server on port $port")
            return true
        } catch (e: Exception) {
            LOGGER.error("[WebUI] Failed to start web server", e)
            return false
        }
    }

    fun stop() {
        if (!isRunning) {
            return
        }

        try {
            server?.stop()
            server = null
            LOGGER.info("[WebUI] Stopped web server")
        } catch (e: Exception) {
            LOGGER.error("[WebUI] Failed to stop web server", e)
        }
    }

    fun setPort(newPort: Int) {
        if (isRunning) {
            stop()
            port = newPort
            start()
        } else {
            port = newPort
        }
    }

    fun getPort(): Int = port
}
