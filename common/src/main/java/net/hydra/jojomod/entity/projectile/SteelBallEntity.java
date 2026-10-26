package net.hydra.jojomod.entity.projectile;

import net.hydra.jojomod.client.ClientNetworking;
import net.hydra.jojomod.entity.ModEntities;
import net.hydra.jojomod.entity.objects.GentlyWeepsEntity;
import net.hydra.jojomod.event.index.FateTypes;
import net.hydra.jojomod.event.powers.ModDamageTypes;
import net.hydra.jojomod.event.powers.StandPowers;
import net.hydra.jojomod.event.powers.StandUser;
import net.hydra.jojomod.item.ModItems;
import net.hydra.jojomod.sound.ModSounds;
import net.hydra.jojomod.util.MainUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**A steel ball, the weapon of Spin users. Thrown with spin it curves through the air, ricochets off of blocks, and
 * cuts whatever it hits. Unlike arrows and knives it is not consumed by a hit: it bounces off of its target,
 * can be picked back up, and can be recalled to its owner with the Spin power.*/
public class SteelBallEntity extends AbstractArrow {
    /**Signed spin. Positive spin curves clockwise (to the right), negative spin curves counter-clockwise.
     * The magnitude is the spin level, from 0 (no spin) to MAX_SPIN.*/
    private static final EntityDataAccessor<Byte> SPIN = SynchedEntityData.defineId(SteelBallEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> RECALLING = SynchedEntityData.defineId(SteelBallEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int MAX_SPIN = 3;
    public static final float BASE_DAMAGE = 5.0F;
    public static final float DAMAGE_PER_SPIN_LEVEL = 1.5F;
    /**Degrees per tick that each level of spin turns the ball by, horizontally*/
    public static final float CURVE_PER_SPIN_LEVEL = 0.55F;
    /**Upward acceleration per spin level, cancels out part of gravity so spinning balls fly flatter*/
    public static final double LIFT_PER_SPIN_LEVEL = 0.012;
    /**How many times a spinning ball can ricochet off blocks, per spin level, plus one*/
    public static final int BOUNCES_PER_SPIN_LEVEL = 1;
    /**A ball that has been recalled but cannot be picked up gives up after this many ticks of hovering at its owner*/
    private static final int RECALL_GIVE_UP_TICKS = 30;

    private int bounceCount = 0;
    private int lastHitEntity = -1;
    private int lastHitTick = -100;
    private int recallHoverTicks = 0;

    public SteelBallEntity(EntityType<? extends SteelBallEntity> entityType, Level level) {
        super(entityType, level);
        this.setSoundEvent(ModSounds.IRON_BALL_BOUNCE_EVENT);
    }

    public SteelBallEntity(Level level, LivingEntity owner, int signedSpin) {
        super(ModEntities.STEEL_BALL, owner, level);
        this.setSoundEvent(ModSounds.IRON_BALL_BOUNCE_EVENT);
        this.setSpin(signedSpin);
    }

    /**The spin that a ball has when it is thrown by hand rather than with the Spin power.
     * Only people with a Spin fate can put natural spin on a ball. Crouching reverses the direction.*/
    public static int getInnateSpin(LivingEntity thrower) {
        if (FateTypes.isSpin(thrower)) {
            return thrower.isCrouching() ? -1 : 1;
        }
        return 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPIN, (byte) 0);
        this.entityData.define(RECALLING, false);
    }

    public int getSpin() {
        return this.entityData.get(SPIN);
    }

    public int getSpinLevel() {
        return Math.abs(getSpin());
    }

    public void setSpin(int signedSpin) {
        this.entityData.set(SPIN, (byte) Mth.clamp(signedSpin, -MAX_SPIN, MAX_SPIN));
    }

    public boolean isRecalling() {
        return this.entityData.get(RECALLING);
    }

    /**For the renderer, a ball lying on the ground does not spin*/
    public boolean isStuckInGround() {
        return this.inGround;
    }

    /**Sends the ball flying back to its owner. It phases through everything on the way and is picked up on contact.*/
    public void recall() {
        if (!this.level().isClientSide()) {
            this.recallHoverTicks = 0;
            this.entityData.set(RECALLING, true);
        }
    }

    private void stopRecall() {
        if (!this.level().isClientSide()) {
            this.entityData.set(RECALLING, false);
            this.setNoPhysics(false);
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.STEEL_BALL);
    }

    @Override
    public void tick() {
        if (this.isRecalling()) {
            tickRecall();
        } else if (!this.inGround && getSpinLevel() > 0) {
            applySpinCurve();
        }
        super.tick();

        if (this.level().isClientSide() && !this.inGround && getSpinLevel() > 0 && this.tickCount % 2 == 0) {
            Vec3 pos = this.position();
            this.level().addParticle(getSpinLevel() >= MAX_SPIN ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    pos.x, pos.y + 0.1, pos.z, 0, 0, 0);
        }
    }

    /**A spinning ball bends its path and stays up in the air longer than a normal throw*/
    private void applySpinCurve() {
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.horizontalDistanceSqr() < 0.0025) {
            return;
        }
        double angle = Math.toRadians(CURVE_PER_SPIN_LEVEL * getSpin());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        this.setDeltaMovement(
                velocity.x * cos - velocity.z * sin,
                velocity.y + LIFT_PER_SPIN_LEVEL * getSpinLevel(),
                velocity.x * sin + velocity.z * cos
        );
    }

    private void tickRecall() {
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            stopRecall();
            return;
        }

        this.inGround = false;
        if (!this.level().isClientSide()) {
            //This flag is synced to the client by the arrow's own data
            this.setNoPhysics(true);
        }

        Vec3 toOwner = owner.getBoundingBox().getCenter().subtract(this.position());
        double distance = toOwner.length();
        if (distance < 1.5) {
            //Picking up happens in playerTouch, if for some reason it cannot, do not hover forever
            if (!this.level().isClientSide() && ++this.recallHoverTicks > RECALL_GIVE_UP_TICKS) {
                this.discard();
                return;
            }
        } else {
            this.recallHoverTicks = 0;
        }
        double speed = Math.min(2.0, 0.5 + distance * 0.08);
        this.setDeltaMovement(distance > 1.0E-4 ? toOwner.scale(speed / distance) : Vec3.ZERO);
        this.hasImpulse = true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        //After bouncing off of something, don't immediately hit it again on the way down
        if (entity.getId() == this.lastHitEntity && this.tickCount - this.lastHitTick < 12) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide()) {
            //The server decides what the ball hits, the client waits for its updated movement
            return;
        }

        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        if (target instanceof LivingEntity living) {
            StandPowers targetPowers = ((StandUser) living).roundabout$getStandPowers();
            if (targetPowers.dealWithProjectile(this, result)) {
                this.discard();
                return;
            } else if (targetPowers.dealWithProjectileNoDiscard(this, result)) {
                return;
            }
        } else if (target instanceof GentlyWeepsEntity gentlyWeeps) {
            GentlyWeepsEntity.dealWithProjectile(this, gentlyWeeps);
            return;
        }

        float damage = (BASE_DAMAGE + DAMAGE_PER_SPIN_LEVEL * getSpinLevel())
                * (float) (ClientNetworking.getAppropriateConfig().spinSettings.steelBallDamageMultiplier * 0.01);
        if (target instanceof LivingEntity living) {
            int projectileProtection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, living);
            damage *= (1 - (projectileProtection * 0.03F));
        }

        DamageSource source = ModDamageTypes.of(this.level(), ModDamageTypes.STEEL_BALL, this, owner != null ? owner : this);
        Vec3 away = this.getDeltaMovement().normalize();

        if (target.hurt(source, damage)) {
            if (owner instanceof LivingEntity livingOwner) {
                livingOwner.setLastHurtMob(target);
            }

            if (target instanceof LivingEntity living) {
                if (owner instanceof LivingEntity livingOwner) {
                    EnchantmentHelper.doPostHurtEffects(living, livingOwner);
                    EnchantmentHelper.doPostDamageEffects(livingOwner, living);
                }
                this.doPostHurtEffects(living);
            }

            //Strongly spinning balls grind into flesh
            if (getSpinLevel() >= 2 && MainUtil.getMobBleed(target)) {
                MainUtil.makeBleed(target, 0, 100 * getSpinLevel(), owner);
            }

            //takeKnockback expects the direction pointing from the target back towards the source
            MainUtil.takeKnockback(target, 0.5 + 0.3 * getSpinLevel(), -away.x, -0.1F, -away.z);
            this.level().playSound(null, this.blockPosition(), ModSounds.IRON_BALL_IMPACT_EVENT, SoundSource.PLAYERS,
                    1.5F, 1.1F + this.random.nextFloat() * 0.2F);
        }

        //Steel balls are not used up by hitting things, they bounce off and can be picked up again
        this.lastHitEntity = target.getId();
        this.lastHitTick = this.tickCount;
        Vec3 rebound = this.getDeltaMovement().scale(-0.12);
        this.setDeltaMovement(rebound.x, Math.max(rebound.y, 0.12), rebound.z);
        this.hasImpulse = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        int maxBounces = getSpinLevel() * BOUNCES_PER_SPIN_LEVEL + (getSpinLevel() > 0 ? 1 : 0);
        Vec3 velocity = this.getDeltaMovement();
        if (this.bounceCount >= maxBounces || velocity.lengthSqr() < 0.09) {
            super.onHitBlock(hitResult);
            return;
        }

        BlockState state = this.level().getBlockState(hitResult.getBlockPos());
        state.onProjectileHit(this.level(), state, hitResult, this);

        //Ricochet: reflect the movement off of the face that was hit, keeping most of the speed
        Direction face = hitResult.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 reflected = velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(0.78);
        this.setDeltaMovement(reflected);

        Vec3 hit = hitResult.getLocation();
        this.setPos(hit.x + normal.x * 0.12, hit.y + normal.y * 0.12, hit.z + normal.z * 0.12);

        this.inGround = false;
        this.shakeTime = 0;
        this.hasImpulse = true;
        this.bounceCount++;

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.blockPosition(), ModSounds.IRON_BALL_BOUNCE_EVENT, SoundSource.PLAYERS,
                    1.4F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("steelBallSpin", (byte) getSpin());
        tag.putBoolean("steelBallRecalling", isRecalling());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpin(tag.getByte("steelBallSpin"));
        this.entityData.set(RECALLING, tag.getBoolean("steelBallRecalling"));
    }
}
