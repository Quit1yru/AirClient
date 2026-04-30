package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.pathfinder.CustomPathHelper
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumParticleTypes
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import javax.vecmath.Vector2f
import kotlin.math.cos
import kotlin.math.sin

class ShootTask(
    private val particleType: String,
    private var motionX: Float,
    private var motionZ: Float,
    private var motionY: Float,
    private val yaw: Float,
    private val pitch: Float,
    private var position: Vec3,
    private val maxDelta: Float,
    private val verticalAngleOffset: Float,
    private val withGravity: Boolean,
    private val motionData: Vector2f
) {
    companion object {
        private val mc = Minecraft.getMinecraft()
        private const val DEGREES_TO_RADIANS = 0.017453292F
        private const val BOUNDING_BOX_EXTRA = 0.25
        private const val HEART_PARTICLE_COUNT = 15
        private const val HEART_Y_OFFSET = 1.25
        private val AIR_BLOCK_CHECK = Blocks.air
        private const val MIN_HEIGHT_OFFSET = -64
        private const val PARTICLE_OFFSET = 0.5
        private const val PARTICLE_DISAPPEAR_RANGE = 128
    }

    private fun findCollidingEntities(prevPos: Vec3, newPos: Vec3): List<Entity> {
        return mc.theWorld.loadedEntityList.filter { entity ->
                    entity != mc.thePlayer &&
                    entity is EntityLivingBase &&
                            checkLine(prevPos, newPos, entity.entityBoundingBox.expand(BOUNDING_BOX_EXTRA, BOUNDING_BOX_EXTRA, BOUNDING_BOX_EXTRA))
        }
    }
    private fun checkLine(start: Vec3, end: Vec3, boundingBox: AxisAlignedBB): Boolean {
        return CustomPathHelper.findPathDirectly(
            start.xCoord, start.yCoord, start.zCoord,
            end.xCoord, end.yCoord, end.zCoord,
            0.1
        ).any { boundingBox.intersectsWith(AxisAlignedBB(it.xCoord - 0.05, it.yCoord - 0.05, it.zCoord - 0.05, it.xCoord + 0.05, it.yCoord + 0.05, it.zCoord + 0.05)) }
    }

    private fun applyCollisionEffect(prevPos: Vec3, newPos: Vec3): Boolean {
        val collidingEntities = findCollidingEntities(prevPos, newPos)

        if (collidingEntities.isNotEmpty()) {
            collidingEntities.forEach { entity ->
                repeat(HEART_PARTICLE_COUNT+nextInt(-10, 10)) {
                    mc.effectRenderer.spawnEffectParticle(
                        EnumParticleTypes.HEART.particleID,
                        entity.posX + Math.random() * PARTICLE_OFFSET,
                        entity.posY + HEART_Y_OFFSET + Math.random() * PARTICLE_OFFSET,
                        entity.posZ+Math.random() * PARTICLE_OFFSET,
                        0.0, 0.0, 0.0,
                        0
                    )
                }
            }
        }

        return collidingEntities.isNotEmpty()
    }

    fun redirect(): ShootTask {
        val yawRadians = yaw * DEGREES_TO_RADIANS
        val pitchRadians = (pitch + verticalAngleOffset) * DEGREES_TO_RADIANS

        motionX *= (-sin(yawRadians) * cos(pitchRadians))
        motionY *= (-sin(pitchRadians))
        motionZ *= (cos(yawRadians) * cos(pitchRadians))

        return this
    }

    fun tick(): Boolean {
        if (mc.theWorld == null) return false

        val currentBlockPos = BlockPos(position.xCoord, position.yCoord, position.zCoord)
        val isAirBlock = BlockUtils.getBlock(currentBlockPos) == AIR_BLOCK_CHECK
        val shouldKeepAlive = if(withGravity)
                position.yCoord > mc.thePlayer.posY + MIN_HEIGHT_OFFSET
                else position.distanceTo(mc.thePlayer.positionVector) < PARTICLE_DISAPPEAR_RANGE && MathHelper.sqrt_float(motionX * motionX + motionZ * motionZ) > 0.15

        if (isAirBlock && shouldKeepAlive) {
            val newPosition = position.addVector(
                motionX.toDouble(),
                motionY.toDouble(),
                motionZ.toDouble()
            )

            applyPhysics()

            renderParticleTrajectory(newPosition)

            val prevPos = position
            position = newPosition

            return !applyCollisionEffect(prevPos, newPosition)
        }

        return false
    }

    private fun applyPhysics() {
        motionX *= motionData.y
        motionZ *= motionData.y
        if(withGravity) motionY -= motionData.x
        motionY *= motionData.y
    }

    private fun renderParticleTrajectory(newPosition: Vec3) {
        val pathPoints = CustomPathHelper.findPathDirectly(
            position.xCoord, position.yCoord, position.zCoord,
            newPosition.xCoord, newPosition.yCoord, newPosition.zCoord,
            maxDelta.toDouble()
        )

        pathPoints.forEach { point ->
            val particleType = getParticleType()
            mc.effectRenderer.spawnEffectParticle(
                particleType.particleID,
                point.xCoord, point.yCoord, point.zCoord,
                0.0, 0.0, 0.0,
                0
            )
        }
    }

    private fun getParticleType(): EnumParticleTypes {
        return when (particleType) {
            "Flame" -> EnumParticleTypes.FLAME
            "Water" -> EnumParticleTypes.WATER_SPLASH
            "Slime" -> EnumParticleTypes.SLIME
            "Snowball" -> EnumParticleTypes.SNOWBALL
            else -> EnumParticleTypes.FLAME
        }
    }
}