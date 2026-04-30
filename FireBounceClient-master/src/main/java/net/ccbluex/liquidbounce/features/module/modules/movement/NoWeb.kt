/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.aac.LAAC
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave.Intave14
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave.Intave2
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave.IntaveNew
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave.IntaveOld
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.tatako.Tatako

object NoWeb : Module("NoWeb", Category.MOVEMENT) {

    private val noWebModes = arrayOf(
        // Vanilla
        None,

        // AAC
        AAC, LAAC,

        // Intave
        IntaveOld,
        IntaveNew,
        Intave14,
        Intave2,

        // Matrix
        Matrix,
        MatrixNew,

        // Other
        Rewi,
        OldGrim,
        Grim2,

        Tatako,
        TestMode,
    )

    private val modes = noWebModes.map { it.modeName }.toTypedArray()

    val mode by choices(
        "Mode", modes, "None"
    )

    val grimStrict by boolean("GrimStrict",false) { mode == "Grim2"}
    val grimBreakOnWorld by boolean("GrimBreakOnWorld",false) { mode == "Grim2" }

    val testOnlyWhenForwarding by boolean("OnlyWhenForwarding",true) { mode == "TestMode" }
    val testModeAlwaysJump by boolean("AlwaysJump",false) { mode == "TestMode" }
    val testDisableInWebState by boolean("CancelInWebState",false) { mode == "TestMode" }
    val testSetMotionXZ by boolean("SetMotionXZFactor",false) { mode == "TestMode" }
    val testMotionXZFactor by float("MotionXZFactor",1.0f,0.01f..10f) {mode == "TestMode" && testSetMotionXZ}
    val testSetMotionY by boolean("SetMotionYFactor",false) { mode == "TestMode" }
    val testMotionYFactor by float("MotionYFactor",1.0f,0.01f..10f) {mode == "TestMode" && testSetMotionY}
    val testSetJumpMovementFactor by boolean("SetJumpMovementFactor",false) { mode == "TestMode" }
    val testJumpMovementFactor by float("JumpMovementFactor",0.02f,0.01f..10f) { mode == "TestMode" && testSetJumpMovementFactor }
    val onUpdate = handler<UpdateEvent> {
        modeModule.onUpdate()
    }
    val matrixNoWebThreshold by int("MatrixNew-ThresholdTicks",4,0..20) {mode == "MatrixNew"}
    val matrixShowThresholdTicks by boolean("MatrixNew-ShowThresholdTicks",false) {mode == "MatrixNew"}
    val matrixKeepY by boolean("KeepY",false) {mode == "MatrixNew"}
    var inWebTicks = 0
    override val tag
        get() = mode

    private val modeModule
        get() = noWebModes.find { it.modeName == mode }!!
}
