package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.minecraft.enchantment.Enchantment
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction
import net.minecraft.potion.Potion


object IllegalItemCreator : Module("IllegalItemCreator", Category.FUN) {
    private val mode by choices("Mode", arrayOf("OPWeapon", "OPArmor", "CrashAnvil", "TrollPotion", "OPPotion"), "OPWeapon")
    val badPotionId = arrayOf(Potion.wither.id, Potion.moveSpeed.id, Potion.confusion.id, Potion.jump.id, Potion.harm.id, Potion.poison.id, Potion.moveSlowdown.id, Potion.weakness.id, Potion.blindness.id, Potion.digSlowdown.id, Potion.hunger.id)
    val goodPotionId = arrayOf(Potion.regeneration.id, Potion.damageBoost.id, Potion.resistance.id, Potion.fireResistance.id, Potion.invisibility.id, Potion.nightVision.id, Potion.waterBreathing.id, Potion.absorption.id, Potion.healthBoost.id, Potion.saturation.id)
    override fun onEnable() {
        super.onEnable()
        if(mc.thePlayer==null)return
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            Chat.print("Creative mode only.")
            state=false
        } else if (mc.thePlayer.inventory.getStackInSlot(0) != null) {
            Chat.print("Please clear the first slot in your hotbar.")
            state=false
        } else {
            when(mode){
                "OPWeapon"->{
                    val item = ItemStack(Items.diamond_pickaxe, 1, 0)
                    item.addEnchantment(Enchantment.sharpness, 127)
                    item.addEnchantment(Enchantment.efficiency, 127)
                    item.addEnchantment(Enchantment.fortune, 127)
                    item.addEnchantment(Enchantment.unbreaking, 127)
                    mc.thePlayer.sendQueue.addToSendQueue(C10PacketCreativeInventoryAction(36, item))
                }

                "OPArmor"->{
                    val item = ItemStack(Items.diamond_helmet, 1, 0)
                    item.addEnchantment(Enchantment.protection, 127)
                    item.addEnchantment(Enchantment.projectileProtection, 127)
                    item.addEnchantment(Enchantment.fireProtection, 127)
                    item.addEnchantment(Enchantment.blastProtection, 127)
                    item.addEnchantment(Enchantment.thorns, 127)
                    item.addEnchantment(Enchantment.unbreaking, 127)
                    mc.thePlayer.sendQueue.addToSendQueue(C10PacketCreativeInventoryAction(36, item))
                }
                "CrashAnvil"->{
                    val stack = ItemStack(Item.getItemFromBlock(Blocks.anvil))
                    stack.setItemDamage(2147483647)
                    stack.setStackDisplayName("Place me Â\u00a7c<3")
                    mc.thePlayer.sendQueue.addToSendQueue(C10PacketCreativeInventoryAction(36, stack))
                    mc.thePlayer.closeScreen()
                    Chat.print("Crashable anvil created.")
                }
                "OPPotion"->{
                    val stack = ItemStack(Items.potionitem, 64)
                    stack.setItemDamage(16384)
                    val effects = NBTTagList()

                    for (i in goodPotionId) {
                        val effect = NBTTagCompound()
                        effect.setInteger("Amplifier", 127)
                        effect.setInteger("Duration", Int.MAX_VALUE)
                        effect.setInteger("Id", i)
                        effects.appendTag(effect)
                    }

                    stack.setTagInfo("CustomPotionEffects", effects)
                    stack.setStackDisplayName("Beautiful Firefly potion")
                    mc.thePlayer.sendQueue.addToSendQueue(C10PacketCreativeInventoryAction(36, stack))
                    Chat.print("Potion created.")
                }
                "TrollPotion"->{
                    val stack = ItemStack(Items.potionitem, 64)
                    stack.setItemDamage(16384)
                    val effects = NBTTagList()

                    for (i in badPotionId) {
                        val effect = NBTTagCompound()
                        effect.setInteger("Amplifier", 127)
                        effect.setInteger("Duration", Int.MAX_VALUE)
                        effect.setInteger("Id", i)
                        effects.appendTag(effect)
                    }

                    stack.setTagInfo("CustomPotionEffects", effects)
                    stack.setStackDisplayName("Â\u00a74Troll Â\u00a7cPot.")
                    mc.thePlayer.sendQueue.addToSendQueue(C10PacketCreativeInventoryAction(36, stack))
                    Chat.print("Potion created. Trololo!")
                }
            }
            state=false
        }
    }
}