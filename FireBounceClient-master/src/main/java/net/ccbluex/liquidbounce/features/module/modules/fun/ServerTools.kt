/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.potion.Potion

object ServerTools : Module("ServerTools", Category.FUN, subjective = true) {

    val serverMode by choices("ServerMode", arrayOf("Loyisa"), "Loyisa")
    val loyisaModule by multiChoices("LoyisaModules", arrayOf("AutoEffect"), arrayOf("AutoEffect")){serverMode=="Loyisa"}
    val effects by multiChoices("LoyisaEffects", arrayOf("SPEED", "JUMP", "INCREASE_DAMAGE", "FAST_DIGGING", "REGENERATION", "DAMAGE_RESISTANCE", "HEALTH_BOOST", "ABSORPTION"), arrayOf("INCREASE_DAMAGE")){serverMode=="Loyisa"&&loyisaModule.any{it=="AutoEffect"}}

    val onUpdate = handler<UpdateEvent>{
        val player = mc.thePlayer?:return@handler
        if(serverMode=="Loyisa") {
            if (loyisaModule.any { it == "AutoEffect" }){
                if(player.ticksExisted%50==0){
                    effects.forEach {
                        when(it){
                            "SPEED"->{
                                if(!player.isPotionActive(Potion.moveSpeed.id)){
                                    mc.thePlayer.sendChatMessage("/effect SPEED 9999 10")
                                    return@handler
                                }
                            }
                            "JUMP"->{
                                if(!player.isPotionActive(Potion.jump.id)){
                                    mc.thePlayer.sendChatMessage("/effect JUMP 9999 10")
                                    return@handler
                                }
                            }
                            "INCREASE_DAMAGE"->{
                                if(!player.isPotionActive(Potion.damageBoost.id)){
                                    mc.thePlayer.sendChatMessage("/effect INCREASE_DAMAGE 9999 10")
                                    return@handler
                                }
                            }
                            "FAST_DIGGING"-> {
                                if (!player.isPotionActive(Potion.digSpeed.id)) {
                                    mc.thePlayer.sendChatMessage("/effect FAST_DIGGING 9999 10")
                                    return@handler
                                }
                            }
                            "REGENERATION"->{
                                if(!player.isPotionActive(Potion.regeneration.id))
                                    mc.thePlayer.sendChatMessage("/effect REGENERATION 9999 10")
                                return@handler
                            }
                            "DAMAGE_RESISTANCE"->{
                                if(!player.isPotionActive(Potion.resistance.id))
                                    mc.thePlayer.sendChatMessage("/effect DAMAGE_RESISTANCE 9999 10")
                                return@handler
                            }
                            "HEALTH_BOOST"->{
                                if(!player.isPotionActive(Potion.healthBoost.id))
                                    mc.thePlayer.sendChatMessage("/effect HEALTH_BOOST 9999 10")
                                return@handler
                            }
                            "ABSORPTION"-> {
                                if (!player.isPotionActive(Potion.absorption.id))
                                    mc.thePlayer.sendChatMessage("/effect ABSORPTION 9999 10")
                                return@handler
                            }
                        }
                    }
                }
            }
        }
    }
}