package net.hydra.jojomod.item;

import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.event.ModParticles;
import net.hydra.jojomod.event.powers.StandUser;
import net.hydra.jojomod.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**A polished steel sphere. Hold use to wind up and release to throw it. People with a Spin fate throw it with spin,
 * everyone else just throws a very heavy ball. Balls that don't hit a target can be picked back up.*/
public class SteelBallItem extends Item {

    public static final int THROW_THRESHOLD_TIME = 6;
    public static final float SHOOT_POWER = 1.9F;

    public SteelBallItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    /**Default 72000*/
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (user instanceof Player player) {
            int chargeTime = this.getUseDuration(stack) - timeLeft;
            if (chargeTime >= THROW_THRESHOLD_TIME) {
                if (!level.isClientSide) {
                    SteelBallEntity ball = new SteelBallEntity(level, player, SteelBallEntity.getInnateSpin(player));
                    ball.shootFromRotation(player, player.getXRot(), player.getYRot(), -0.3F, SHOOT_POWER,
                            ball.getSpinLevel() > 0 ? 0.3F : 1.0F);
                    if (player.getAbilities().instabuild) {
                        ball.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }

                    level.addFreshEntity(ball);
                    level.playSound(null, ball, ModSounds.KNIFE_THROW_SOUND_EVENT, SoundSource.PLAYERS, 1.0F, 0.75F);

                    if (((StandUser) player).roundabout$isBubbleEncased()) {
                        StandUser standUser = ((StandUser) player);
                        standUser.roundabout$setBubbleEncased((byte) 0);
                        level.playSound(null, player.blockPosition(), ModSounds.BUBBLE_POP_EVENT,
                                SoundSource.PLAYERS, 2F, (float) (0.98 + (Math.random() * 0.04)));
                        ((ServerLevel) level).sendParticles(ModParticles.BUBBLE_POP,
                                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                                5, 0.25, 0.25, 0.25, 0.025);
                    }

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }

                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
