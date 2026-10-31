package dev.vortex.mixin;

import dev.vortex.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    /**
     * Skips creating cosmetic particles before they cost anything (no object allocation, no tick, no draw).
     * Only distance, explosion and weather particles are filtered; critical/damage/etc. are untouched.
     */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void vortex$filter(ParticleEffect effect, double x, double y, double z,
                               double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        try {
            if (!ModuleManager.PARTICLES.enabled) return;
            ParticleType<?> t = effect.getType();
            if (ModuleManager.PARTICLE_EXPLOSIONS.getBool()
                    && (t == ParticleTypes.EXPLOSION || t == ParticleTypes.EXPLOSION_EMITTER)) {
                cir.setReturnValue(null);
                return;
            }
            if (ModuleManager.PARTICLE_WEATHER.getBool() && (t == ParticleTypes.RAIN || t == ParticleTypes.SPLASH)) {
                cir.setReturnValue(null);
                return;
            }
            Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
            if (cam != null && cam.isReady()) {
                double max = ModuleManager.PARTICLE_DIST.getInt();
                double dx = cam.getPos().x - x, dy = cam.getPos().y - y, dz = cam.getPos().z - z;
                if (dx * dx + dy * dy + dz * dz > max * max) cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }
}
