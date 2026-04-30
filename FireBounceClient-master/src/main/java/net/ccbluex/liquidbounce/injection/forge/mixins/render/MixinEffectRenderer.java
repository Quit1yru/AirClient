/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.NoBlockParticles;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityParticleEmitter;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

@Mixin(EffectRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEffectRenderer {

    @Shadow
    protected abstract void updateEffectLayer(int layer);

    @Shadow
    private List<EntityParticleEmitter> particleEmitters;

    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void addBlockDestroyEffects(BlockPos pos, IBlockState state, CallbackInfo ci) {
        NoBlockParticles noBlockParticles = NoBlockParticles.INSTANCE;
        if (noBlockParticles.handleEvents() && noBlockParticles.getBlockParticles()) {
            ci.cancel();
        }
    }
    @Inject(method = "addBlockHitEffects(Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void addBlockHitEffects(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        NoBlockParticles noBlockParticles = NoBlockParticles.INSTANCE;
        if (noBlockParticles.handleEvents() && noBlockParticles.getBreakBlockParticles()) {
            ci.cancel();
        }
    }
    /**
     * @author Mojang
     * @author Marco
     * @reason Fix
     */
    @Overwrite
    public void updateEffects() {
        try {
            for (int i = 0; i < 4; ++i)
                updateEffectLayer(i);

            for (final Iterator<EntityParticleEmitter> it = particleEmitters.iterator(); it.hasNext(); ) {
                final EntityParticleEmitter entityParticleEmitter = it.next();

                entityParticleEmitter.onUpdate();

                if (entityParticleEmitter.isDead)
                    it.remove();
            }
        } catch(final ConcurrentModificationException ignored) {
        }
    }
}