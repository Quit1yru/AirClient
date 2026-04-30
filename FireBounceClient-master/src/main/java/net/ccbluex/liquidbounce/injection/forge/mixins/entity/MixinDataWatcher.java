/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.features.module.modules.exploit.ClientFixes;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.DataWatcher;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

@Mixin(DataWatcher.class)
@SideOnly(Side.CLIENT)
public class MixinDataWatcher{
    @Final
    @Shadow
    private Map<Integer, DataWatcher.WatchableObject> watchedObjects;
    @Shadow
    private ReadWriteLock lock;

    @Shadow
    private DataWatcher.WatchableObject getWatchedObject(int p_getWatchedObject_1_) {
        this.lock.readLock().lock();

        DataWatcher.WatchableObject lvt_2_1_;
        try {
            lvt_2_1_ = this.watchedObjects.get(p_getWatchedObject_1_);
        } catch (Throwable lvt_3_1_) {
            CrashReport lvt_4_1_ = CrashReport.makeCrashReport(lvt_3_1_, "Getting synched entity data");
            CrashReportCategory lvt_5_1_ = lvt_4_1_.makeCategory("Synched entity data");
            lvt_5_1_.addCrashSection("Data ID", p_getWatchedObject_1_);
            throw new ReportedException(lvt_4_1_);
        }

        this.lock.readLock().unlock();
        return lvt_2_1_;
    }

    /**
     * @author _0x16z
     * @reason Fucking exploits
     */
    @Overwrite
    public byte getWatchableObjectByte(int p_getWatchableObjectByte_1_) {
        if(!(ClientFixes.INSTANCE.handleEvents()&& ClientFixes.INSTANCE.getDataWatcherFix()))return (Byte)this.getWatchedObject(p_getWatchableObjectByte_1_).getObject();
        else{
            try {
                return (Byte) this.getWatchedObject(p_getWatchableObjectByte_1_).getObject();
            } catch (ClassCastException e) {
                this.getWatchedObject(p_getWatchableObjectByte_1_).setObject(0);
                return 0;
            }
        }
    }

    /**
     * @author _0x16z
     * @reason Fucking exploits
     */
    @Overwrite
    public float getWatchableObjectFloat(int p_getWatchableObjectFloat_1_) {
        if(!(ClientFixes.INSTANCE.handleEvents()&& ClientFixes.INSTANCE.getDataWatcherFix()))return (Float)this.getWatchedObject(p_getWatchableObjectFloat_1_).getObject();
        else {
            try {
                return (Float) this.getWatchedObject(p_getWatchableObjectFloat_1_).getObject();
            } catch (ClassCastException e) {
                this.getWatchedObject(p_getWatchableObjectFloat_1_).setObject(20);
                return 20;
            }
        }
    }
}
