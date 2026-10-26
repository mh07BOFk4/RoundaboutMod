package net.hydra.jojomod.item;

import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        if (level instanceof ServerLevel server && ticks % 5 == 0) {
            float charge = SpinCharge.progress(ticks);
            Vec3 center = user.getEyePosition().add(user.getLookAngle().scale(0.65));
            for (int i = 0; i < 6; i++) {
                double angle = ticks * 0.5 + i * Math.PI / 3;
                server.sendParticles(SpinCharge.isGolden(ticks) ? SteelBallEntity.GOLDEN_PARTICLE : ParticleTypes.END_ROD,
                        center.x + Math.cos(angle) * 0.18, center.y + Math.sin(angle) * 0.18,
                        center.z, 1, 0, 0, 0, 0);
            }
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
        if (player.getCooldowns().isOnCooldown(this)) return;
        SteelBallEntity ball = new SteelBallEntity(level, player, stack,
                SpinCharge.progress(ticks), SpinCharge.isGolden(ticks));
        ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                1.4F + SpinCharge.progress(ticks), 0.25F);
        // Consume only after a successful spawn; creative throws never create recoverable items.
        if (level.addFreshEntity(ball)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 12);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(null, player.blockPosition(), ModSounds.STEEL_BALL_THROW_EVENT,
                    SoundSource.PLAYERS, 0.8F, SpinCharge.isGolden(ticks) ? 1.2F : 0.9F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.roundabout.steel_ball.charge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.roundabout.steel_ball.golden").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.roundabout.steel_ball.return").withStyle(ChatFormatting.DARK_AQUA));
    }
}
