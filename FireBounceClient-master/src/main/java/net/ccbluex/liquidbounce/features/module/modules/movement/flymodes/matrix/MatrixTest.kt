package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extra.StuckUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.math.max

object MatrixTest : FlyMode("MatrixTest") {

    private var moveTicksRemaining = 0
    private var allowMovement = true
    private var freezeActive = false
    private var stuckActive = false

    private var savedMotionX = 0.0
    private var savedMotionY = 0.0
    private var savedMotionZ = 0.0
    private var savedX = 0.0
    private var savedY = 0.0
    private var savedZ = 0.0

    private var stuckStartedByThis = false

    private val period get() = max(1, Fly.matrixPeriod)
    private val motionY1 get() = Fly.matrixMotionY1.toDouble()
    private val motionY2 get() = Fly.matrixMotionY2.toDouble()
    private val timerSpeed get() = Fly.matrixTimerSpeed
    private val configuredMoveTick get() = Fly.matrixMoveTick
    private val onlyOnDamage get() = Fly.matrixOnlyOnDamage
    private val useStuck get() = Fly.matrixUseStuck

    override fun onEnable() {
        try {
            mc.timer.timerSpeed = timerSpeed
        } catch (_: Throwable) {}

        allowMovement = !onlyOnDamage
        moveTicksRemaining = 0
        freezeActive = false
        stuckActive = false
        stuckStartedByThis = false
    }

    override fun onDisable() {
        try {
            mc.timer.timerSpeed = 1f
        } catch (_: Throwable) {}

        if (stuckActive && stuckStartedByThis) {
            try { StuckUtils.stopStuck() } catch (_: Throwable) {}
            stuckActive = false
            stuckStartedByThis = false
        }

        if (freezeActive) {
            val player = mc.thePlayer
            if (player != null) {
                try {
                    player.motionX = savedMotionX
                    player.motionY = savedMotionY
                    player.motionZ = savedMotionZ
                    player.setPositionAndRotation(savedX, savedY, savedZ, player.rotationYaw, player.rotationPitch)
                } catch (_: Throwable) {}
            }
            freezeActive = false
        }

        allowMovement = true
        moveTicksRemaining = 0
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        try { mc.timer.timerSpeed = timerSpeed } catch (_: Throwable) {}

        if (onlyOnDamage && !allowMovement) {
            if (!freezeActive && !stuckActive) {
                savedMotionX = player.motionX
                savedMotionY = player.motionY
                savedMotionZ = player.motionZ
                savedX = player.posX
                savedY = player.posY
                savedZ = player.posZ
            }
            if (!stuckActive) {
                freezeActive = true
                player.motionX = 0.0
                player.motionY = 0.0
                player.motionZ = 0.0
                try {
                    player.setPositionAndRotation(savedX, savedY, savedZ, player.rotationYaw, player.rotationPitch)
                } catch (_: Throwable) {}
            }
        }

        if (moveTicksRemaining > 0) {
            moveTicksRemaining--
            if (moveTicksRemaining <= 0) {
                allowMovement = false

                if (useStuck) {
                    try {
                        StuckUtils.stuck()
                        stuckActive = true
                        stuckStartedByThis = true
                        freezeActive = false
                    } catch (_: Throwable) {
                        stuckActive = false
                        freezeActive = true
                    }
                } else {
                    freezeActive = true
                    savedMotionX = player.motionX
                    savedMotionY = player.motionY
                    savedMotionZ = player.motionZ
                    savedX = player.posX
                    savedY = player.posY
                    savedZ = player.posZ
                }
            }
        }

        if (allowMovement) {
            val mod = (player.ticksExisted % period)
            val mY = if (mod == 0) motionY1 else motionY2
            try {
                player.motionY = mY
            } catch (_: Throwable) {}
            if (stuckActive) {
                try { StuckUtils.stopStuck() } catch (_: Throwable) {}
                stuckActive = false
                stuckStartedByThis = false
            }
            if (freezeActive) {
                try {
                    player.motionX = savedMotionX
                    player.motionZ = savedMotionZ
                } catch (_: Throwable) {}
                freezeActive = false
            }
        }
    }

    override fun onTick() {
        val player = mc.thePlayer ?: return

        if (player.hurtTime > 0) {
            if (onlyOnDamage) {
                allowMovement = true
                moveTicksRemaining = configuredMoveTick
                if (stuckActive) {
                    try { StuckUtils.stopStuck() } catch (_: Throwable) {}
                    stuckActive = false
                    stuckStartedByThis = false
                }
                if (freezeActive) {
                    try {
                        player.motionX = savedMotionX
                        player.motionY = savedMotionY
                        player.motionZ = savedMotionZ
                        player.setPositionAndRotation(savedX, savedY, savedZ, player.rotationYaw, player.rotationPitch)
                    } catch (_: Throwable) {}
                    freezeActive = false
                }
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (!freezeActive) return

        val packet = event.packet
        if (packet is C03PacketPlayer) {
            event.cancelEvent()
        } else if (packet is S08PacketPlayerPosLook) {
            try {
                savedX = packet.x
                savedY = packet.y
                savedZ = packet.z
                savedMotionX = 0.0
                savedMotionY = 0.0
                savedMotionZ = 0.0
            } catch (_: Throwable) {}
        }
    }

    fun onWorld() {
        try { mc.timer.timerSpeed = 1f } catch (_: Throwable) {}
        if (stuckActive && stuckStartedByThis) {
            try { StuckUtils.stopStuck() } catch (_: Throwable) {}
            stuckActive = false
            stuckStartedByThis = false
        }
        freezeActive = false
        allowMovement = !onlyOnDamage
        moveTicksRemaining = 0
    }
}
