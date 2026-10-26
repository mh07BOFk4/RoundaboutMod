package net.hydra.jojomod.item;

import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class SteelBallItem extends Item {
    public SteelBallItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int ticks = getUseDuration(stack) - remaining;
        if (!level.isClientSide && ticks % 5 == 0) {
            float charge = SpinCharge.progress(ticks);
            if (ticks <= SpinCharge.FULL_TICKS || ticks % 20 == 0) {
                level.playSound(null, user.blockPosition(), ModSounds.STEEL_BALL_CHARGE_EVENT,
                        SoundSource.PLAYERS, 0.25F, 0.6F + charge * 0.8F);
            }
            if (ticks == SpinCharge.FULL_TICKS) {
                level.playSound(null, user.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                        SoundSource.PLAYERS, 0.7F, 1.2F);
                if (user instanceof Player player)
                    player.displayClientMessage(Component.translatable("message.roundabout.golden_spin")
                            .withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int ticks = getUseDuration(stack) - remaining;
        if (!(user instanceof Player player) || level.isClientSide || ticks < SpinCharge.MIN_TICKS) return;
        throwBall(player, stack, SpinCharge.progress(ticks), SpinCharge.isGolden(ticks), false);
    }

    /** Shared by item release and Fate skills: the server owns consumption and buffs. */
    public boolean throwBall(Player player, ItemStack stack, float charge, boolean golden, boolean disruptive) {
        return throwBall(player, stack, charge, golden, disruptive, false);
    }

    public boolean throwBall(Player player, ItemStack stack, float charge, boolean golden, boolean disruptive, boolean requestedInfinite) {
        Level level = player.level();
        if (level.isClientSide || stack.isEmpty() || !stack.is(this)
                || player.getCooldowns().isOnCooldown(this)) return false;
        net.hydra.jojomod.fates.powers.SpinFate spin =
                ((net.hydra.jojomod.access.IFatePlayer) player).rdbt$getFatePowers()
                        instanceof net.hydra.jojomod.fates.powers.SpinFate fate ? fate : null;
        boolean infinite = requestedInfinite && spin != null && spin.canUseInfinite();
        boolean counter = spin != null && spin.hasCounter() && !golden;
        golden |= counter || infinite;

        SteelBallEntity ball = new SteelBallEntity(level, player, stack, charge, golden);
        ball.setSpinTechnique(infinite ? 2 : disruptive ? 1 : 0);
        ball.setBounceLimit((golden ? 3 : 2) + (spin != null && spin.rank() >= 3 ? 1 : 0));
        ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.4F + charge, 0.25F);
        if (!level.addFreshEntity(ball)) return false;
        if (counter) spin.consumeCounter();
        if (infinite) level.playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_GOLDEN_EVENT,
                SoundSource.PLAYERS, 0.8F, 0.65F);
        player.swing(stack == player.getMainHandItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 8);
        player.awardStat(Stats.ITEM_USED.get(this));
        level.playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_THROW_EVENT,
                SoundSource.PLAYERS, 0.8F, golden ? 1.2F : 0.9F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.roundabout.steel_ball.charge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.roundabout.steel_ball.golden").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.roundabout.steel_ball.return").withStyle(ChatFormatting.DARK_AQUA));
    }
}
