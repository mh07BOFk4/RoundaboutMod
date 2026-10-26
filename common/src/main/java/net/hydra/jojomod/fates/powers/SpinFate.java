package net.hydra.jojomod.fates.powers;

import net.hydra.jojomod.access.ISpinPlayer;
import net.hydra.jojomod.client.StandIcons;
import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.event.AbilityIconInstance;
import net.hydra.jojomod.event.index.*;
import net.hydra.jojomod.event.powers.TimeStop;
import net.hydra.jojomod.event.powers.whitesnake.disc.DiscItemData;
import net.hydra.jojomod.fates.FatePowers;
import net.hydra.jojomod.item.SteelBallItem;
import net.hydra.jojomod.sound.ModSounds;
import net.hydra.jojomod.stand.powers.elements.PowerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpinFate extends FatePowers {
    private static final byte FIRST_MOVE = 91;
    // Resolve client Materials only while drawing, never during server initialization.
    private static ResourceLocation icon(int slot) {
        return switch (slot) {
            case 1 -> StandIcons.TUSK_SPINTHROW;
            case 2 -> StandIcons.SILVER_CHARIOT_RAPIER_SPIN;
            case 3 -> StandIcons.VAMPIRE_STRENGTH;
            default -> StandIcons.GREEN_DAY_MOLD_SPIN_RIGHT;
        };
    }
    private static final String[] NAMES = {"shot", "control", "body", "rotation"};
    public SpinFate() { super(); }
    public SpinFate(LivingEntity self) { super(self); }
    @Override public FatePowers generateFatePowers(LivingEntity entity) { return new SpinFate(entity); }
    @Override public Component getFateName() { return Component.translatable("text.roundabout.fate.spin"); }
    private ISpinPlayer data() { return (ISpinPlayer) self; }
    public int rank() { return SpinProgression.rank(data().rdbt$getSpinExperience()); }
    public boolean hasCounter() { return data().rdbt$getSpinCounterTicks() > 0; }
    public void consumeCounter() { data().rdbt$setSpinCounterTicks(0); }
    public boolean canUseInfinite() { return SpinProgression.infiniteAvailable(rank(), isMovingHorse()); }
    private static byte cooldownSlot(int slot, boolean crouching) {
        if (crouching && slot == 2) return PowerIndex.FATE_2_SNEAK;
        if (crouching && slot == 4) return PowerIndex.FATE_4_SNEAK;
        return (byte) (PowerIndex.FATE_1 + slot - 1);
    }

    @Override public boolean isServerControlledCooldown(byte slot) {
        return (slot >= PowerIndex.FATE_1 && slot <= PowerIndex.FATE_4) || slot == PowerIndex.FATE_2_SNEAK || slot == PowerIndex.FATE_4_SNEAK;
    }
    @Override public void powerActivate(PowerContext context) {
        tryPowerPacket((byte) (FIRST_MOVE + context.ordinal() / 4));
    }
    private boolean canAct(Player player) {
        return FateTypes.getFateFromByte(((net.hydra.jojomod.access.IPlayerEntity) player).roundabout$getFate()) == FateTypes.SPIN
                && player.isAlive() && !player.isSpectator() && !isDazed(player) && !hasActive(player)
                && !getStandUserSelf().roundabout$isPossessed()
                && !((TimeStop) player.level()).CanTimeStopEntity(player)
                && !PowerTypes.isErasingTime(player) && DiscItemData.canUseAbilities(player);
    }
    @Override public boolean tryPower(int move, boolean forced) {
        if (move == PowerIndex.NONE) return super.tryPower(move, forced);
        if (!(self instanceof ServerPlayer player) || !canAct(player)) return false;
        int slot = move - FIRST_MOVE + 1;
        if (!SpinProgression.canUse(slot)) return false;
        boolean crouching = player.isShiftKeyDown();
        byte cd = cooldownSlot(slot, crouching);
        if (onCooldown(cd)) return false;
        if ((slot == 1 || (slot == 4 && !crouching)) && player.isUsingItem()) return false;
        int cooldown = SpinProgression.cooldown(slot);
        if (slot == 4 && crouching) {
            if (!player.hasEffect(net.hydra.jojomod.event.ModEffects.SPIN_WOUND)) return false;
            player.removeEffect(net.hydra.jojomod.event.ModEffects.SPIN_WOUND);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            cooldown = 80;
            message(player, "message.roundabout.spin_reverse");
            player.level().playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                    SoundSource.PLAYERS, 0.6F, 0.5F);
        } else if (slot == 1 || slot == 4) {
            ItemStack stack = player.getMainHandItem().getItem() instanceof SteelBallItem
                    ? player.getMainHandItem() : player.getOffhandItem();
            if (!(stack.getItem() instanceof SteelBallItem item)) {
                message(player, "message.roundabout.spin_need_ball");
                return false;
            }
            boolean infinite = slot == 4 && canUseInfinite();
            if (!item.throwBall(player, stack, 1F, slot == 4, slot == 1 && crouching, infinite)) return false;
            if (infinite) {
                cooldown = 160;
                message(player, "message.roundabout.spin_rectangle_shot");
            }
        } else if (slot == 2) {
            List<SteelBallEntity> balls = player.level().getEntitiesOfClass(SteelBallEntity.class,
                    player.getBoundingBox().inflate(64), ball -> ball.getOwner() == player && !ball.isReturning());
            if (crouching) {
                if (balls.isEmpty()) return false;
                for (SteelBallEntity ball : balls) ball.recall(player);
            } else {
                Vec3 target = aimPoint(player);
                balls.sort(Comparator.comparingDouble(ball -> ball.distanceToSqr(target)));
                boolean redirected = false;
                for (SteelBallEntity ball : balls) {
                    if (ball.redirect(player, target, SpinProgression.redirects(rank()))) {
                        redirected = true;
                        break;
                    }
                }
                if (!redirected) {
                    message(player, "message.roundabout.spin_no_ball");
                    return false;
                }
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                    SoundSource.PLAYERS, 0.6F, crouching ? 1.1F : 1.6F);
        } else {
            if (crouching) {
                if (player.getHealth() >= player.getMaxHealth()
                        && !player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !player.hasEffect(MobEffects.WEAKNESS)
                        && !player.hasEffect(net.hydra.jojomod.event.ModEffects.BLEED)) return false;
                player.heal(rank() >= 3 ? 4F : 2F);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(net.hydra.jojomod.event.ModEffects.BLEED);
                cooldown = 160;
                message(player, "message.roundabout.spin_first_aid");
            } else {
                data().rdbt$setSpinGuardTicks(SpinProgression.guardDuration(rank()));
                message(player, "message.roundabout.spin_harden");
            }
            player.level().playSound(null, player.blockPosition(), ModSounds.IRON_BALL_BOUNCE_EVENT,
                    SoundSource.PLAYERS, 0.4F, 0.7F);
        }
        setCooldown(cd, cooldown);
        return true;
    }
    private Vec3 aimPoint(Player player) {
        Vec3 start = player.getEyePosition(), look = player.getLookAngle().scale(40);
        HitResult block = player.level().clip(new ClipContext(start, start.add(look),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(look).inflate(1),
                target -> target.isPickable() && target != player.getVehicle() && !target.isSpectator(),
                start.distanceToSqr(end));
        return entity == null ? end : entity.getLocation();
    }
    private boolean isMovingHorse() {
        return self.getVehicle() instanceof AbstractHorse horse && horse.isTamed() && horse.isSaddled()
                && horse.getControllingPassenger() == self && horse.hurtTime == 0 && horse.onGround()
                && horse.getDeltaMovement().horizontalDistanceSqr() >= 0.0025;
    }
    @Override public float getDamageReduction(DamageSource source, float amount) {
        boolean physical = amount > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)
                && (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.PLAYER_ATTACK));
        int remaining = data().rdbt$getSpinGuardTicks();
        float reduction = SpinProgression.guardReduction(remaining, rank(), physical);
        if (reduction > 0 && SpinProgression.perfectGuard(remaining, rank())
                && !self.level().isClientSide && !hasCounter()) {
            data().rdbt$setSpinCounterTicks(40);
            gainPractice(2);
            self.level().playSound(null, self.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                    SoundSource.PLAYERS, 0.6F, 1.5F);
            if (self instanceof Player player) message(player, "message.roundabout.spin_counter");
        }
        return reduction;
    }
    @Override public void tickPower() {
        super.tickPower();
        if (self.level().isClientSide || !self.isAlive()) return;
        if (data().rdbt$getSpinGuardTicks() > 0) data().rdbt$setSpinGuardTicks(data().rdbt$getSpinGuardTicks() - 1);
        if (data().rdbt$getSpinCounterTicks() > 0) data().rdbt$setSpinCounterTicks(data().rdbt$getSpinCounterTicks() - 1);
    }
    public void recordHit(Entity target, boolean golden, boolean trickShot) {
        if (self.level().isClientSide || !(target instanceof Mob || target instanceof Player) || target == self) return;
        gainPractice(SpinProgression.practice(golden, trickShot));
    }
    private void gainPractice(int amount) {
        int previous = rank();
        data().rdbt$setSpinExperience(SpinProgression.experience(data().rdbt$getSpinExperience() + amount));
        if (rank() > previous && self instanceof Player player) {
            message(player, "message.roundabout.spin_rank_up", rank());
            self.level().playSound(null, self.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }
    private static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), true);
    }
    @Override public void renderIcons(GuiGraphics context, int x, int y) {
        for (int slot = 1; slot <= 4; slot++)
            setSkillIcon(context, x, y, slot, icon(slot),
                    cooldownSlot(slot, self.isShiftKeyDown()));
        Component status = hasCounter() ? Component.translatable("hud.roundabout.spin_counter")
                : data().rdbt$getSpinGuardTicks() > 0 ? Component.translatable("hud.roundabout.spin_guard")
                : canUseInfinite() ? Component.translatable("hud.roundabout.spin_rectangle")
                : Component.translatable("hud.roundabout.spin", rank());
        context.drawString(Minecraft.getInstance().font, status, x + 22, y - 13, 0xE9C45A, true);
    }
    @Override public List<AbilityIconInstance> drawGUIIcons(GuiGraphics context, float delta, int mouseX,
            int mouseY, int left, int top, byte ignoredStandLevel, boolean ignoredCreativeBypass) {
        List<AbilityIconInstance> result = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++)
            result.add(drawSingleGUIIcon(context, 18, left + 20 + (slot - 1) * 22, top + 88, 1,
                    "ability.roundabout.spin_" + NAMES[slot - 1], "instruction.roundabout.press_skill",
                    icon(slot), slot, (byte) rank(), false));
        return result;
    }
    @Override public void drawOtherGUIElements(Font font, GuiGraphics context, float delta, int mouseX,
            int mouseY, int left, int top, ResourceLocation rl) {
        context.drawString(font, Component.translatable("menu.roundabout.spin_training",
                data().rdbt$getSpinExperience(), SpinProgression.MAX_EXPERIENCE), left + 18, top + 54, 0xE9C45A, false);
        context.drawString(font, Component.translatable("menu.roundabout.spin_practice"), left + 18, top + 66, 0xFFFFFF, false);
        context.drawString(font, Component.translatable("menu.roundabout.spin_upgrades"),
                left + 18, top + 115, 0xA0A0A0, false);
    }
}
