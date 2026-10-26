package net.hydra.jojomod.entity.projectile;

import net.hydra.jojomod.entity.ModEntities;
import net.hydra.jojomod.item.SpinCharge;
import net.hydra.jojomod.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** A recoverable Spin weapon, separate from the crossbow's iron-ball ammunition. */
public class SteelBallEntity extends IronBallEntity {
    public static final DustParticleOptions GOLDEN_PARTICLE =
            new DustParticleOptions(new Vector3f(1F, 0.72F, 0.12F), 1F);
    private static final EntityDataAccessor<Boolean> GOLDEN =
            SynchedEntityData.defineId(SteelBallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(SteelBallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TECHNIQUE =
            SynchedEntityData.defineId(SteelBallEntity.class, EntityDataSerializers.INT);
    private float charge;
    private int age;
    private int bounces;
    private int redirects;
    private int bounceLimit;
    private int returnAge = SpinProjectileRules.OUTBOUND_TICKS;
    private boolean recoverable;

    public SteelBallEntity(EntityType<? extends SteelBallEntity> type, Level level) {
        super(type, level);
        // The crossbow ball preserves its initial velocity for 12 ticks. A Spin
        // ricochet must instead keep the reflected velocity, even just after release.
        falloff = 0;
    }

    public SteelBallEntity(Level level, Player owner, ItemStack stack, float charge, boolean golden) {
        this(ModEntities.STEEL_BALL, level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setArrow(stack.copyWithCount(1));
        this.charge = SpinCharge.clamp(charge);
        entityData.set(GOLDEN, golden);
        bounceLimit = golden ? 3 : 2;
        recoverable = !owner.getAbilities().instabuild;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GOLDEN, false);
        entityData.define(RETURNING, false);
        entityData.define(TECHNIQUE, 0);
    }

    public void setSpinTechnique(int technique) { entityData.set(TECHNIQUE, Math.max(0, Math.min(2, technique))); }
    public int getSpinTechnique() { return entityData.get(TECHNIQUE); }

    public void setBounceLimit(int limit) { bounceLimit = Math.max(2, Math.min(4, limit)); }

    public boolean redirect(Player player, Vec3 target, int limit) {
        if (level().isClientSide || !SpinProjectileRules.canRedirect(getOwner() == player,
                isReturning(), redirects, limit, age)) return false;
        Vec3 direction = target.subtract(position());
        if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 0.01) return false;
        setDeltaMovement(direction.normalize().scale(Math.max(1.4, Math.min(2.6, getDeltaMovement().length()))));
        returnAge = SpinProjectileRules.returnDeadline(age, returnAge);
        redirects++;
        hasImpulse = true;
        return true;
    }

    public boolean recall(Player player) {
        if (level().isClientSide || getOwner() != player || isReturning()) return false;
        beginReturn();
        return true;
    }

    public boolean isGolden() { return entityData.get(GOLDEN); }
    public boolean isReturning() { return entityData.get(RETURNING); }

    @Override
    public void tick() {
        if (isReturning()) {
            noPhysics = true;
            setNoGravity(true);
        }
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide) {
            // Two counterposed points describe a spiral around the direction of travel.
            Vec3 forward = getDeltaMovement().normalize();
            Vec3 axis = Math.abs(forward.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = forward.cross(axis).normalize();
            Vec3 up = forward.cross(right).normalize();
            double angle = tickCount * 0.9;
            Vec3 offset = right.scale(Math.cos(angle) * 0.22).add(up.scale(Math.sin(angle) * 0.22));
            for (int side : new int[]{-1, 1}) {
                Vec3 p = position().add(offset.scale(side));
                level().addParticle(isGolden() ? GOLDEN_PARTICLE : ParticleTypes.CRIT,
                        p.x, p.y, p.z, 0, 0, 0);
            }
            return;
        }
        age++;
        if (age >= returnAge) beginReturn();
        Entity owner = getOwner();
        if (age >= SpinProjectileRules.LIFETIME_TICKS || (owner != null && !owner.isAlive())) {
            recover(null);
            return;
        }
        if (isReturning() && owner != null) {
            Vec3 target = owner.getEyePosition().add(0, -0.3, 0);
            Vec3 toOwner = target.subtract(position());
            if (toOwner.lengthSqr() < 2.25) {
                recover(owner instanceof Player player ? player : null);
                return;
            }
            setDeltaMovement(toOwner.normalize().scale(Math.min(1.8, toOwner.length())));
            hasImpulse = true;
        }
    }

    private void beginReturn() {
        if (!isReturning()) {
            entityData.set(RETURNING, true);
            noPhysics = true;
            setNoGravity(true);
        }
    }

    private void recover(Player player) {
        if (recoverable) {
            ItemStack stack = getArrow().copy();
            recoverable = false;
            if (player == null || !player.getInventory().add(stack)) spawnAtLocation(stack, 0.1F);
        }
        if (player != null)
            level().playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_CATCH_EVENT,
                    SoundSource.PLAYERS, 0.6F, 1.1F);
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !isReturning() && entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide || isReturning()) return;
        Entity target = hit.getEntity();
        // The server's arrow hit result may contain only the target's feet position.
        // Recover the trajectory intersection so upper-body and leg shots really differ.
        Vec3 contact = target.getBoundingBox().inflate(0.1).clip(position(), position().add(getDeltaMovement()))
                .orElse(target.getBoundingBox().getCenter());
        if (target.hurt(damageSources().thrown(this, getOwner()),
                SpinCharge.damage(charge, isGolden(), bounces > 0, getSpinTechnique() == 2))
                && target instanceof LivingEntity living) {
            if (getSpinTechnique() == 2) {
                living.addEffect(new MobEffectInstance(net.hydra.jojomod.event.ModEffects.SPIN_WOUND, 70, 0), getOwner());
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), getOwner());
            } else if (getSpinTechnique() == 1) {
                boolean legs = SpinProjectileRules.hitsLegs(contact.y - living.getY(), living.getBbHeight());
                living.addEffect(new MobEffectInstance(legs ? MobEffects.MOVEMENT_SLOWDOWN : MobEffects.WEAKNESS,
                        60, legs ? 2 : 1), getOwner());
            } else {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        isGolden() ? 40 : 20, 0), getOwner());
            }
            if (getOwner() instanceof Player owner &&
                    ((net.hydra.jojomod.access.IFatePlayer) owner).rdbt$getFatePowers()
                            instanceof net.hydra.jojomod.fates.powers.SpinFate spin)
                spin.recordHit(target, isGolden(), bounces > 0 || redirects > 0);
        }
        impact(contact);
        beginReturn();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (level().isClientSide || isReturning()) return;
        var state = level().getBlockState(hit.getBlockPos());
        state.onProjectileHit(level(), state, hit, this);
        if (isRemoved()) return;
        impact(hit.getLocation());
        if (++bounces >= (bounceLimit > 0 ? bounceLimit : isGolden() ? 3 : 2)) {
            beginReturn();
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 velocity = getDeltaMovement();
        setDeltaMovement(velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(0.8));
        setPos(hit.getLocation().add(normal.scale(0.15)));
        hasImpulse = true;
    }

    private void impact(Vec3 pos) {
        level().playSound(null, blockPosition(), ModSounds.STEEL_BALL_IMPACT_EVENT,
                SoundSource.PLAYERS, 0.8F, isGolden() ? 1.3F : 1F);
        if (level() instanceof ServerLevel server)
            server.sendParticles(isGolden() ? GOLDEN_PARTICLE : ParticleTypes.CRIT,
                    pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.08);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SpinCharge", charge);
        tag.putInt("SpinTechnique", getSpinTechnique());
        tag.putBoolean("GoldenSpin", isGolden());
        tag.putBoolean("SpinReturning", isReturning());
        tag.putBoolean("SpinRecoverable", recoverable);
        tag.putInt("SpinAge", age);
        tag.putInt("SpinBounces", bounces);
        tag.putInt("SpinRedirects", redirects);
        tag.putInt("SpinBounceLimit", bounceLimit);
        tag.putInt("SpinReturnAge", returnAge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpinTechnique(tag.getInt("SpinTechnique"));
        charge = SpinCharge.clamp(tag.getFloat("SpinCharge"));
        entityData.set(GOLDEN, tag.getBoolean("GoldenSpin"));
        entityData.set(RETURNING, tag.getBoolean("SpinReturning"));
        recoverable = tag.getBoolean("SpinRecoverable");
        age = Math.max(0, Math.min(SpinProjectileRules.LIFETIME_TICKS, tag.getInt("SpinAge")));
        bounces = Math.max(0, Math.min(4, tag.getInt("SpinBounces")));
        redirects = Math.max(0, Math.min(2, tag.getInt("SpinRedirects")));
        setBounceLimit(tag.contains("SpinBounceLimit") ? tag.getInt("SpinBounceLimit") : isGolden() ? 3 : 2);
        returnAge = tag.contains("SpinReturnAge") ? Math.max(SpinProjectileRules.OUTBOUND_TICKS,
                Math.min(SpinProjectileRules.MAX_OUTBOUND_TICKS, tag.getInt("SpinReturnAge"))) : SpinProjectileRules.OUTBOUND_TICKS;
    }
}
