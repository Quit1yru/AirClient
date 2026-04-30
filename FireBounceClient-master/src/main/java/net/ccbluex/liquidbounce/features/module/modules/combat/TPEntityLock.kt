package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.pathfinder.CustomPathHelper
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3


object TPEntityLock : Module("TPEntityLock", Category.COMBAT) {
    private val rangeValue by int("SelectRange", 512, 10..512, "b")
    private val fovValue by float("Fov", 180F, 0F..180F, "°")
    private val pathType by choices("PathType", arrayOf("Smart", "Direct", "Immediate", "Flux"), "Smart")
    private val moveDistance by float("MoveDistance", 8.0F, 0.1F..10.0F){pathType!="Flux"}
    private val sortBy by choices("SortBy", arrayOf("Health", "DistanceFarToClose", "DistanceCloseToFar"), "Health")
    private val packetMode by choices("PacketMode", arrayOf("C04", "C06"), "C04")
    private val loop by int("SearchBlocks", 1500, 500..32767){pathType=="Smart"}
    private val depth by int("SearchRange", 4, 1..100){pathType=="Smart"}
    private val maxKR by float("MaxRange", 0.1F, 0F..6F)
    private val ignoreUnreachableTarget by boolean("IgnoreUnreachableTargets", true)

    @Suppress("unused")
    val onWorldEvent = handler<WorldEvent> {
        state = false
        Chat.print("Disabled TPEntityLock bc world change")
    }

    val onUpdate = handler<UpdateEvent> {
        runAttack()
    }

    private fun calcPath(fromPos: Vec3, finalItPosition: Vec3): List<Vec3>{
        var path = CustomPathHelper.findPathDirectly(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
            finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,
            moveDistance.toDouble())
        when(pathType){
            "Immediate" -> {
                path = CustomPathHelper.findPathImmediately(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,

                    moveDistance.toDouble())
            }
            "Smart" -> {
                path = CustomPathHelper.findTeleportPathPointToPoint(
                    fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,
                    moveDistance.toDouble(), loop, depth)
            }
            "Flux" -> { //上个世纪的老东西
                val pathFlux1337 = CustomPathHelper.fluxGetPath(fromPos, finalItPosition)
                pathFlux1337.forEach {
                    path.add(Vec3(it.x+0.5, it.y + 0.0, it.z+0.5))
                }
            }
        }
        path.add(finalItPosition)
        return path
    }
    private fun runAttack() {
        if (mc.thePlayer == null || mc.theWorld == null) return

        val targets = mutableListOf<EntityLivingBase>()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true) && mc.thePlayer.getDistanceToEntity(
                    entity
                ) <= rangeValue
            ) {

                if (fovValue < 180F && RotationUtils.getRotationDifference(entity) > fovValue)
                    continue

                targets.add(entity)
            }
        }
        if(ignoreUnreachableTarget)targets.removeIf {
            calcPath(mc.thePlayer.positionVector, it.positionVector).last().distanceTo(it.positionVector)>5.9F
        }
        when(sortBy){
            "Health" -> targets.sortBy { it.health }
            "DistanceCloseToFar" -> targets.sortBy { mc.thePlayer.getDistanceToEntity(it) }
            "DistanceFarToClose" -> targets.sortByDescending { mc.thePlayer.getDistanceToEntity(it) }
        }

        if (targets.isEmpty()) {
            return
        }
        val target = targets[0]
        if(target.getDistanceToEntity(mc.thePlayer)>maxKR){
            val path = calcPath(mc.thePlayer.positionVector, target.positionVector)
            if(path.isNotEmpty()){
                path.forEach { sendPosition(it) }
            }
            mc.thePlayer.setPositionAndUpdate(path.last().xCoord, path.last().yCoord, path.last().zCoord)
        }
    }
    fun sendPosition(pos: Vec3){
        if(packetMode=="C04") PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(pos.xCoord,pos.yCoord, pos.zCoord, true))
        else  PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C06PacketPlayerPosLook(
                pos.xCoord,
                pos.yCoord,
                pos.zCoord,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                true
            )
        )
    }
}