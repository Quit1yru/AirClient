package net.ccbluex.liquidbounce.utils.skid.nightx

import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper
import kotlin.math.atan2

object NightXRotationUtils {
    val mc = Minecraft.getMinecraft()!!
    fun getRotations(posX: Double, posY: Double, posZ: Double): Rotation {
        val player = mc.thePlayer
        val x = posX - player.posX
        val y = posY - (player.posY + player.getEyeHeight().toDouble())
        val z = posZ - player.posZ
        val dist = MathHelper.sqrt_double(x * x + z * z).toDouble()
        val yaw = (atan2(z, x) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        val pitch = (-(atan2(y, dist) * 180.0 / 3.141592653589793)).toFloat()
        return Rotation(yaw, pitch)
    }
    fun getRotationsEntity(entity: EntityLivingBase): Rotation {
        return getRotations(
            entity.posX,
            entity.posY + entity.eyeHeight - 0.4,
            entity.posZ
        )
    }
    fun getRotations1(posX: Double, posY: Double, posZ: Double): FloatArray {
        val player = mc.thePlayer
        val x = posX - player.posX
        val y = posY - (player.posY + player.getEyeHeight().toDouble())
        val z = posZ - player.posZ
        val dist = MathHelper.sqrt_double(x * x + z * z).toDouble()
        val yaw = (atan2(z, x) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = -(atan2(y, dist) * 180.0 / Math.PI).toFloat()
        return floatArrayOf(yaw, pitch)
    }
}