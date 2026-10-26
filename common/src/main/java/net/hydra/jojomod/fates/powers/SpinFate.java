package net.hydra.jojomod.fates.powers;

import net.hydra.jojomod.access.ISpinPlayer;
import net.hydra.jojomod.client.StandIcons;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SpinFate extends FatePowers {
    private static final byte FIRST_MOVE = 91;
    // Resolve client icons only while drawing, never during Fate initialization on a server.
    private static ResourceLocation icon(int slot) {
        return switch (slot) {
            case 1 -> StandIcons.TUSK_SPINTHROW;
            case 2 -> StandIcons.SILVER_CHARIOT_RAPIER_SPIN;
            case 3 -> StandIcons.TUSK_NAILLEAP;
            default -> StandIcons.GREEN_DAY_MOLD_SPIN_RIGHT;
        };
    }
    private static final String[] NAMES = {"shot", "disrupt", "step", "rotation"};

    public SpinFate() { super(); }
    public SpinFate(LivingEntity self) { super(self); }
    @Override public FatePowers generateFatePowers(LivingEntity entity) { return new SpinFate(entity); }
    @Override public Component getFateName() { return Component.translatable("text.roundabout.fate.spin"); }
    private ISpinPlayer data() { return (ISpinPlayer) self; }
    public int rank() { return SpinProgression.rank(data().rdbt$getSpinExperience()); }
    public boolean isEmpowered() { return data().rdbt$getSpinEmpoweredTicks() > 0; }
    public boolean hasInfiniteRotation() { return isEmpowered() && data().rdbt$isInfiniteSpin(); }
    private static byte cooldownSlot(int slot) { return (byte) (PowerIndex.FATE_1 + slot - 1); }

    @Override public boolean isServerControlledCooldown(byte slot) {
        return slot >= PowerIndex.FATE_1 && slot <= PowerIndex.FATE_4;
    }

    @Override public void powerActivate(PowerContext context) {
        // Keep the existing input latch and remappable Skill 1–4 bindings.
        int slot = context.ordinal() / 4 + 1;
        tryPowerPacket((byte) (FIRST_MOVE + slot - 1));
    }

    private boolean canAct(Player player) {
        return FateTypes.getFateFromByte(((net.hydra.jojomod.access.IPlayerEntity) player).roundabout$getFate()) == FateTypes.SPIN
                && player.isAlive() && !player.isSpectator() && !isDazed(player)
                && !player.isUsingItem() && !hasActive(player)
                && !getStandUserSelf().roundabout$isPossessed()
                && !((TimeStop) player.level()).CanTimeStopEntity(player)
                && !PowerTypes.isErasingTime(player) && DiscItemData.canUseAbilities(player);
    }

    @Override public boolean tryPower(int move, boolean forced) {
        if (move == PowerIndex.NONE) return super.tryPower(move, forced);
        if (!(self instanceof ServerPlayer player) || !canAct(player)) return false;
        int slot = move - FIRST_MOVE + 1;
        if (slot < 1 || slot > 4 || onCooldown(cooldownSlot(slot))) return false;
        if (rank() < slot) {
            message(player, "message.roundabout.spin_locked", slot, SpinProgression.requiredExperience(slot));
            return false;
        }
        if (!SpinProgression.canUse(slot, data().rdbt$getSpinExperience(), data().rdbt$getSpinRotation())) {
            message(player, "message.roundabout.spin_cost", SpinProgression.cost(slot));
            return false;
        }
        if (slot <= 2) {
            ItemStack stack = player.getMainHandItem().getItem() instanceof SteelBallItem
                    ? player.getMainHandItem() : player.getOffhandItem();
            if (!(stack.getItem() instanceof SteelBallItem item)) {
                message(player, "message.roundabout.spin_need_ball");
                return false;
            }
            if (!item.throwBall(player, stack, slot == 1 ? 0.8F : 1F, false, slot == 2)) return false;
        } else if (slot == 3) {
            if (!player.onGround() || player.isPassenger()) return false;
            Vec3 direction = player.getLookAngle().multiply(1, 0, 1).normalize();
            if (direction.lengthSqr() < 0.01) return false;
            player.setDeltaMovement(direction.scale(0.85).add(0, 0.2, 0));
            player.hurtMarked = true;
            player.level().playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_THROW_EVENT,
                    SoundSource.PLAYERS, 0.6F, 1.5F);
        } else {
            if (isEmpowered()) return false;
            boolean infinite = isMovingHorse() && data().rdbt$getSpinHorseCharge() >= SpinProgression.HORSE_TICKS;
            data().rdbt$setInfiniteSpin(infinite);
            data().rdbt$setSpinEmpoweredTicks(infinite ? 200 : 120);
            data().rdbt$setSpinHorseCharge(0);
            message(player, infinite ? "message.roundabout.infinite_rotation" : "message.roundabout.golden_rotation");
            player.level().playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                    SoundSource.PLAYERS, 0.8F, infinite ? 0.7F : 1F);
        }
        data().rdbt$setSpinRotation(data().rdbt$getSpinRotation() - SpinProgression.cost(slot));
        setCooldown(cooldownSlot(slot), SpinProgression.cooldown(slot));
        return true;
    }

    private boolean isMovingHorse() {
        return self.getVehicle() instanceof AbstractHorse horse && horse.isTamed() && horse.isSaddled()
                && horse.getControllingPassenger() == self && horse.hurtTime == 0 && horse.onGround()
                && horse.getDeltaMovement().horizontalDistanceSqr() >= 0.0025;
    }

    @Override public void tickPower() {
        super.tickPower();
        if (self.level().isClientSide || !self.isAlive()) return;
        if (data().rdbt$getSpinEmpoweredTicks() > 0) {
            data().rdbt$setSpinEmpoweredTicks(data().rdbt$getSpinEmpoweredTicks() - 1);
            if (data().rdbt$getSpinEmpoweredTicks() == 0) data().rdbt$setInfiniteSpin(false);
        }
        int previous = data().rdbt$getSpinHorseCharge();
        int next = SpinProgression.horseCharge(previous, isMovingHorse(), rank());
        data().rdbt$setSpinHorseCharge(next);
        if (previous < 200 && next == 200 && self instanceof Player player) {
            message(player, "message.roundabout.spin_horse_ready");
            self.level().playSound(null, self.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                    SoundSource.PLAYERS, 0.6F, 0.8F);
        }
        if (self.tickCount % 20 == 0 && !isEmpowered()
                && (self.getMainHandItem().getItem() instanceof SteelBallItem
                || self.getOffhandItem().getItem() instanceof SteelBallItem)) {
            data().rdbt$setSpinRotation(data().rdbt$getSpinRotation() + 1);
        }
    }

    /** Only a successful, server-confirmed projectile hit awards practice. */
    public void recordHit(Entity target, boolean golden) {
        if (self.level().isClientSide || !(self instanceof Player player)
                || !(target instanceof Mob || target instanceof Player) || target == self) return;
        int previous = rank();
        data().rdbt$setSpinExperience(SpinProgression.experience(data().rdbt$getSpinExperience() + (golden ? 6 : 4)));
        data().rdbt$setSpinRotation(data().rdbt$getSpinRotation() + 12);
        if (rank() > previous) {
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
            setSkillIcon(context, x, y, slot, icon(slot), cooldownSlot(slot), rank() < slot);
        context.drawString(Minecraft.getInstance().font,
                Component.translatable("hud.roundabout.spin", rank(), data().rdbt$getSpinRotation()),
                x + 22, y - 13, 0xE9C45A, true);
        if (isEmpowered() || data().rdbt$getSpinHorseCharge() > 0)
            context.drawString(Minecraft.getInstance().font, isEmpowered()
                    ? Component.translatable(hasInfiniteRotation() ? "hud.roundabout.spin_infinite" : "hud.roundabout.spin_golden",
                            (data().rdbt$getSpinEmpoweredTicks() + 19) / 20)
                    : Component.translatable("hud.roundabout.spin_horse", data().rdbt$getSpinHorseCharge() / 2),
                    x + 22, y - 24, 0xE9C45A, true);
    }

    @Override public List<AbilityIconInstance> drawGUIIcons(GuiGraphics context, float delta, int mouseX,
            int mouseY, int left, int top, byte ignoredStandLevel, boolean ignoredCreativeBypass) {
        List<AbilityIconInstance> result = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++) {
            int x = left + 20 + (slot - 1) * 22;
            result.add(drawSingleGUIIcon(context, 18, x, top + 88, slot,
                    "ability.roundabout.spin_" + NAMES[slot - 1], "instruction.roundabout.press_skill",
                    icon(slot), slot, (byte) rank(), false));
        }
        return result;
    }

    @Override public void drawOtherGUIElements(Font font, GuiGraphics context, float delta, int mouseX,
            int mouseY, int left, int top, ResourceLocation rl) {
        context.drawString(font, Component.translatable("menu.roundabout.spin_training",
                data().rdbt$getSpinExperience(), SpinProgression.MAX_EXPERIENCE), left + 18, top + 54, 0xE9C45A, false);
        context.drawString(font, Component.translatable("menu.roundabout.spin_practice"), left + 18, top + 66, 0xFFFFFF, false);
        context.fill(left + 29, top + 95, left + 94, top + 97, 0xFFB9A34F);
        for (int slot = 1; slot <= 4; slot++)
            context.drawString(font, Component.literal(Integer.toString(SpinProgression.requiredExperience(slot))),
                    left + 20 + (slot - 1) * 22, top + 111, 0xAAAAAA, false);
    }
}
