package net.hydra.jojomod.item;

import net.hydra.jojomod.access.IPlayerEntity;
import net.hydra.jojomod.event.index.FateTypes;
import net.hydra.jojomod.fates.powers.SpinProgression;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class SpinManualItem extends Item {
    public SpinManualItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            IPlayerEntity data = (IPlayerEntity) player;
            FateTypes fate = FateTypes.getFateFromByte(data.roundabout$getFate());
            if (fate == FateTypes.HUMAN) {
                data.roundabout$setFate((byte) FateTypes.SPIN.ordinal());
                player.displayClientMessage(Component.translatable("message.roundabout.spin_learned")
                        .withStyle(ChatFormatting.GOLD), false);
            } else if (fate == FateTypes.SPIN && player.isShiftKeyDown()) {
                data.roundabout$setFate((byte) FateTypes.HUMAN.ordinal());
                player.displayClientMessage(Component.translatable("message.roundabout.spin_left"), false);
            } else if (fate == FateTypes.SPIN) {
                player.displayClientMessage(Component.translatable("message.roundabout.spin_status",
                        SpinProgression.rank(data.rdbt$getSpinExperience()), data.rdbt$getSpinExperience(),
                        data.rdbt$getSpinRotation()), false);
            } else {
                player.displayClientMessage(Component.translatable("message.roundabout.spin_other_fate")
                        .withStyle(ChatFormatting.RED), false);
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.roundabout.spin_manual.learn").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.roundabout.spin_manual.leave").withStyle(ChatFormatting.GRAY));
    }
}
