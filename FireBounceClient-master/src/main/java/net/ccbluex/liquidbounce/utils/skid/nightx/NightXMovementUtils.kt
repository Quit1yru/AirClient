package net.ccbluex.liquidbounce.utils.skid.nightx

import net.ccbluex.liquidbounce.event.MoveEvent
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockIce
import net.minecraft.block.BlockPackedIce
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The type Movement utils.
 */
object NightXMovementUtils {
    val mc = Minecraft.getMinecraft()!!
    val speed: Float
        /**
         * Gets speed.
         *
         * @return the speed
         */
        get() = getSpeed(mc.thePlayer.motionX, mc.thePlayer.motionZ).toFloat()

    /**
     * Gets speed.
     *
     * @param motionX the motion x
     * @param motionZ the motion z
     * @return the speed
     */
    fun getSpeed(motionX: Double, motionZ: Double): Double {
        return sqrt(motionX * motionX + motionZ * motionZ)
    }

    val isOnIce: Boolean
        /**
         * Is on ice boolean.
         *
         * @return the boolean
         */
        get() {
            val player: EntityPlayerSP = mc.thePlayer
            val blockUnder: Block? =
                mc.theWorld.getBlockState(BlockPos(player.posX, player.posY - 1.0, player.posZ)).block
            return blockUnder is BlockIce || blockUnder is BlockPackedIce
        }

    fun setSpeed(moveSpeed: Double, yaw: Float, strafe: Double, forward: Double) {
        var yaw = yaw
        var strafe = strafe
        var forward = forward
        if (forward != 0.0) {
            if (strafe > 0.0) {
                yaw += (if (forward > 0.0) -45 else 45).toFloat()
            } else if (strafe < 0.0) {
                yaw += (if (forward > 0.0) 45 else -45).toFloat()
            }
            strafe = 0.0
            if (forward > 0.0) {
                forward = 1.0
            } else if (forward < 0.0) {
                forward = -1.0
            }
        }
        if (strafe > 0.0) {
            strafe = 1.0
        } else if (strafe < 0.0) {
            strafe = -1.0
        }
        val mx = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val mz = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx
    }

    fun setSpeed(moveSpeed: Double) {
        setSpeed(
            moveSpeed,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.movementInput.moveStrafe.toDouble(),
            mc.thePlayer.movementInput.moveForward.toDouble()
        )
    }

    val isBlockUnder: Boolean
        /**
         * Is block under boolean.
         *
         * @return the boolean
         */
        get() {
            if (mc.thePlayer == null) return false

            if (mc.thePlayer.posY < 0.0) {
                return false
            }
            var off = 0
            while (off < mc.thePlayer.posY.toInt() + 2) {
                val bb: AxisAlignedBB? = mc.thePlayer.entityBoundingBox.offset(0.0, (-off).toDouble(), 0.0)
                if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                    return true
                }
                off += 2
            }
            return false
        }

    val isRidingBlock: Boolean
        get() = (mc.theWorld.getBlockState(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.8, mc.thePlayer.posZ))
            .block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 0.5,
                mc.thePlayer.posZ
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 1.8,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 1.8,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 1.8,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 1.8,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 0.5,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 0.5,
                mc.thePlayer.posZ + 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX + 0.5,
                mc.thePlayer.posY - 0.5,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir) || (mc.theWorld.getBlockState(
            BlockPos(
                mc.thePlayer.posX - 0.5,
                mc.thePlayer.posY - 0.5,
                mc.thePlayer.posZ - 0.5
            )
        ).block !is BlockAir)

    /**
     * Accelerate.
     *
     * @param speed the speed
     */
    fun accelerate(speed: Float) {
        if (!isMoving) return

        val yaw = direction
        mc.thePlayer.motionX += -sin(yaw) * speed
        mc.thePlayer.motionZ += cos(yaw) * speed
    }

    val isMoving: Boolean
        /**
         * Is moving boolean.
         *
         * @return the boolean
         */
        get() = mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward !== 0f || mc.thePlayer.movementInput.moveStrafe !== 0f)

    /**
     * Has motion boolean.
     *
     * @return the boolean
     */
    fun hasMotion(): Boolean {
        return mc.thePlayer.motionX !== 0.0 && mc.thePlayer.motionZ !== 0.0 && mc.thePlayer.motionY !== 0.0
    }

    private val moveYaw: Float
        get() {
            val player: EntityPlayerSP = mc.thePlayer
            var moveYaw = player.rotationYaw

            if (player.moveForward != 0f && player.moveStrafing == 0f) {
                moveYaw += (if (player.moveForward > 0) 0 else 180).toFloat()
            } else if (player.moveForward != 0f) {
                if (player.moveForward > 0) {
                    moveYaw += (if (player.moveStrafing > 0) -45 else 45).toFloat()
                } else {
                    moveYaw -= (if (player.moveStrafing > 0) -45 else 45).toFloat()
                }
                moveYaw += (if (player.moveForward > 0) 0 else 180).toFloat()
            } else if (player.moveStrafing != 0f) {
                moveYaw += (if (player.moveStrafing > 0) -90 else 90).toFloat()
            }

            return moveYaw
        }

    /**
     * Strafe.
     *
     * @param speed the speed
     */
    /**
     * Strafe.
     */
    @JvmOverloads
    fun strafe(speed: Float = NightXMovementUtils.speed) {
        val shotSpeed =
            sqrt((mc.thePlayer.motionX * mc.thePlayer.motionX) + (mc.thePlayer.motionZ * mc.thePlayer.motionZ))
        val fixSpeed = (shotSpeed * 1)
        val motionX: Double = (mc.thePlayer.motionX * (0))
        val motionZ: Double = (mc.thePlayer.motionZ * (0))

        if (!isMoving) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }

        val yaw = moveYaw
        mc.thePlayer.motionX = (((-sin(Math.toRadians(yaw.toDouble())) * fixSpeed) + motionX))
        mc.thePlayer.motionZ = (((cos(Math.toRadians(yaw.toDouble())) * fixSpeed) + motionZ))

        mc.thePlayer.motionX = -sin(direction) * speed
        mc.thePlayer.motionZ = cos(direction) * speed
    }

    /**
     * Forward.
     *
     * @param length the length
     */
    fun forward(length: Double) {
        val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        mc.thePlayer.setPosition(
            mc.thePlayer.posX + (-sin(yaw) * length),
            mc.thePlayer.posY,
            mc.thePlayer.posZ + (cos(yaw) * length)
        )
    }

    val direction: Double
        /**
         * Gets direction.
         *
         * @return the direction
         */
        get() {
            var rotationYaw: Float = mc.thePlayer.rotationYaw

            if (mc.thePlayer.moveForward < 0f) rotationYaw += 180f

            var forward = 1f
            if (mc.thePlayer.moveForward < 0f) forward = -0.5f
            else if (mc.thePlayer.moveForward > 0f) forward = 0.5f

            if (mc.thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward

            if (mc.thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward

            return Math.toRadians(rotationYaw.toDouble())
        }

    val speedEffect: Int
        /**
         * Gets speed effect.
         *
         * @return the speed effect
         */
        get() = if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)
            .amplifier + 1 else 0

    val baseMoveSpeed: Double
        /**
         * Gets base move speed.
         *
         * @return the base move speed
         */
        get() {
            var baseSpeed = 0.2873
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)
                    .amplifier + 1) as Double
            }

            return baseSpeed
        }

    /**
     * Gets base move speed.
     *
     * @param customSpeed the custom speed
     * @return the base move speed
     */
    fun getBaseMoveSpeed(customSpeed: Double): Double {
        var baseSpeed = if (isOnIce) 0.258977700006 else customSpeed
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            val amplifier: Int = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1)
        }
        return baseSpeed
    }

    /**
     * Gets jump boost modifier.
     *
     * @param baseJumpHeight the base jump height
     * @param potionJump     the potion jump
     * @return the jump boost modifier
     */
    fun getJumpBoostModifier(baseJumpHeight: Double, potionJump: Boolean): Double {
        var baseJumpHeight = baseJumpHeight
        if (mc.thePlayer.isPotionActive(Potion.jump) && potionJump) {
            val amplifier: Int = mc.thePlayer.getActivePotionEffect(Potion.jump).amplifier
            baseJumpHeight += ((amplifier + 1).toFloat() * 0.1f).toDouble()
        }

        return baseJumpHeight
    }

    /**
     * Sets speed.
     *
     * @param moveEvent     the move event
     * @param moveSpeed     the move speed
     * @param pseudoYaw     the pseudo yaw
     * @param pseudoStrafe  the pseudo strafe
     * @param pseudoForward the pseudo forward
     */
    fun setSpeed(
        moveEvent: MoveEvent,
        moveSpeed: Double,
        pseudoYaw: Float,
        pseudoStrafe: Double,
        pseudoForward: Double
    ) {
        var forward = pseudoForward
        var strafe = pseudoStrafe
        var yaw = pseudoYaw

        if (forward == 0.0 && strafe == 0.0) {
            moveEvent.x = 0.0
            moveEvent.z = 0.0
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (if (forward > 0.0) -45 else 45).toFloat()
                } else if (strafe < 0.0) {
                    yaw += (if (forward > 0.0) 45 else -45).toFloat()
                }
                strafe = 0.0
                if (forward > 0.0) {
                    forward = 1.0
                } else if (forward < 0.0) {
                    forward = -1.0
                }
            }
            if (strafe > 0.0) {
                strafe = 1.0
            } else if (strafe < 0.0) {
                strafe = -1.0
            }
            val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
            val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))

            moveEvent.x = ((forward * moveSpeed * cos + strafe * moveSpeed * sin))
            moveEvent.z = ((forward * moveSpeed * sin - strafe * moveSpeed * cos))
        }
    }
}