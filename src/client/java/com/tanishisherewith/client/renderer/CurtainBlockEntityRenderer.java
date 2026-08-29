package com.tanishisherewith.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.client.state.CurtainRenderState;
import com.tanishisherewith.entity.CurtainBlockEntity;
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
import org.jspecify.annotations.Nullable;

public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainRenderState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float ROD_Z = 0.875f;
    private static final float BASE_THICKNESS = 0.018f;

    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public CurtainRenderState createRenderState() {
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

            int packedLight = state.lightCoords != 0 ? state.lightCoords : 15728880;
            int packedOverlay = OverlayTexture.NO_OVERLAY;
            int numSegments = Math.max(1, state.segmentColors.size());


            for (int ix = 0; ix < w - 1; ix++) {
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

                    float nx = (y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0);
                    float ny = (z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0);
                    float nz = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0.001f) {
                        nx /= len; ny /= len; nz /= len;
                    } else {
                        nx = 0.0f; ny = 0.0f; nz = 1.0f;
                    }


                    float vTop = (float) iy / (h - 1);
                    float vBot = (float) (iy + 1) / (h - 1);

                    int topSegIdx = Mth.clamp((int) (vTop * numSegments), 0, numSegments - 1);
                    int topColor = state.segmentColors.get(topSegIdx);
                    float tR = (float) ((topColor >> 16) & 0xFF) / 255.0F;
                    float tG = (float) ((topColor >> 8) & 0xFF) / 255.0F;
                    float tB = (float) (topColor & 0xFF) / 255.0F;

                    int botSegIdx = Mth.clamp((int) (vBot * numSegments), 0, numSegments - 1);
                    int botColor = state.segmentColors.get(botSegIdx);
                    float bR = (float) ((botColor >> 16) & 0xFF) / 255.0F;
                    float bG = (float) ((botColor >> 8) & 0xFF) / 255.0F;
                    float bB = (float) (botColor & 0xFF) / 255.0F;

                    float foldShade = Mth.clamp(1.0f - Math.abs(state.meshZ[ix][iy]) * 4.0f, 0.70f, 1.0f);
                    tR *= foldShade; tG *= foldShade; tB *= foldShade;
                    bR *= foldShade; bG *= foldShade; bB *= foldShade;

                    float u0 = (float) ix / (w - 1) * (float) state.span;
                    float u1 = (float) (ix + 1) / (w - 1) * (float) state.span;
                    float v0 = vTop * (float) state.length;
                    float v1 = vBot * (float) state.length;

                    // Front
                    buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
                    buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
                    buffer.addVertex(matrix, x2, y2, z2 + BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
                    buffer.addVertex(matrix, x3, y3, z3 + BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);

                    // Back
                    float btR = tR * 0.85f; float btG = tG * 0.85f; float btB = tB * 0.85f;
                    float bbR = bR * 0.85f; float bbG = bG * 0.85f; float bbB = bB * 0.85f;
                    buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(btR, btG, btB, 1.0f).setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-nx, -ny, -nz);
                    buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(btR, btG, btB, 1.0f).setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-nx, -ny, -nz);
                    buffer.addVertex(matrix, x3, y3, z3 - BASE_THICKNESS).setColor(bbR, bbG, bbB, 1.0f).setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-nx, -ny, -nz);
                    buffer.addVertex(matrix, x2, y2, z2 - BASE_THICKNESS).setColor(bbR, bbG, bbB, 1.0f).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-nx, -ny, -nz);
                }
            }

            // Top Edge face
            int topEdgeColor = state.segmentColors.get(0);
            float topR = ((topEdgeColor >> 16 & 255) / 255.0F) * 0.90f;
            float topG = ((topEdgeColor >> 8 & 255) / 255.0F) * 0.90f;
            float topB = ((topEdgeColor & 255) / 255.0F) * 0.90f;

            for (int ix = 0; ix < w - 1; ix++) {
                float x0 = state.meshX[ix][0];
                float y0 = state.meshY[ix][0];
                float z0 = ROD_Z + state.meshZ[ix][0];
                float x1 = state.meshX[ix + 1][0];
                float y1 = state.meshY[ix + 1][0];
                float z1 = ROD_Z + state.meshZ[ix + 1][0];

                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(0.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(1.0f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(1.0f, 0.1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(topR, topG, topB, 1.0f).setUv(0.0f, 0.1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
            }

            // Bottom Edge face
            int bottom = h - 1;
            int bottomEdgeColor = state.segmentColors.getLast();
            float botR = ((bottomEdgeColor >> 16 & 255) / 255.0F) * 0.75f;
            float botG = ((bottomEdgeColor >> 8 & 255) / 255.0F) * 0.75f;
            float botB = ((bottomEdgeColor & 255) / 255.0F) * 0.75f;

            for (int ix = 0; ix < w - 1; ix++) {
                float x0 = state.meshX[ix][bottom];
                float y0 = state.meshY[ix][bottom];
                float z0 = ROD_Z + state.meshZ[ix][bottom];
                float x1 = state.meshX[ix + 1][bottom];
                float y1 = state.meshY[ix + 1][bottom];
                float z1 = ROD_Z + state.meshZ[ix + 1][bottom];

                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(0.0f, 0.9f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(1.0f, 0.9f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(1.0f, 1.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(botR, botG, botB, 1.0f).setUv(0.0f, 1.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
            }

            //Left edge face
            for (int iy = 0; iy < h - 1; iy++) {
                float vTop = (float) iy / (h - 1);
                float vBot = (float) (iy + 1) / (h - 1);

                int topSegIdx = Mth.clamp((int) (vTop * numSegments), 0, numSegments - 1);
                int topColor = state.segmentColors.get(topSegIdx);
                float tR = (((topColor >> 16) & 0xFF) / 255.0F) * 0.80f;
                float tG = (((topColor >> 8) & 0xFF) / 255.0F) * 0.80f;
                float tB = ((topColor & 0xFF) / 255.0F) * 0.80f;

                int botSegIdx = Mth.clamp((int) (vBot * numSegments), 0, numSegments - 1);
                int botColor = state.segmentColors.get(botSegIdx);
                float bR = (((botColor >> 16) & 0xFF) / 255.0F) * 0.80f;
                float bG = (((botColor >> 8) & 0xFF) / 255.0F) * 0.80f;
                float bB = ((botColor & 0xFF) / 255.0F) * 0.80f;

                float x0 = state.meshX[0][iy];
                float y0 = state.meshY[0][iy];
                float z0 = ROD_Z + state.meshZ[0][iy];
                float x1 = state.meshX[0][iy + 1];
                float y1 = state.meshY[0][iy + 1];
                float z1 = ROD_Z + state.meshZ[0][iy + 1];

                float v0 = vTop * (float) state.length;
                float v1 = vBot * (float) state.length;

                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(0.0f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(0.1f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(0.1f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(0.0f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
            }

            // right edge face
            int lastX = w - 1;
            for (int iy = 0; iy < h - 1; iy++) {
                float vTop = (float) iy / (h - 1);
                float vBot = (float) (iy + 1) / (h - 1);

                int topSegIdx = Mth.clamp((int) (vTop * numSegments), 0, numSegments - 1);
                int topColor = state.segmentColors.get(topSegIdx);
                float tR = (((topColor >> 16) & 0xFF) / 255.0F) * 0.80f;
                float tG = (((topColor >> 8) & 0xFF) / 255.0F) * 0.80f;
                float tB = ((topColor & 0xFF) / 255.0F) * 0.80f;

                int botSegIdx = Mth.clamp((int) (vBot * numSegments), 0, numSegments - 1);
                int botColor = state.segmentColors.get(botSegIdx);
                float bR = (((botColor >> 16) & 0xFF) / 255.0F) * 0.80f;
                float bG = (((botColor >> 8) & 0xFF) / 255.0F) * 0.80f;
                float bB = ((botColor & 0xFF) / 255.0F) * 0.80f;

                float x0 = state.meshX[lastX][iy];
                float y0 = state.meshY[lastX][iy];
                float z0 = ROD_Z + state.meshZ[lastX][iy];
                float x1 = state.meshX[lastX][iy + 1];
                float y1 = state.meshY[lastX][iy + 1];
                float z1 = ROD_Z + state.meshZ[lastX][iy + 1];

                float v0 = vTop * (float) state.length;
                float v1 = vBot * (float) state.length;

                buffer.addVertex(matrix, x0, y0, z0 + BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(0.0f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
                buffer.addVertex(matrix, x0, y0, z0 - BASE_THICKNESS).setColor(tR, tG, tB, 1.0f).setUv(0.1f, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
                buffer.addVertex(matrix, x1, y1, z1 - BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(0.1f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
                buffer.addVertex(matrix, x1, y1, z1 + BASE_THICKNESS).setColor(bR, bG, bB, 1.0f).setUv(0.0f, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
            }
        });

        matrices.popPose();
    }
}