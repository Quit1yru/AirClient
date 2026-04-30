/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.FightBot
import net.ccbluex.liquidbounce.features.module.modules.player.Eagle
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold2
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.hasScheduledInLastLoop
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenContainer
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import org.lwjgl.input.Mouse

object GUIMove : Module("GUIMove", Category.MOVEMENT, gameDetecting = false) {

    private val notInChests by boolean("NotInChests", false)
    val aacAdditionPro by boolean("AACAdditionPro", false)
    private val intave by boolean("Intave", false)
    private val intaveSafe by boolean("IntaveSafe", false) { intave }
    private val saveC0E by boolean("SaveC0E", false)
    private val allowJump by boolean("AllowJump", false)
    private val allowSneak by boolean("AllowSneak", false) { !intave }
    private val noSprintWhenClosed by boolean("NoSprintWhenClosed", false) { saveC0E }

    private val isIntave: Boolean
        get() = (mc.currentScreen is GuiInventory || mc.currentScreen is GuiChest) && intave
    private val clickWindowList = ArrayDeque<C0EPacketClickWindow>()
    private val scaffoldSneak: Boolean
        get() = (Scaffold.handleEvents() && Scaffold.eagleSneaking) || (Scaffold2.handleEvents() && Scaffold2.eagleSneaking)
    private val eagleModuleSneak: Boolean
        get() = Eagle.eagleSneaking && Eagle.handleEvents()

    private val noMove by +InventoryManager.noMoveValue
    private val noMoveAir by +InventoryManager.noMoveAirValue
    private val noMoveGround by +InventoryManager.noMoveGroundValue
    private val undetectable by +InventoryManager.undetectableValue

    private var intaveShouldCancel = false

    // If player violates nomove check and inventory is open, close inventory and reopen it when still
    private val silentlyCloseAndReopen by boolean(
        "SilentlyCloseAndReopen",
        false
    ) { noMove && (noMoveAir || noMoveGround) }

    // Reopen closed inventory just before a click (could flag for clicking too fast after opening inventory)
    private val reopenOnClick by boolean(
        "ReopenOnClick",
        false
    ) { silentlyCloseAndReopen && noMove && (noMoveAir || noMoveGround) }

    private val inventoryMotion by float("InventoryMotion", 1F, 0F..2F)

    private val affectedBindings = arrayOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindJump,
        mc.gameSettings.keyBindSprint,
        mc.gameSettings.keyBindSneak,
    )

    val onUpdate = handler<UpdateEvent>(priority = -1) {
        val player = mc.thePlayer ?: return@handler
        val screen = mc.currentScreen

        if ((FightBot.state || scaffoldSneak || eagleModuleSneak) && screen == null) {
            return@handler
        }

        if (shouldFreezeInputs(screen)) {
            unPressKeys()
            return@handler
        }

        if (isIntave && intaveSafe && !player.onGround) {
            unPressKeys()
            return@handler
        }

        if (FightBot.state || scaffoldSneak) {
            if (!FightBot.state) {
                restorePhysicalKeys()
            }
            return@handler
        }

        if (screen is GuiInventory || screen is GuiChest) {
            player.motionX *= inventoryMotion
            player.motionZ *= inventoryMotion
        }

        if (silentlyCloseAndReopen && screen is GuiInventory) {
            if (canClickInventory(closeWhenViolating = true) && !reopenOnClick) serverOpenInventory = true
        }

        if (isIntave && mc.thePlayer.isMoving) {
            mc.gameSettings.keyBindSneak.pressed = true
        }

        if (!FightBot.state) {
            for (affectedBinding in affectedBindings) {
                if (affectedBinding == mc.gameSettings.keyBindSneak && isIntave && mc.thePlayer.isMoving) continue

                affectedBinding.pressed = isButtonPressed(affectedBinding) ||
                        affectedBinding == mc.gameSettings.keyBindSneak && allowSneak && isButtonPressed(mc.gameSettings.keyBindSneak) ||
                        affectedBinding == mc.gameSettings.keyBindSprint && Sprint.handleEvents() && Sprint.mode == "Legit" && (!Sprint.onlyOnSprintPress || mc.thePlayer.isSprinting) ||
                        affectedBinding == mc.gameSettings.keyBindForward && AutoWalk.handleEvents()
            }
        }
    }

    private fun shouldFreezeInputs(screen: GuiScreen?): Boolean {
        if (undetectable) {
            if (screen == null) return false

            if (screen is GuiHudDesigner || screen is ClickGui) return false

            return true
        }

        if (notInChests && screen is GuiChest) return true

        return false
    }

    val onStrafe = handler<StrafeEvent> {
        if (isIntave) {
            if (intaveSafe && intaveShouldCancel) {
                it.cancelEvent()
                WaitTickUtils.schedule(1) {intaveShouldCancel = false}
                return@handler
            }

            if (mc.thePlayer.isMoving) {
                mc.gameSettings.keyBindSneak.pressed = true
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        if (!allowJump && (mc.currentScreen is GuiInventory || mc.currentScreen is GuiChest)) event.cancelEvent()
    }

    val onClick = handler<ClickWindowEvent> { event ->
        if (!canClickInventory()) event.cancelEvent()
        else if (reopenOnClick) {
            hasScheduledInLastLoop = false
            serverOpenInventory = true
        }
    }
    val onStep = handler<StepEvent> { e ->
        intaveShouldCancel = true
    }
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        if (!saveC0E) return@handler

        if (noSprintWhenClosed) {
            if (clickWindowList.isNotEmpty() && !(serverOpenInventory || serverOpenContainer)) player.isSprinting =
                false

            if (packet is C0DPacketCloseWindow) {
                event.cancelEvent()
                player.isSprinting = false
                if (!player.serverSprintState) PacketUtils.sendPacket(C0DPacketCloseWindow(), false)
            }
        }

        if (serverOpenInventory || serverOpenContainer) {
            if (packet is C0EPacketClickWindow) {
                clickWindowList.add(packet)
                event.cancelEvent()
            }
        } else if (clickWindowList.isNotEmpty()) {
            clickWindowList.forEach {
                PacketUtils.sendPacket(it, false)
            }
            clickWindowList.clear()
        }
    }

    override fun onDisable() {
        restorePhysicalKeys()
    }

    /**
     * 恢复按键的物理状态
     */
    private fun restorePhysicalKeys() {
        for (affectedBinding in affectedBindings) {
            affectedBinding.pressed = isButtonPressed(affectedBinding)
        }
    }

    /**
     * 强制释放所有按键（仅用于特殊情况）
     */
    private fun unPressKeys() {
        affectedBindings.forEach {
            it.pressed = false
        }
    }

    private fun isButtonPressed(keyBinding: KeyBinding): Boolean {
        return if (keyBinding.keyCode < 0) {
            Mouse.isButtonDown(keyBinding.keyCode + 100)
        } else {
            GameSettings.isKeyDown(keyBinding)
        }
    }

    override val tag
        get() = when {
            aacAdditionPro -> "AACAdditionPro"
            inventoryMotion != 1F -> inventoryMotion.toString()
            else -> null
        }
}