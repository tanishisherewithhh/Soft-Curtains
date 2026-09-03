package com.tanishisherewith.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.client.state.CurtainRenderState;
import com.tanishisherewith.entity.CurtainBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainRenderState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float ROD_Z = 0.875f;
    private static final float BASE_THICKNESS = 0.015f;

    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public @NonNull CurtainRenderState createRenderState() {
        return new CurtainRenderState();
    }

    @Override
    public void extractRenderState(CurtainBlockEntity be, CurtainRenderState state, float tickDelta, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, tickDelta, cameraPos, crumblingOverlay);

        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof CurtainRodBlock) ||
                !blockState.getValue(CurtainRodBlock.HAS_CURTAIN) ||
                !be.isAnchor) {
            state.meshX = null;
            return;
        }

        be.ensureGrid();

        int w = be.getSpan() * CurtainBlockEntity.NODES_PER_BLOCK + 1;
        int h = CurtainBlockEntity.GRID_H;

        if (state.meshX == null || state.meshX.length != w) {
            state.meshX = new float[w][h];
            state.meshY = new float[w][h];
            state.meshZ = new float[w][h];
        }

        for (int ix = 0; ix < w; ix++) {
            for (int iy = 0; iy < h; iy++) {
                state.meshX[ix][iy] = be.getMeshX(ix, iy, tickDelta);
                state.meshY[ix][iy] = be.getMeshY(ix, iy, tickDelta);
                state.meshZ[ix][iy] = be.getMeshZ(ix, iy, tickDelta);
            }
        }

        state.color = be.getColor().getTextureDiffuseColor();
        state.segmentColors.clear();
        for (DyeColor segColor : be.getSegmentColors()) {
            state.segmentColors.add(segColor.getTextureDiffuseColor());
        }

        state.span = be.getSpan();
        state.expandRight = be.isExpandRight();
        state.length = be.getLength();
        state.facing = blockState.getValue(CurtainRodBlock.FACING);

        if (be.getLevel() != null) {
            state.lightCoords = Minecraft.getInstance().level.getLightEmission(be.getBlockPos());
        }
    }

    private static float[] getBlendedColor(CurtainRenderState state, float vProgress) {
        int count = state.segmentColors.size();
        if (count <= 1) {
            int c = count == 1 ? state.segmentColors.getFirst() : 0xFFFFFF;
            return new float[]{
                    ((c >> 16) & 0xFF) / 255.0F,
                    ((c >> 8) & 0xFF) / 255.0F,
                    (c & 0xFF) / 255.0F
            };
        }

        float pos = Mth.clamp(vProgress, 0.0f, 0.9999f) * count;
        int idx = (int) pos;
        float frac = pos - idx;

        int c0 = state.segmentColors.get(idx);

        if (frac <= 0.85f || idx >= count - 1) {
            return new float[]{
                    ((c0 >> 16) & 0xFF) / 255.0F,
                    ((c0 >> 8) & 0xFF) / 255.0F,
                    (c0 & 0xFF) / 255.0F
            };
        }

        int c1 = state.segmentColors.get(idx + 1);
        float t = (frac - 0.85f) / 0.15f;

        float r0 = ((c0 >> 16) & 0xFF) / 255.0F;
        float g0 = ((c0 >> 8) & 0xFF) / 255.0F;
        float b0 = (c0 & 0xFF) / 255.0F;

        float r1 = ((c1 >> 16) & 0xFF) / 255.0F;
        float g1 = ((c1 >> 8) & 0xFF) / 255.0F;
        float b1 = (c1 & 0xFF) / 255.0F;

        float r = (float) Math.sqrt(Mth.lerp(t, r0 * r0, r1 * r1));
        float g = (float) Math.sqrt(Mth.lerp(t, g0 * g0, g1 * g1));
        float b = (float) Math.sqrt(Mth.lerp(t, b0 * b0, b1 * b1));

        return new float[]{r, g, b};
    }

    @Override
    public void submit(CurtainRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        if (state.meshX == null || state.facing == null) {
            return;
        }

        matrices.pushPose();

        matrices.translate(0.5D, 0.0D, 0.5D);
        float rotation = switch (state.facing) {
            case SOUTH -> 180.0F;
            case WEST  -> 90.0F;
            case EAST  -> -90.0F;
            default    -> 0.0F;
        };
        matrices.mulPose(Axis.YP.rotationDegrees(rotation));
        matrices.translate(-0.5D, 0.0D, -0.5D);

        RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        submitNodeCollector.submitCustomGeometry(matrices, renderType, (matrix, buffer) -> {
            int w = state.meshX.length;
            int h = CurtainBlockEntity.GRID_H;

            int packedLight = state.lightCoords;
            int packedOverlay = OverlayTexture.NO_OVERLAY;

            for (int ix = 0; ix < w - 1; ix++) {
                float u0 = (float) (ix % CurtainBlockEntity.NODES_PER_BLOCK) / (float) CurtainBlockEntity.NODES_PER_BLOCK;
                float u1 = (float) ((ix % CurtainBlockEntity.NODES_PER_BLOCK) + 1) / (float) CurtainBlockEntity.NODES_PER_BLOCK;

                for (int iy = 0; iy < h - 1; iy++) {
                    float x0 = state.meshX[ix][iy];
                    float y0 = state.meshY[ix][iy];
                    float z0 = ROD_Z + state.meshZ[ix][iy];

                    float x1 = state.meshX[ix + 1][iy];
                    float y1 = state.meshY[ix + 1][iy];
                    float z1 = ROD_Z + state.meshZ[ix + 1][iy];

                    float x2 = state.meshX[ix + 1][iy + 1];
                    float y2 = state.meshY[ix + 1][iy + 1];
                    float z2 = ROD_Z + state.meshZ[ix + 1][iy + 1];

                    float x3 = state.meshX[ix][iy + 1];
                    float y3 = state.meshY[ix][iy + 1];
                    float z3 = ROD_Z + state.meshZ[ix][iy + 1];

                    float totalVProgress0 = ((float) iy / (h - 1)) * (float) state.length;
                    float totalVProgress1 = ((float) (iy + 1) / (h - 1)) * (float) state.length;

                    float v0 = totalVProgress0 - (float) Math.floor(totalVProgress0);
                    float v1 = totalVProgress1 - (float) Math.floor(totalVProgress0);

                    float vProgressTop = (float) iy / (h - 1);
                    float vProgressBot = (float) (iy + 1) / (h - 1);

                    float[] topC = getBlendedColor(state, vProgressTop);
                    float tR = topC[0];
                    float tG = topC[1];
                    float tB = topC[2];

                    float[] botC = getBlendedColor(state, vProgressBot);
                    float bR = botC[0];
                    float bG = botC[1];
                    float bB = botC[2];

                    buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS)
                            .setColor(tR, tG, tB, 1.0f)
                            .setUv(u0, v0)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, 1.0f);

                    buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS)
                            .setColor(tR, tG, tB, 1.0f)
                            .setUv(u1, v0)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, 1.0f);

                    buffer.addVertex(matrix, x2, y2, z2 + BASE_THICKNESS)
                            .setColor(bR, bG, bB, 1.0f)
                            .setUv(u1, v1)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, 1.0f);

                    buffer.addVertex(matrix, x3, y3, z3 + BASE_THICKNESS)
                            .setColor(bR, bG, bB, 1.0f)
                            .setUv(u0, v1)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, 1.0f);

                    float btR = tR * 0.95f;
                    float btG = tG * 0.95f;
                    float btB = tB * 0.95f;
                    float bbR = bR * 0.95f;
                    float bbG = bG * 0.95f;
                    float bbB = bB * 0.95f;

                    buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS)
                            .setColor(btR, btG, btB, 1.0f)
                            .setUv(u1, v0)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, -1.0f);

                    buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS)
                            .setColor(btR, btG, btB, 1.0f)
                            .setUv(u0, v0)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, -1.0f);

                    buffer.addVertex(matrix, x3, y3, z3 - BASE_THICKNESS)
                            .setColor(bbR, bbG, bbB, 1.0f)
                            .setUv(u0, v1)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, -1.0f);

                    buffer.addVertex(matrix, x2, y2, z2 - BASE_THICKNESS)
                            .setColor(bbR, bbG, bbB, 1.0f)
                            .setUv(u1, v1)
                            .setOverlay(packedOverlay)
                            .setLight(packedLight)
                            .setNormal(matrix, 0.0f, 0.0f, -1.0f);
                }
            }

            float[] topEdgeColor = getBlendedColor(state, 0.0f);
            float topR = topEdgeColor[0] * 0.85f;
            float topG = topEdgeColor[1] * 0.85f;
            float topB = topEdgeColor[2] * 0.85f;

            for (int ix = 0; ix < w - 1; ix++) {
                float u0 = (float) (ix % CurtainBlockEntity.NODES_PER_BLOCK) / (float) CurtainBlockEntity.NODES_PER_BLOCK;
                float u1 = (float) ((ix % CurtainBlockEntity.NODES_PER_BLOCK) + 1) / (float) CurtainBlockEntity.NODES_PER_BLOCK;

                float x0 = state.meshX[ix][0];
                float y0 = state.meshY[ix][0];
                float z0 = ROD_Z + state.meshZ[ix][0];
                float x1 = state.meshX[ix + 1][0];
                float y1 = state.meshY[ix + 1][0];
                float z1 = ROD_Z + state.meshZ[ix + 1][0];

                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(u0, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(u1, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(u1, 0.1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(u0, 0.1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
            }

            int bottom = h - 1;
            float[] bottomEdgeColor = getBlendedColor(state, 1.0f);
            float botR = bottomEdgeColor[0] * 0.70f;
            float botG = bottomEdgeColor[1] * 0.70f;
            float botB = bottomEdgeColor[2] * 0.70f;

            for (int ix = 0; ix < w - 1; ix++) {
                float u0 = (float) (ix % CurtainBlockEntity.NODES_PER_BLOCK) / (float) CurtainBlockEntity.NODES_PER_BLOCK;
                float u1 = (float) ((ix % CurtainBlockEntity.NODES_PER_BLOCK) + 1) / (float) CurtainBlockEntity.NODES_PER_BLOCK;

                float x0 = state.meshX[ix][bottom];
                float y0 = state.meshY[ix][bottom];
                float z0 = ROD_Z + state.meshZ[ix][bottom];
                float x1 = state.meshX[ix + 1][bottom];
                float y1 = state.meshY[ix + 1][bottom];
                float z1 = ROD_Z + state.meshZ[ix + 1][bottom];

                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(u0, 0.9f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(u1, 0.9f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(u1, 1.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(u0, 1.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
            }

            for (int iy = 0; iy < h - 1; iy++) {
                float totalVProgress0 = ((float) iy / (h - 1)) * (float) state.length;
                float totalVProgress1 = ((float) (iy + 1) / (h - 1)) * (float) state.length;

                float v0 = totalVProgress0 - (float) Math.floor(totalVProgress0);
                float v1 = totalVProgress1 - (float) Math.floor(totalVProgress0);

                float vProgressTop = (float) iy / (h - 1);
                float vProgressBot = (float) (iy + 1) / (h - 1);

                float[] topC = getBlendedColor(state, vProgressTop);
                float sideTopR = topC[0] * 0.80f;
                float sideTopG = topC[1] * 0.80f;
                float sideTopB = topC[2] * 0.80f;

                float[] botC = getBlendedColor(state, vProgressBot);
                float sideBotR = botC[0] * 0.80f;
                float sideBotG = botC[1] * 0.80f;
                float sideBotB = botC[2] * 0.80f;

                float x0 = state.meshX[0][iy];
                float y0 = state.meshY[0][iy];
                float z0 = ROD_Z + state.meshZ[0][iy];
                float x1 = state.meshX[0][iy + 1];
                float y1 = state.meshY[0][iy + 1];
                float z1 = ROD_Z + state.meshZ[0][iy + 1];

                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(sideTopR, sideTopG, sideTopB, 1.0f).setUv(0.0f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(sideTopR, sideTopG, sideTopB, 1.0f).setUv(0.1f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(sideBotR, sideBotG, sideBotB, 1.0f).setUv(0.1f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(sideBotR, sideBotG, sideBotB, 1.0f).setUv(0.0f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
            }

            int lastX = w - 1;
            for (int iy = 0; iy < h - 1; iy++) {
                float totalVProgress0 = ((float) iy / (h - 1)) * (float) state.length;
                float totalVProgress1 = ((float) (iy + 1) / (h - 1)) * (float) state.length;

                float v0 = totalVProgress0 - (float) Math.floor(totalVProgress0);
                float v1 = totalVProgress1 - (float) Math.floor(totalVProgress0);

                float vProgressTop = (float) iy / (h - 1);
                float vProgressBot = (float) (iy + 1) / (h - 1);

                float[] topC = getBlendedColor(state, vProgressTop);
                float sideTopR = topC[0] * 0.80f;
                float sideTopG = topC[1] * 0.80f;
                float sideTopB = topC[2] * 0.80f;

                float[] botC = getBlendedColor(state, vProgressBot);
                float sideBotR = botC[0] * 0.80f;
                float sideBotG = botC[1] * 0.80f;
                float sideBotB = botC[2] * 0.80f;

                float x0 = state.meshX[lastX][iy];
                float y0 = state.meshY[lastX][iy];
                float z0 = ROD_Z + state.meshZ[lastX][iy];
                float x1 = state.meshX[lastX][iy + 1];
                float y1 = state.meshY[lastX][iy + 1];
                float z1 = ROD_Z + state.meshZ[lastX][iy + 1];

                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(sideTopR, sideTopG, sideTopB, 1.0f).setUv(0.0f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(sideTopR, sideTopG, sideTopB, 1.0f).setUv(0.1f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(sideBotR, sideBotG, sideBotB, 1.0f).setUv(0.1f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(sideBotR, sideBotG, sideBotB, 1.0f).setUv(0.0f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(matrix, 0.0f, 0.0f, 1.0f);
            }
        });

        matrices.popPose();
    }
}