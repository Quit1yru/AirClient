package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.airTicks
import net.ccbluex.liquidbounce.utils.extensions.getPing
import kotlin.math.sqrt

object PlayerInfoDisplay : Module("PlayerInfoDisplay", Category.ALERTS) {
    private val displayInfo by choices("DisplayInfo",arrayOf(
        "Health",
        "Food",
        "SprintingState",
        "BlockingState",
        "SneakingState",
        "XPLevel",
        "ArmorPoint",
        "Ping",
        "HeldItemDamage",
        "FallDistance",
        "DistanceWalked",
        "OnGround",
        "AirTicks",
        "Velocity",
        "X",
        "Y",
        "Z",
        "Yaw",
        "Pitch",
        "MotionX",
        "MotionY",
        "MotionZ",
        "InWater",
        "InLava",
        "InWeb",
        "Fire",
        "FireResistance",
        "IsInCreative",
        "HeldItem",
        "Experience",
        "ExperienceTotal",
        "ExperienceLevel",
        "WalkSpeed",
        "FlySpeed",
        "HurtTime",
        "HurtResistance",
    ),"Health")
    private val serverSideOrClientSide by choices("ClientSideOrServerSide",arrayOf("ServerSide","ClientSide"),"ClientSide") {displayInfo in arrayOf("SprintingState")}

    override val tag: String?
        get() = when (displayInfo) {
            "HurtTime" -> "${mc.thePlayer.hurtTime}/${mc.thePlayer.maxHurtTime}"
            "HurtResistance" -> "${mc.thePlayer.hurtResistantTime}/${mc.thePlayer.maxHurtResistantTime}"
            "Health" -> mc.thePlayer.health
            "Food" -> mc.thePlayer.foodStats.foodLevel
            "SprintingState" -> if (serverSideOrClientSide == "ClientSide") mc.thePlayer.isSprinting else mc.thePlayer.serverSprintState
            "BlockingState" -> mc.thePlayer.isBlocking
            "SneakingState" -> mc.thePlayer.isSneaking
            "XPLevel" -> mc.thePlayer.experienceLevel
            "ArmorPoint" -> mc.thePlayer.totalArmorValue
            "Ping" -> mc.thePlayer.getPing()
            "HeldItemDamage" -> mc.thePlayer.heldItem?.itemDamage ?: "None"
            "FallDistance" -> mc.thePlayer.fallDistance
            "DistanceWalked" -> mc.thePlayer.distanceWalkedModified
            "OnGround" -> mc.thePlayer.onGround
            "AirTicks" -> mc.thePlayer.airTicks
            "Velocity" -> "${String.format("%.2f",
                sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ)
            )} blocks/tick"
            "X" -> String.format("%.2f", mc.thePlayer.posX)
            "Y" -> String.format("%.2f", mc.thePlayer.posY)
            "Z" -> String.format("%.2f", mc.thePlayer.posZ)
            "Yaw" -> String.format("%.2f", mc.thePlayer.rotationYaw)
            "Pitch" -> String.format("%.2f", mc.thePlayer.rotationPitch)
            "MotionX" -> String.format("%.4f", mc.thePlayer.motionX)
            "MotionY" -> String.format("%.4f", mc.thePlayer.motionY)
            "MotionZ" -> String.format("%.4f", mc.thePlayer.motionZ)
            "InWater" -> mc.thePlayer.isInWater
            "InLava" -> mc.thePlayer.isInLava
            "InWeb" -> mc.thePlayer.isInWeb
            "Fire" -> mc.thePlayer.fire
            "FireResistance" -> mc.thePlayer.fireResistance
            "IsInCreative" -> mc.thePlayer.capabilities.isCreativeMode
            "HeldItem" -> mc.thePlayer.heldItem?.displayName ?: "None"
            "Experience" -> mc.thePlayer.experience
            "ExperienceTotal" -> mc.thePlayer.experienceTotal
            "ExperienceLevel" -> mc.thePlayer.experienceLevel
            "WalkSpeed" -> mc.thePlayer.capabilities.walkSpeed
            "FlySpeed" -> mc.thePlayer.capabilities.flySpeed
            else -> mc.thePlayer.totalArmorValue
        }.toString()

}