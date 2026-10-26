package net.hydra.jojomod.fates.powers;

import com.google.common.collect.Lists;
import net.hydra.jojomod.client.ClientNetworking;
import net.hydra.jojomod.client.StandIcons;
import net.hydra.jojomod.event.AbilityIconInstance;
import net.hydra.jojomod.fates.FatePowers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**The fate of someone who has mastered the Spin: the rotation of the body is used to keep balance, and the
 * rotation of steel balls is used to kill. The active abilities of the fate are in SpinGeneralPowers, the fate itself
 * is what unlocks them (see PowerTypes.canHavePower) and what gives the passive benefits below.*/
public class SpinFate extends FatePowers {

    public SpinFate(LivingEntity self) {
        super(self);
    }

    public SpinFate() {
    }

    @Override
    public FatePowers generateFatePowers(LivingEntity entity) {
        return new SpinFate(entity);
    }

    @Override
    public Component getFateName() {
        return Component.translatable("text.roundabout.fate.spin");
    }

    @Override
    public float getDamageReduction(DamageSource source, float amt) {
        if (source.is(DamageTypes.FALL)) {
            return ClientNetworking.getAppropriateConfig().spinSettings.spinFateFallDamageReduction;
        }
        return super.getDamageReduction(source, amt);
    }

    @Override
    public List<AbilityIconInstance> drawGUIIcons(GuiGraphics context, float delta, int mouseX, int mouseY, int leftPos, int topPos, byte level, boolean bypas) {
        List<AbilityIconInstance> icons = Lists.newArrayList();
        icons.add(drawSingleGUIIcon(context, 18, leftPos + 20, topPos + 80, 0, "ability.roundabout.spin_passive",
                "instruction.roundabout.passive", StandIcons.SPIN, 0, level, bypas));
        return icons;
    }
}
