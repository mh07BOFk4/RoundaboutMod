package net.hydra.jojomod.client.models.projectile.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.hydra.jojomod.Roundabout;
import net.hydra.jojomod.client.models.layers.ModEntityRendererClient;
import net.hydra.jojomod.client.models.projectile.StandFireballModel;
import net.hydra.jojomod.entity.projectile.SteelBallEntity;
import net.hydra.jojomod.event.powers.TimeStop;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**Renders the steel ball as a small sphere-like cube using the same model as the iron ball, with its own texture.
 * Balls with spin visibly rotate around the direction they are travelling in.*/
public class SteelBallRenderer extends EntityRenderer<SteelBallEntity> {

    private final StandFireballModel model;

    public static final ResourceLocation STEEL_BALL_TEXTURE = new ResourceLocation(Roundabout.MOD_ID, "textures/entity/projectile/steel_ball.png");

    public SteelBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new StandFireballModel(context.bakeLayer(ModEntityRendererClient.IRON_BALL_LAYER));
    }

    @Override
    public void render(SteelBallEntity ball, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (((TimeStop) ball.level()).inTimeStopRange(ball)) {
            partialTick = 0;
        }
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(ball.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ball.getXRot()));
        if (ball.getSpinLevel() > 0 && !ball.isStuckInGround()) {
            float direction = ball.getSpin() > 0 ? 1 : -1;
            poseStack.mulPose(Axis.XP.rotationDegrees(direction * (ball.tickCount + partialTick) * 38.0F * ball.getSpinLevel()));
        }
        poseStack.translate(0, -0.12f, 0);
        poseStack.scale(0.95f, 0.95f, 0.95f);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(ball)));
        this.model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1f);
        poseStack.popPose();
        super.render(ball, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SteelBallEntity ball) {
        return STEEL_BALL_TEXTURE;
    }
}
