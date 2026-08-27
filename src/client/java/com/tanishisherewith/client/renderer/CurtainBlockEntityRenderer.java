package com.tanishisherewith.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.client.state.CurtainRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainRenderState> {
    private static final Identifier CLOTH_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static boolean loggedOnce = false;
    private static boolean loggedExtract = false;
    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public CurtainRenderState createRenderState() {
        return new CurtainRenderState();
    }

    @Override
    public void extractRenderState(CurtainBlockEntity entity, CurtainRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = entity.getBlockState();
        if (blockState.getBlock() instanceof CurtainRodBlock) {
            state.facing = blockState.getValue(CurtainRodBlock.FACING);
            state.color = blockState.getValue(CurtainRodBlock.COLOR);
            state.isOpen = blockState.getValue(CurtainRodBlock.OPEN);
        }
        state.sway = Mth.lerp(partialTicks, entity.prevSwayOffset, entity.swayOffset);
        state.length = entity.length;

        if (!loggedExtract) {
            System.out.println("[SoftCurtains-DEBUG] extractRenderState called successfully for pos=" + entity.getBlockPos());
            loggedExtract = true;
        }
    }

    @Override
    public void submit(CurtainRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!loggedOnce) {
            System.out.println("[SoftCurtains-DEBUG] Submitting geometry! Facing=" + state.facing + ", Color=" + state.color + ", Open=" + state.isOpen + ", Length=" + state.length);
            loggedOnce = true;
        }

        Direction facing = state.facing;
        DyeColor dye = state.color;
        boolean isOpen = state.isOpen;
        float sway = state.sway;
        int packedLight = state.lightCoords;
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        int colorValue = dye.getTextureDiffuseColor();
        float r = (float) (colorValue >> 16 & 255) / 255.0F;
        float g = (float) (colorValue >> 8 & 255) / 255.0F;
        float b = (float) (colorValue & 255) / 255.0F;

        float width = isOpen ? 0.35f : 1.0f;
        float topY = 0.88f;
        float bottomY = -(state.length - 0.15f);
        float maxUvV = (float) state.length;
        float rodZ = 0.875f;
        RenderType renderType = RenderTypes.entityCutout(CLOTH_TEXTURE);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        float xLeft = (1.0f - width) / 2.0f;
        float xRight = xLeft + width;

        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            buffer.addVertex(matrix, xLeft, topY, rodZ).setColor(r, g, b, 1.0f).setUv(0.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
            buffer.addVertex(matrix, xRight, topY, rodZ).setColor(r, g, b, 1.0f).setUv(1.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
            buffer.addVertex(matrix, xRight, bottomY, rodZ + sway).setColor(r, g, b, 1.0f).setUv(1.0f, maxUvV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
            buffer.addVertex(matrix, xLeft, bottomY, rodZ + sway).setColor(r, g, b, 1.0f).setUv(0.0f, maxUvV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);

            buffer.addVertex(matrix, xRight, topY, rodZ).setColor(r, g, b, 1.0f).setUv(1.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
            buffer.addVertex(matrix, xLeft, topY, rodZ).setColor(r, g, b, 1.0f).setUv(0.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
            buffer.addVertex(matrix, xLeft, bottomY, rodZ + sway).setColor(r, g, b, 1.0f).setUv(0.0f, maxUvV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
            buffer.addVertex(matrix, xRight, bottomY, rodZ + sway).setColor(r, g, b, 1.0f).setUv(1.0f, maxUvV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
        });

        poseStack.popPose();
    }
}