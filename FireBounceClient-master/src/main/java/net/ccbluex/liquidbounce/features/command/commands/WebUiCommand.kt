/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.web.WebServerManager

object WebUiCommand : Command("webui", "web") {
    
    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("webui <start/stop/port>")
            return
        }
        
        when (args[1].lowercase()) {
            "start" -> {
                if (WebServerManager.isRunning) {
                    chat("§cWeb UI server is already running!")
                    return
                }
                
                if (WebServerManager.start()) {
                    chat("§aWeb UI server started on http://localhost:${WebServerManager.getPort()}")
                } else {
                    chat("§cFailed to start Web UI server!")
                }
            }
            "stop" -> {
                if (!WebServerManager.isRunning) {
                    chat("§cWeb UI server is not running!")
                    return
                }
                
                WebServerManager.stop()
                chat("§aWeb UI server stopped")
            }
            "port" -> {
                if (args.size <= 2) {
                    chat("Current port: ${WebServerManager.getPort()}")
                    chatSyntax("webui port <port>")
                    return
                }
                
                try {
                    val newPort = args[2].toInt()
                    if (newPort < 1 || newPort > 65535) {
                        chat("§cPort must be between 1 and 65535!")
                        return
                    }
                    
                    WebServerManager.setPort(newPort)
                    chat("§aPort changed to $newPort")
                    
                    if (WebServerManager.isRunning) {
                        chat("§eServer restarted with new port")
                    }
                } catch (_: NumberFormatException) {
                    chat("§cInvalid port number!")
                }
            }
            else -> chatSyntax("webui <start/stop/port>")
        }
    }
    
    override fun tabComplete(args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("start", "stop", "port").filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
