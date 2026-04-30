package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object KillEffect : Module("KillEffect", Category.RENDER) {
    private val mode by multiChoices("Mode", arrayOf(
        "LightningBolt",
        "Flame",
        "Smoke",
        "Water",
        "Love",
        "Blood",
        "Squid",
        "Enchant",
        "Explosion",
    ), arrayOf("LightningBolt"))
    private val timeOut by int("TimeOut",3,1..10,"s")

    private val tipKills by boolean("TipKills", false)

    private var target: EntityLivingBase? = null
    private var targetTimer = MSTimer()
    private var squid: EntitySquid? = null
    private var kills = 0
    private var percent = 0.0

    override fun onDisable() {
        if (tipKills) {
            kills = 0
        }
        target = null
        squid = null
    }

    override fun onEnable() {
        if (tipKills) {
            kills = 0
        }
        targetTimer.reset()
    }

    val onUpdate = handler<UpdateEvent> { event ->
        // 处理鱿鱼特效动画
        if ("Squid" in mode && squid != null) {
            handleSquidEffect()
        }
        val currentTarget = target
        if (currentTarget != null && targetTimer.hasTimePassed(timeOut * 1000L)) {
            target = null
        }
        // 检测目标死亡
        checkTargetDeath()
    }

    val onAttack = handler<AttackEvent> { e ->
        if (e.targetEntity is EntityLivingBase) {
            target = e.targetEntity
            targetTimer.reset()
        }
    }

    private fun handleSquidEffect() {
        val currentSquid = squid ?: return

        if (mc.theWorld.loadedEntityList.contains(currentSquid)) {
            if (percent < 1.0) {
                percent += Math.random() * 0.048
            }
            if (percent >= 1.0) {
                percent = 0.0
                repeat(8) {
                    mc.effectRenderer.emitParticleAtEntity(currentSquid, EnumParticleTypes.FLAME)
                }
                mc.theWorld.removeEntityFromWorld(currentSquid.entityId)
                squid = null
                return
            }
        } else {
            percent = 0.0
        }

        // 简单的动画计算
        val easeInOutCirc = easeInOutCirc(1.0 - percent)
        currentSquid.setPositionAndUpdate(
            currentSquid.posX,
            currentSquid.posY + easeInOutCirc * 0.9,
            currentSquid.posZ
        )

        // 固定鱿鱼角度
        currentSquid.squidPitch = 0.0f
        currentSquid.prevSquidPitch = 0.0f
        currentSquid.squidYaw = 0.0f
        currentSquid.squidRotation = 90.0f
    }

    private fun checkTargetDeath() {
        val currentTarget = target ?: return

        if ((currentTarget.health <= 0.0f || currentTarget.isDead) && !mc.theWorld.loadedEntityList.contains(currentTarget)) {
            // 击杀提示
            if (tipKills) {
                kills++
                chat("§aKilled $kills Players.")
            }

            // 执行击杀特效
            executeKillEffect(currentTarget)
            target = null
        }
    }

    private fun executeKillEffect(target: EntityLivingBase) {
        if ("LightningBolt" in mode) {
            val lightning = EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ)
            mc.theWorld.addEntityToWorld((-Math.random() * 100000.0).toInt(), lightning)

            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                "ambient.weather.thunder", 1.0f, 1.0f, false)
            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                "random.explode", 1.0f, 1.0f, false)
            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                "item.fireCharge.use", 1.0f, 1.0f, false)
        }
        if ("Explosion" in mode) {
             mc.effectRenderer.spawnEffectParticle(
                 EnumParticleTypes.EXPLOSION_HUGE.particleID,
                 target.posX, target.posY + target.height / 2, target.posZ,
                 0.0, 0.0, 0.0,
                 0
             )
        }
        if ("Squid" in mode) {
            squid = EntitySquid(mc.theWorld).apply {
                setPosition(target.posX, target.posY, target.posZ)
            }
            mc.theWorld.addEntityToWorld(-847815, squid)
        }

        if ("Flame" in mode) {
            repeat(25) {  // 从8增加到25
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.FLAME)
                // 添加一些随机偏移让效果更分散
                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.FLAME.particleID,
                    target.posX + nextFloat(-1f, 1f),
                    target.posY + nextFloat(0f, target.height),
                    target.posZ + nextFloat(-1f, 1f),
                    0.0, 0.0, 0.0,
                    0
                )
            }
        }

        if ("Smoke" in mode) {
            repeat(20) {  // 从8增加到20
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.SMOKE_LARGE)
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.SMOKE_NORMAL)
            }
        }

        if ("Water" in mode) {
            repeat(30) {  // 从8增加到30
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.WATER_DROP)
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.WATER_SPLASH)
            }
            mc.theWorld.playSound(
                target.posX, target.posY, target.posZ,
                "liquid.splash", 0.8f, 1.0f, false
            )
        }

        if ("Love" in mode) {
            repeat(25) {  // 从8增加到25
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.HEART)
                // 添加一些漂浮的心形粒子
                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.HEART.particleID,
                    target.posX + nextFloat(-2f, 2f),
                    target.posY + nextFloat(0f, 3f),
                    target.posZ + nextFloat(-2f, 2f),
                    0.0, 0.1, 0.0,
                    0
                )
            }
            mc.theWorld.playSound(
                target.posX, target.posY, target.posZ,
                "game.player.levelup", 0.5f, 1.2f, false
            )
        }

        if ("Blood" in mode) {
            repeat(40) {
                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.BLOCK_CRACK.particleID,
                    target.posX + nextFloat(-1f, 1f),
                    target.posY + nextFloat(0f, target.height),
                    target.posZ + nextFloat(-1f, 1f),
                    nextFloat(-0.3f, 0.3f).toDouble(),
                    nextFloat(-0.1f, 0.3f).toDouble(),
                    nextFloat(-0.3f, 0.3f).toDouble(),
                    net.minecraft.block.Block.getStateId(Blocks.redstone_block.defaultState)
                )

                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.REDSTONE.particleID,
                    target.posX + nextFloat(-1.5f, 1.5f),
                    target.posY + nextFloat(0f, target.height),
                    target.posZ + nextFloat(-1.5f, 1.5f),
                    0.0, 0.0, 0.0,
                    0
                )
            }
        }

        if ("Enchant" in mode) {
            // 多重魔法阵效果
            repeat(3) { layer ->
                val layerRadius = 1.5 + layer * 0.8
                val layerHeight = target.height / 2.0 + layer * 0.5

                repeat(30) {  // 每层30个粒子
                    val angle = 2 * Math.PI * it / 30.0
                    val x = target.posX + layerRadius * cos(angle)
                    val y = target.posY + layerHeight
                    val z = target.posZ + layerRadius * sin(angle)

                    mc.effectRenderer.spawnEffectParticle(
                        EnumParticleTypes.ENCHANTMENT_TABLE.particleID,
                        x, y, z,
                        nextFloat(-0.1f, 0.1f).toDouble(),
                        nextFloat(0.05f, 0.2f).toDouble(),
                        nextFloat(-0.1f, 0.1f).toDouble(),
                        0
                    )
                }
            }

            // 添加上升的魔法粒子
            repeat(15) {
                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.ENCHANTMENT_TABLE.particleID,
                    target.posX + nextFloat(-2f, 2f),
                    target.posY + nextFloat(0f, 1f),
                    target.posZ + nextFloat(-2f, 2f),
                    nextFloat(-0.05f, 0.05f).toDouble(),
                    nextFloat(0.1f, 0.3f).toDouble(),
                    nextFloat(-0.05f, 0.05f).toDouble(),
                    0
                )
            }

            // 添加紫色粒子增强效果
            repeat(10) {
                mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.SPELL_WITCH.particleID,
                    target.posX + nextFloat(-1.5f, 1.5f),
                    target.posY + nextFloat(0f, target.height),
                    target.posZ + nextFloat(-1.5f, 1.5f),
                    0.0, 0.1, 0.0,
                    0
                )
            }

            mc.theWorld.playSound(
                target.posX, target.posY, target.posZ,
                "block.enchantment_table.use", 1.2f, 0.9f + nextFloat(0f, 0.3f), false
            )
            mc.theWorld.playSound(
                target.posX, target.posY, target.posZ,
                "block.enchantment_table.use", 0.8f, 1.1f + nextFloat(0f, 0.2f), false
            )
        }
    }

    private fun easeInOutCirc(x: Double): Double {
        return if (x < 0.5) {
            (1.0 - sqrt(1.0 - (2.0 * x).pow(2.0))) / 2.0
        } else {
            (sqrt(1.0 - (-2.0 * x + 2.0).pow(2.0)) + 1.0) / 2.0
        }
    }

    private fun nextFloat(startInclusive: Float, endInclusive: Float): Float {
        if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0f) {
            return startInclusive
        }
        return (startInclusive + (endInclusive - startInclusive) * Math.random()).toFloat()
    }
}