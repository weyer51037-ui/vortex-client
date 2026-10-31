package dev.vortex.mixin;

import dev.vortex.module.ModuleManager;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    /**
     * Optional distance cull. FAIRNESS RULES (do not remove):
     *  - players are never culled
     *  - bosses and any large entity are never culled
     *  - minimum configurable distance is 32 blocks
     * This only skips drawing; it never changes what the server sends or what the game knows.
     */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void vortex$cull(E entity, Frustum frustum, double x, double y, double z,
                                                CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!ModuleManager.ENTITY_CULL.enabled) return;
            if (entity instanceof PlayerEntity || entity instanceof EnderDragonEntity || entity instanceof WitherEntity) return;
            if (entity.getWidth() > 3.0f || entity.getHeight() > 4.0f) return;
            double max = ModuleManager.ENTITY_DIST.getInt();
            if (entity.squaredDistanceTo(x, y, z) > max * max) cir.setReturnValue(false);
        } catch (Throwable ignored) {
        }
    }
}
