package net.hydra.jojomod.powers.power_types;

import com.google.common.collect.Lists;
import net.hydra.jojomod.access.IEntityAndData;
import net.hydra.jojomod.access.IProjectileAccess;
import net.hydra.jojomod.client.ClientNetworking;
import net.hydra.jojomod.client.StandIcons;
import net.hydra.jojomod.client.hud.StandHudRender;
import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.event.AbilityIconInstance;
import net.hydra.jojomod.event.index.PowerIndex;
import net.hydra.jojomod.event.index.PowerTypes;
import net.hydra.jojomod.item.ModItems;
import net.hydra.jojomod.powers.GeneralPowers;
import net.hydra.jojomod.sound.ModSounds;
import net.hydra.jojomod.stand.powers.elements.PowerContext;
import net.hydra.jojomod.util.MainUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**The active abilities of the Spin, which is unlocked by the Spin fate. The user wields steel balls: they wind up
 * a throw to put spin on a ball, recall their balls back to their hand, leap forward on a golden rotation, and
 * deflect projectiles with a ring of rotation.
 *
 * Controls:
 * Skill 1: Start winding up a steel ball throw, press again to throw it. The longer the wind up, the more spin.
 *          Crouching when you throw reverses the direction of the spin.
 * Skill 2: Recall every steel ball you have thrown back to you.
 * Skill 3: Rotation Leap, a burst of movement in the direction you are looking.
 * Skill 4: Rotation Guard, briefly deflects projectiles that hit you.*/
public class SpinGeneralPowers extends GeneralPowers {
    public SpinGeneralPowers(LivingEntity self) {
        super(self);
    }

    public SpinGeneralPowers() {
        super(null);
    }

    @Override
    public GeneralPowers generatePowers(LivingEntity entity) {
        return new SpinGeneralPowers(entity);
    }

    public static final byte SPIN_CHARGE = PowerIndex.POWER_1;
    public static final byte SPIN_THROW = PowerIndex.POWER_1_BONUS;
    public static final byte BALL_RECALL = PowerIndex.POWER_2;
    public static final byte ROTATION_LEAP = PowerIndex.POWER_3;
    public static final byte ROTATION_GUARD = PowerIndex.POWER_4;

    /**The ticks of winding up needed to reach spin level 1, 2, and 3 respectively*/
    public static final int[] SPIN_CHARGE_THRESHOLDS = {8, 20, 36};
    /**Where the charge bar tops out, holding longer does not add spin*/
    public static final int MAX_CHARGE = SPIN_CHARGE_THRESHOLDS[SPIN_CHARGE_THRESHOLDS.length - 1];
    /**If you hold a wound up throw for this long without throwing, you lose focus and cancel it*/
    public static final int MAX_HOLD_TICKS = 160;
    public static final double RECALL_RANGE = 64;

    /**The text name of the power*/
    @Override
    public Component getPowerName() {
        return Component.translatable("text.roundabout.powers.spin");
    }

    @Override
    public Component getPowerTagName() {
        return Component.translatable("text.roundabout.powers.spin_select");
    }

    @Override
    public void powerActivate(PowerContext context) {
        switch (context) {
            case SKILL_1_NORMAL, SKILL_1_CROUCH, SKILL_1_GUARD, SKILL_1_CROUCH_GUARD -> {
                clientSpinThrow();
            }
            case SKILL_2_NORMAL, SKILL_2_CROUCH, SKILL_2_GUARD, SKILL_2_CROUCH_GUARD -> {
                clientBallRecall();
            }
            case SKILL_3_NORMAL, SKILL_3_CROUCH, SKILL_3_GUARD, SKILL_3_CROUCH_GUARD -> {
                clientRotationLeap();
            }
            case SKILL_4_NORMAL, SKILL_4_CROUCH, SKILL_4_GUARD, SKILL_4_CROUCH_GUARD -> {
                clientRotationGuard();
            }
        }
    }

    /**Cooldowns are controlled by the server so that the abilities cannot be used faster than intended*/
    @Override
    public boolean isServerControlledCooldown(byte num) {
        if (num == PowerIndex.GENERAL_1 || num == PowerIndex.GENERAL_2
                || num == PowerIndex.GENERAL_3 || num == PowerIndex.GENERAL_4) {
            return true;
        }
        return super.isServerControlledCooldown(num);
    }

    /*
     * STEEL BALL INVENTORY
     */

    public boolean hasSteelBall() {
        if (self instanceof Player pl) {
            if (pl.getAbilities().instabuild) {
                return true;
            }
            if (pl.getOffhandItem().is(ModItems.STEEL_BALL)) {
                return true;
            }
            for (ItemStack stack : pl.getInventory().items) {
                if (stack.is(ModItems.STEEL_BALL)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**Takes a steel ball out of the inventory of the user, preferring the hands. Returns false if they have none.*/
    public boolean consumeSteelBall() {
        if (self instanceof Player pl) {
            if (pl.getAbilities().instabuild) {
                return true;
            }
            if (pl.getMainHandItem().is(ModItems.STEEL_BALL)) {
                pl.getMainHandItem().shrink(1);
                return true;
            }
            if (pl.getOffhandItem().is(ModItems.STEEL_BALL)) {
                pl.getOffhandItem().shrink(1);
                return true;
            }
            for (ItemStack stack : pl.getInventory().items) {
                if (stack.is(ModItems.STEEL_BALL)) {
                    stack.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * SKILL 1: SPIN THROW
     */

    public static int getSpinForCharge(int chargeTicks) {
        int spin = 0;
        for (int i = 0; i < SPIN_CHARGE_THRESHOLDS.length; i++) {
            if (chargeTicks >= SPIN_CHARGE_THRESHOLDS[i]) {
                spin = i + 1;
            }
        }
        return spin;
    }

    /**How far along the wind up of a throw is, for the HUD*/
    public int getSpinCharge() {
        if (getActivePower() == SPIN_CHARGE) {
            return Math.max(0, Math.min(getAttackTimeDuring(), MAX_CHARGE));
        }
        return 0;
    }

    public void clientSpinThrow() {
        if (getActivePower() == SPIN_CHARGE) {
            this.tryPower(SPIN_THROW);
        } else if (getActivePower() == PowerIndex.NONE && !onCooldown(PowerIndex.GENERAL_1) && hasSteelBall()) {
            this.tryPower(SPIN_CHARGE);
        }
    }

    public void doSpinCharge() {
        if (!self.level().isClientSide()) {
            if (!onCooldown(PowerIndex.GENERAL_1) && hasSteelBall()) {
                this.attackTimeDuring = 0;
                setActivePower(SPIN_CHARGE);
                playSoundIfPossible(self.level(), null, self.blockPosition(), ModSounds.IMPALE_CHARGE_EVENT,
                        SoundSource.PLAYERS, 1F, 1.4F + (float) (Math.random() * 0.1F));
            }
        } else {
            setActivePower(SPIN_CHARGE);
            setAttackTimeDuring(0);
            tryPowerPacket(SPIN_CHARGE);
        }
    }

    public void doSpinThrow() {
        if (!self.level().isClientSide()) {
            if (getActivePower() == SPIN_CHARGE) {
                int spin = getSpinForCharge(this.attackTimeDuring);
                if (consumeSteelBall()) {
                    MainUtil.playPop(self);
                    SteelBallEntity ball = new SteelBallEntity(self.level(), self, self.isCrouching() ? -spin : spin);
                    ball.shootFromRotation(self, self.getXRot(), self.getYRot(), 0.0F,
                            2.1F + 0.3F * spin, spin > 0 ? 0.15F : 0.9F);
                    if (self instanceof Player pl && pl.getAbilities().instabuild) {
                        ball.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }
                    PowerTypes.copyPlaneOfExisting(self, ball);
                    self.level().addFreshEntity(ball);

                    playSoundIfPossible(self.level(), null, self.blockPosition(), ModSounds.KNIFE_THROW_SOUND_EVENT,
                            SoundSource.PLAYERS, 1F, 0.7F + 0.1F * spin);
                    setCooldown(PowerIndex.GENERAL_1, ClientNetworking.getAppropriateConfig().spinSettings.spinThrowCooldown);
                }
            }
            setPowerNone();
        } else {
            tryPowerPacket(SPIN_THROW);
            setActivePower(PowerIndex.NONE);
            setAttackTimeDuring(-1);
        }
    }

    /*
     * SKILL 2: BALL RECALL
     */

    public void clientBallRecall() {
        if (!onCooldown(PowerIndex.GENERAL_2)) {
            this.tryPower(BALL_RECALL);
        }
    }

    public void doBallRecall() {
        if (!self.level().isClientSide()) {
            if (!onCooldown(PowerIndex.GENERAL_2)) {
                List<SteelBallEntity> balls = self.level().getEntitiesOfClass(SteelBallEntity.class,
                        self.getBoundingBox().inflate(RECALL_RANGE),
                        ball -> ball.getOwner() == self && !ball.isRecalling());
                if (!balls.isEmpty()) {
                    for (SteelBallEntity ball : balls) {
                        ball.recall();
                    }
                    playSoundIfPossible(self.level(), null, self.blockPosition(), ModSounds.IRON_BALL_BOUNCE_EVENT,
                            SoundSource.PLAYERS, 1.2F, 1.6F);
                    setCooldown(PowerIndex.GENERAL_2, ClientNetworking.getAppropriateConfig().spinSettings.ballRecallCooldown);
                }
            }
        } else {
            tryPowerPacket(BALL_RECALL);
        }
    }

    /*
     * SKILL 3: ROTATION LEAP
     */

    public void clientRotationLeap() {
        if (!onCooldown(PowerIndex.GENERAL_3)) {
            this.tryPower(ROTATION_LEAP);
        }
    }

    public void doRotationLeap() {
        if (!self.level().isClientSide()) {
            if (!onCooldown(PowerIndex.GENERAL_3)) {
                Vec3 look = self.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    flat = Vec3.ZERO;
                } else {
                    flat = flat.normalize();
                }
                float lift = self.onGround() ? 0.5F : 0.25F;
                self.setDeltaMovement(flat.x * 1.15, lift + Math.max(look.y, 0) * 0.35, flat.z * 1.15);
                self.hurtMarked = true;
                self.fallDistance = 0;

                Vec3 pos = self.position();
                sendParticlesIfPossible(self.level(), ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z,
                        10, 0.3, 0.05, 0.3, 0.05);
                playSoundIfPossible(self.level(), null, self.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 1F, 1.3F);
                setCooldown(PowerIndex.GENERAL_3, ClientNetworking.getAppropriateConfig().spinSettings.rotationLeapCooldown);
            }
        } else {
            tryPowerPacket(ROTATION_LEAP);
        }
    }

    /*
     * SKILL 4: ROTATION GUARD
     */

    public void clientRotationGuard() {
        if (!onCooldown(PowerIndex.GENERAL_4) && getActivePower() == PowerIndex.NONE) {
            this.tryPower(ROTATION_GUARD);
        }
    }

    public void doRotationGuard() {
        if (!self.level().isClientSide()) {
            if (!onCooldown(PowerIndex.GENERAL_4)) {
                this.attackTimeDuring = 0;
                setActivePower(ROTATION_GUARD);
                playSoundIfPossible(self.level(), null, self.blockPosition(), ModSounds.IMPALE_CHARGE_EVENT,
                        SoundSource.PLAYERS, 1F, 1.7F + (float) (Math.random() * 0.1F));
                setCooldown(PowerIndex.GENERAL_4, ClientNetworking.getAppropriateConfig().spinSettings.rotationGuardCooldown);
            }
        } else {
            tryPowerPacket(ROTATION_GUARD);
        }
    }

    /**While guarding with a rotation, anything that hits you is thrown back where it came from, aimed where you look*/
    @Override
    public boolean dealWithProjectileNoDiscard(Entity ent, HitResult res) {
        if (getActivePower() == ROTATION_GUARD && ent instanceof Projectile projectile && projectile.getOwner() != self) {
            IProjectileAccess access = (IProjectileAccess) projectile;
            if (!access.roundabout$getIsDeflected()) {
                access.roundabout$setIsDeflected(true);
                double speed = Math.max(projectile.getDeltaMovement().length(), 1.0);
                ((IEntityAndData) projectile).rdbt$forceDeltaMovement(self.getLookAngle().scale(speed));
                projectile.setOwner(self);

                playSoundIfPossible(self.level(), null, self.blockPosition(), ModSounds.HIT_1_SOUND_EVENT,
                        SoundSource.PLAYERS, 1F, 1.05F + (float) (Math.random() * 0.1F));
                self.swing(InteractionHand.MAIN_HAND, true);
                return true;
            }
        }
        return super.dealWithProjectileNoDiscard(ent, res);
    }

    /*
     * MOVE HANDLING AND TICKING
     */

    @Override
    public boolean setPowerOther(int move, int lastMove) {
        if (move == SPIN_CHARGE) {
            doSpinCharge();
        } else if (move == SPIN_THROW) {
            doSpinThrow();
        } else if (move == BALL_RECALL) {
            doBallRecall();
        } else if (move == ROTATION_LEAP) {
            doRotationLeap();
        } else if (move == ROTATION_GUARD) {
            doRotationGuard();
        }
        return super.setPowerOther(move, lastMove);
    }

    @Override
    public void tickPower() {
        if (!self.level().isClientSide()) {
            byte active = getActivePower();
            if (active == SPIN_CHARGE) {
                if (!hasSteelBall() || this.attackTimeDuring > MAX_HOLD_TICKS) {
                    xTryPower(PowerIndex.NONE, true);
                } else {
                    tickSpinCharge();
                }
            } else if (active == ROTATION_GUARD) {
                if (this.attackTimeDuring >= ClientNetworking.getAppropriateConfig().spinSettings.rotationGuardDuration) {
                    xTryPower(PowerIndex.NONE, true);
                } else {
                    tickRotationGuard();
                }
            }
        }
        super.tickPower();
    }

    private void tickSpinCharge() {
        int charge = this.attackTimeDuring;
        for (int i = 0; i < SPIN_CHARGE_THRESHOLDS.length; i++) {
            if (charge == SPIN_CHARGE_THRESHOLDS[i]) {
                //A rising chime to let you know that the spin got stronger
                playSoundIfPossible(self.level(), null, self.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 1F, 0.8F + 0.3F * i);
            }
        }
        if (charge % 3 == 0 && getSpinForCharge(charge) > 0) {
            Vec3 hand = self.getEyePosition().add(self.getLookAngle().scale(0.7)).add(0, -0.35, 0);
            sendParticlesIfPossible(self.level(), ParticleTypes.CRIT, hand.x, hand.y, hand.z,
                    getSpinForCharge(charge), 0.12, 0.12, 0.12, 0.05);
        }
    }

    private void tickRotationGuard() {
        if (this.attackTimeDuring % 2 == 0) {
            Vec3 pos = self.position();
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(this.attackTimeDuring * 30 + i * 120);
                sendParticlesIfPossible(self.level(), ParticleTypes.CRIT,
                        pos.x + Math.cos(angle) * 0.9, pos.y + self.getBbHeight() * 0.5, pos.z + Math.sin(angle) * 0.9,
                        1, 0, 0, 0, 0);
            }
        }
    }

    /**Winding up a throw is a slow, focused action*/
    @Override
    public float inputSpeedModifiers(float basis) {
        if (getActivePower() == SPIN_CHARGE) {
            basis *= 0.6F;
        }
        return super.inputSpeedModifiers(basis);
    }

    /*
     * CLIENT: ICONS, HUD, AND ABILITY LIST
     */

    @Override
    public void renderIcons(GuiGraphics context, int x, int y) {
        setSkillIcon(context, x, y, 1, StandIcons.SPIN_THROW, PowerIndex.GENERAL_1);
        setSkillIcon(context, x, y, 2, StandIcons.BALL_RECALL, PowerIndex.GENERAL_2);
        setSkillIcon(context, x, y, 3, StandIcons.ROTATION_LEAP, PowerIndex.GENERAL_3);
        setSkillIcon(context, x, y, 4, StandIcons.ROTATION_GUARD, PowerIndex.GENERAL_4);
    }

    @Override
    public boolean isAttackIneptVisually(byte activeP, int slot) {
        if (slot == 1 && getActivePower() != SPIN_CHARGE && !hasSteelBall()) {
            return true;
        }
        return super.isAttackIneptVisually(activeP, slot);
    }

    @Override
    public boolean replaceHudActively() {
        return getActivePower() == SPIN_CHARGE;
    }

    /**If the above function is set to true, this will be the code called instead of the exp bar one.*/
    @Override
    public void getReplacementHUD(GuiGraphics context, Player cameraPlayer, int screenWidth, int screenHeight, int x,
                                  boolean removeNum) {
        StandHudRender.renderSpinHud(context, cameraPlayer, screenWidth, screenHeight, x, removeNum);
    }

    @Override
    public List<AbilityIconInstance> drawGUIIcons(GuiGraphics context, float delta, int mouseX, int mouseY, int leftPos, int topPos, byte level, boolean bypas) {
        List<AbilityIconInstance> icons = Lists.newArrayList();
        icons.add(drawSingleGUIIcon(context, 18, leftPos + 20, topPos + 80, 0, "ability.roundabout.spin_throw",
                "instruction.roundabout.press_skill", StandIcons.SPIN_THROW, 1, level, bypas));
        icons.add(drawSingleGUIIcon(context, 18, leftPos + 20, topPos + 99, 0, "ability.roundabout.ball_recall",
                "instruction.roundabout.press_skill", StandIcons.BALL_RECALL, 2, level, bypas));
        icons.add(drawSingleGUIIcon(context, 18, leftPos + 20, topPos + 118, 0, "ability.roundabout.rotation_leap",
                "instruction.roundabout.press_skill", StandIcons.ROTATION_LEAP, 3, level, bypas));
        icons.add(drawSingleGUIIcon(context, 18, leftPos + 39, topPos + 80, 0, "ability.roundabout.rotation_guard",
                "instruction.roundabout.press_skill", StandIcons.ROTATION_GUARD, 4, level, bypas));
        return icons;
    }
}
