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
    private float charge;
    private int age;
    private int bounces;
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
        this.charge = charge;
        entityData.set(GOLDEN, golden);
        recoverable = !owner.getAbilities().instabuild;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GOLDEN, false);
        entityData.define(RETURNING, false);
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
        if (age >= 24) beginReturn();
        Entity owner = getOwner();
        if (age >= 120 || (owner != null && !owner.isAlive())) {
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
        if (target.hurt(damageSources().thrown(this, getOwner()), SpinCharge.damage(charge, isGolden()))
                && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    isGolden() ? 60 : 30, 0), getOwner());
        }
        impact(hit.getLocation());
        beginReturn();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (level().isClientSide || isReturning()) return;
        var state = level().getBlockState(hit.getBlockPos());
        state.onProjectileHit(level(), state, hit, this);
        if (isRemoved()) return;
        impact(hit.getLocation());
        if (++bounces >= (isGolden() ? 3 : 2)) {
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
        tag.putBoolean("GoldenSpin", isGolden());
        tag.putBoolean("SpinReturning", isReturning());
        tag.putBoolean("SpinRecoverable", recoverable);
        tag.putInt("SpinAge", age);
        tag.putInt("SpinBounces", bounces);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        charge = net.minecraft.util.Mth.clamp(tag.getFloat("SpinCharge"), 0, 1);
        entityData.set(GOLDEN, tag.getBoolean("GoldenSpin"));
        entityData.set(RETURNING, tag.getBoolean("SpinReturning"));
        recoverable = tag.getBoolean("SpinRecoverable");
        age = Math.max(0, tag.getInt("SpinAge"));
        bounces = Math.max(0, tag.getInt("SpinBounces"));
    }
}
