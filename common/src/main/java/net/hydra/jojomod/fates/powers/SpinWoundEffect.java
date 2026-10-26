package net.hydra.jojomod.fates.powers;

import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** A short, bounded adaptation of transmitted rotation, separate from Tusk's permanent effect. */
public class SpinWoundEffect extends MobEffect {
    public SpinWoundEffect() { super(MobEffectCategory.HARMFUL, 0xE6B832); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity target, int amplifier) {
        if (!(target.level() instanceof ServerLevel server) || !target.isAlive()) return;
        if (target.tickCount % 20 == 0) target.hurt(target.damageSources().magic(), 1F);
        if (target.tickCount % 5 != 0) return;
        double angle = target.tickCount * 0.65;
        for (int i = 0; i < 4; i++) {
            double theta = angle + i * Math.PI / 2;
            server.sendParticles(SteelBallEntity.GOLDEN_PARTICLE,
                    target.getX() + Math.cos(theta) * target.getBbWidth(),
                    target.getY() + 0.2 + i * 0.07,
                    target.getZ() + Math.sin(theta) * target.getBbWidth(), 1, 0, 0, 0, 0);
        }
    }
}
