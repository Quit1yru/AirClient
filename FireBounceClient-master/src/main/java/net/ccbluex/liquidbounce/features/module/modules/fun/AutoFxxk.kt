package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.util.EnumParticleTypes

object AutoFxxk : Module("AutoFxxk", Category.FUN) {
    val onUpdate = handler<UpdateEvent> { _ ->
        if(mc.objectMouseOver.entityHit!=null){
            val entity = mc.objectMouseOver.entityHit
            if(mc.thePlayer.getDistanceToEntity(entity)<1.25&& RotationUtils.angleDifference(entity.rotationYaw, mc.thePlayer.rotationYaw)<=30F && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)){
                mc.gameSettings.keyBindSneak.pressed=mc.thePlayer.ticksExisted%4<=1

                val midPointX2 = entity.positionVector.add(mc.thePlayer.positionVector)
                if(mc.thePlayer.ticksExisted%4<=1)repeat(20){
                    mc.effectRenderer.spawnEffectParticle(
                        EnumParticleTypes.WATER_SPLASH.particleID,
                        midPointX2.xCoord/2, midPointX2.yCoord/2, midPointX2.zCoord/2,
                        0.0, 0.0, 0.0,
                        0
                    )
                }
            }
        }
    }
}