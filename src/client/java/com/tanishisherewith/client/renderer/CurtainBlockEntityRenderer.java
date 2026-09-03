package com.tanishisherewith.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainRenderState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float ROD_Z = 0.875f;
    private static final float BASE_THICKNESS = 0.015f;
    private static final float SHUTTER_THICKNESS = 0.045f;

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

        state.lightCoords = 15728880;
        state.style = be.getStyle();
        state.openProgress = be.getOpenProgress();

        int midX = w / 2;
        state.swayZ = state.meshZ[midX][h - 1];

        float[] usableBounds = be.getUsableHorizontalBounds();
        state.minX = usableBounds[0];
        state.maxX = usableBounds[1];
    }

    private static float[] getBlendedColor(CurtainRenderState state, float vProgress) {
        int count = state.segmentColors.size();
        if (count <= 1) {
            int c = count == 1 ? state.segmentColors.getFirst() : 0xFFFFFF;
            return unpackRgb(c);
        }

        float pos = Mth.clamp(vProgress, 0.0f, 0.9999f) * count;
        int idx = (int) pos;
        float frac = pos - idx;

        int c0 = state.segmentColors.get(idx);
        if (frac <= 0.85f || idx >= count - 1) {
            return unpackRgb(c0);
        }

        int c1 = state.segmentColors.get(idx + 1);
        float t = (frac - 0.85f) / 0.15f;

        float[] rgb0 = unpackRgb(c0);
        float[] rgb1 = unpackRgb(c1);

        float r = (float) Math.sqrt(Mth.lerp(t, rgb0[0] * rgb0[0], rgb1[0] * rgb1[0]));
        float g = (float) Math.sqrt(Mth.lerp(t, rgb0[1] * rgb0[1], rgb1[1] * rgb1[1]));
        float b = (float) Math.sqrt(Mth.lerp(t, rgb0[2] * rgb0[2], rgb1[2] * rgb1[2]));

        return new float[]{r, g, b};
    }

    private static float[] unpackRgb(int rgb) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F
        };
    }

    private static void putQuad(PoseStack.Pose matrix, VertexConsumer buffer,
                                float x0, float y0, float z0, float u0, float v0, float[] c0,
                                float x1, float y1, float z1, float u1, float v1, float[] c1,
                                float x2, float y2, float z2, float u2, float v2, float[] c2,
                                float x3, float y3, float z3, float u3, float v3, float[] c3,
                                float nx, float ny, float nz, int light, int overlay) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(c0[0], c0[1], c0[2], 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
        buffer.addVertex(matrix, x1, y1, z1).setColor(c1[0], c1[1], c1[2], 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2).setColor(c2[0], c2[1], c2[2], 1.0f).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
        buffer.addVertex(matrix, x3, y3, z3).setColor(c3[0], c3[1], c3[2], 1.0f).setUv(u3, v3).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
    }

    private static void putQuadUniformColor(PoseStack.Pose matrix, VertexConsumer buffer,
                                            float x0, float y0, float z0, float u0, float v0,
                                            float x1, float y1, float z1, float u1, float v1,
                                            float x2, float y2, float z2, float u2, float v2,
                                            float x3, float y3, float z3, float u3, float v3,
                                            float[] color, float nx, float ny, float nz, int light, int overlay) {
        putQuad(matrix, buffer,
                x0, y0, z0, u0, v0, color,
                x1, y1, z1, u1, v1, color,
                x2, y2, z2, u2, v2, color,
                x3, y3, z3, u3, v3, color,
                nx, ny, nz, light, overlay);
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

        switch (state.style) {
            case BLINDS -> renderBlinds(state, matrices, submitNodeCollector);
            case SHUTTERS -> renderShutters(state, matrices, submitNodeCollector);
            case ROLLER -> renderRoller(state, matrices, submitNodeCollector);
            case DRAPES -> renderDrapesMesh(state, matrices, submitNodeCollector);
        }

        matrices.popPose();
    }

    private void renderBlinds(CurtainRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        submitNodeCollector.submitCustomGeometry(matrices, renderType, (matrix, buffer) -> {
            int light = state.lightCoords;
            int overlay = OverlayTexture.NO_OVERLAY;
            float x0 = state.minX;
            float x1 = state.maxX;

            float topY = 0.730f;
            float totalHangLength = (float) state.length - (1.0f - topY);

            int totalSlats = Math.max(5, state.length * 8);
            float slatSpacing = totalHangLength / (float) totalSlats;
            float bottomY = topY - ((totalSlats - 1) * slatSpacing);

            float slatDepth = (slatSpacing / (float) Math.sin(Math.toRadians(84.0f))) * 1.25f;
            float pitchAngle = (float) Math.toRadians((1.0f - state.openProgress) * 84.0f);

            int segCount = state.segmentColors.size();

            for (int i = 0; i < totalSlats; i++) {
                float fraction = (float) i / (float) (totalSlats - 1);
                float y = topY - (i * slatSpacing);
                float currentZ = ROD_Z + (state.swayZ * fraction);

                int colorVal;
                if (segCount <= 1) {
                    colorVal = segCount == 1 ? state.segmentColors.getFirst() : 0xFFFFFF;
                } else {
                    int segIndex = Mth.clamp((int) (fraction * segCount), 0, segCount - 1);
                    colorVal = state.segmentColors.get(segIndex);
                }

                float[] rgb = unpackRgb(colorVal);
                BlindSlatRenderer.Layout layout = new BlindSlatRenderer.Layout(x0, x1, y, currentZ, pitchAngle, slatDepth);
                BlindSlatRenderer.renderSlat(matrix, buffer, layout, rgb[0], rgb[1], rgb[2], light, overlay);
            }

            renderBlindCords(matrix, buffer, state, x0, x1, bottomY, slatDepth, pitchAngle, light, overlay);
        });
    }

    private void renderBlindCords(PoseStack.Pose matrix, VertexConsumer buffer,
                                  CurtainRenderState state, float x0, float x1,
                                  float bottomY, float slatDepth, float pitchAngle,
                                  int light, int overlay) {
        float cordRadius = 0.0035f;
        float halfD = slatDepth * 0.40f;

        float rodCenterY = CurtainBlockEntity.CURTAIN_TOP_Y;

        float[] cordColor = new float[]{0.22f, 0.22f, 0.22f};

        float cos = (float) Math.cos(pitchAngle);
        float dzOffset = halfD * cos;

        float baseBlockX = state.expandRight ? 0.0f : 1.0f - (float) state.span;

        List<Float> cordXPositions = new ArrayList<>();
        cordXPositions.add(x0 + 0.03f);

        for (int i = 1; i < state.span; i++) {
            float innerX = baseBlockX + (float) i;
            if (innerX > x0 + 0.05f && innerX < x1 - 0.05f) {
                cordXPositions.add(innerX);
            }
        }

        cordXPositions.add(x1 - 0.03f);

        for (float cx : cordXPositions) {
            float botZ = ROD_Z + state.swayZ;

            float zF_Top = ROD_Z + dzOffset;
            float zF_Bot = botZ + dzOffset;
            putQuadUniformColor(matrix, buffer,
                    cx - cordRadius, rodCenterY, zF_Top, 0.0f, 0.0f,
                    cx + cordRadius, rodCenterY, zF_Top, 0.1f, 0.0f,
                    cx + cordRadius, bottomY, zF_Bot, 0.1f, 1.0f,
                    cx - cordRadius, bottomY, zF_Bot, 0.0f, 1.0f,
                    cordColor, 0.0f, 0.0f, 1.0f, light, overlay);

            float zB_Top = ROD_Z - dzOffset;
            float zB_Bot = botZ - dzOffset;
            putQuadUniformColor(matrix, buffer,
                    cx + cordRadius, rodCenterY, zB_Top, 0.0f, 0.0f,
                    cx - cordRadius, rodCenterY, zB_Top, 0.1f, 0.0f,
                    cx - cordRadius, bottomY, zB_Bot, 0.1f, 1.0f,
                    cx + cordRadius, bottomY, zB_Bot, 0.0f, 1.0f,
                    cordColor, 0.0f, 0.0f, -1.0f, light, overlay);
        }
    }

    private void renderShutters(CurtainRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        submitNodeCollector.submitCustomGeometry(matrices, renderType, (matrix, buffer) -> {
            int light = state.lightCoords;
            int overlay = OverlayTexture.NO_OVERLAY;

            float x0 = state.minX;
            float x1 = state.maxX;

            float topY = CurtainBlockEntity.CURTAIN_TOP_Y;
            float panelH = (float) state.length;

            float swingPitch = (float) Math.toRadians(state.openProgress * 85.0f);
            float cos = (float) Math.cos(swingPitch);
            float sin = (float) Math.sin(swingPitch);

            float halfT = SHUTTER_THICKNESS * 0.5f;

            //rotate normals so it always appears bright on both sides.
            //todo make the bottom side darker for shadow effect
            float frontNormY = sin;
            float frontNormZ = cos;

            float backNormY  = sin;
            float backNormZ  = -cos;

            int slices = Math.max(4, state.length * 8);
            float sliceH = panelH / slices;

            for (int s = 0; s < slices; s++) {
                float f0 = (float) s / slices;
                float f1 = (float) (s + 1) / slices;

                float h0 = s * sliceH;
                float h1 = (s + 1) * sliceH;

                float yTopF = topY - (h0 * cos) - (halfT * sin);
                float zTopF = ROD_Z + (h0 * sin) + (halfT * cos);
                float yBotF = topY - (h1 * cos) - (halfT * sin);
                float zBotF = ROD_Z + (h1 * sin) + (halfT * cos);

                float yTopB = topY - (h0 * cos) + (halfT * sin);
                float zTopB = ROD_Z + (h0 * sin) - (halfT * cos);
                float yBotB = topY - (h1 * cos) + (halfT * sin);
                float zBotB = ROD_Z + (h1 * sin) - (halfT * cos);

                float[] c0 = getBlendedColor(state, f0);
                float[] c1 = getBlendedColor(state, f1);

                float v0 = f0 * state.length;
                float v1 = f1 * state.length;

                putQuad(matrix, buffer,
                        x0, yTopF, zTopF, 0.0f, v0, c0,
                        x1, yTopF, zTopF, 1.0f, v0, c0,
                        x1, yBotF, zBotF, 1.0f, v1, c1,
                        x0, yBotF, zBotF, 0.0f, v1, c1,
                        0.0f, frontNormY, frontNormZ, light, overlay);

                // Back surface slice
                putQuad(matrix, buffer,
                        x1, yTopB, zTopB, 1.0f, v0, c0,
                        x0, yTopB, zTopB, 0.0f, v0, c0,
                        x0, yBotB, zBotB, 0.0f, v1, c1,
                        x1, yBotB, zBotB, 1.0f, v1, c1,
                        0.0f, backNormY, backNormZ, light, overlay);

                putQuad(matrix, buffer,
                        x0, yTopB, zTopB, 0.0f, v0, c0,
                        x0, yTopF, zTopF, 0.05f, v0, c0,
                        x0, yBotF, zBotF, 0.05f, v1, c1,
                        x0, yBotB, zBotB, 0.0f, v1, c1,
                        -1.0f, 0.0f, 0.0f, light, overlay);

                putQuad(matrix, buffer,
                        x1, yTopF, zTopF, 0.0f, v0, c0,
                        x1, yTopB, zTopB, 0.05f, v0, c0,
                        x1, yBotB, zBotB, 0.05f, v1, c1,
                        x1, yBotF, zBotF, 0.0f, v1, c1,
                        1.0f, 0.0f, 0.0f, light, overlay);
            }

            float botYFront = topY - (panelH * cos) - (halfT * sin);
            float botZFront = ROD_Z + (panelH * sin) + (halfT * cos);
            float botYBack  = topY - (panelH * cos) + (halfT * sin);
            float botZBack  = ROD_Z + (panelH * sin) - (halfT * cos);

            float[] botC = getBlendedColor(state, 1.0f);
            float[] capColor = new float[]{botC[0] * 0.95f, botC[1] * 0.95f, botC[2] * 0.95f};

            putQuadUniformColor(matrix, buffer,
                    x0, botYFront, botZFront, 0.0f, 0.0f,
                    x1, botYFront, botZFront, 1.0f, 0.0f,
                    x1, botYBack, botZBack, 1.0f, 0.05f,
                    x0, botYBack, botZBack, 0.0f, 0.05f,
                    capColor, 0.0f, -cos, sin, light, overlay);
        });
    }


    private void renderRoller(CurtainRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        submitNodeCollector.submitCustomGeometry(matrices, renderType, (matrix, buffer) -> {
            int light = state.lightCoords;
            int overlay = OverlayTexture.NO_OVERLAY;

            float x0 = state.minX;
            float x1 = state.maxX;

            float rodCenterY = CurtainBlockEntity.CURTAIN_TOP_Y;
            float rodCenterZ = ROD_Z;

            float totalLength = (float) state.length;
            float visibleLength = totalLength * (1.0f - state.openProgress);

            float minRollRadius = 0.032f;
            float maxRollRadius = 0.048f;
            float currentRadius = Mth.lerp(state.openProgress, minRollRadius, maxRollRadius);

            float rolledTurns = state.openProgress * totalLength * 2.5f;
            float rollAngleOffset = (float) (rolledTurns * 2.0 * Math.PI);

            float[] rollColor = getBlendedColor(state, state.openProgress);

            int rollSegments = 12;
            for (int seg = 0; seg < rollSegments; seg++) {
                float a0 = rollAngleOffset + (float) (seg * 2.0 * Math.PI / rollSegments);
                float a1 = rollAngleOffset + (float) ((seg + 1) * 2.0 * Math.PI / rollSegments);

                float y0 = rodCenterY + currentRadius * (float) Math.sin(a0);
                float z0 = rodCenterZ + currentRadius * (float) Math.cos(a0);
                float y1 = rodCenterY + currentRadius * (float) Math.sin(a1);
                float z1 = rodCenterZ + currentRadius * (float) Math.cos(a1);

                float ny = (float) Math.sin((a0 + a1) * 0.5f);
                float nz = (float) Math.cos((a0 + a1) * 0.5f);

                float u0 = (float) seg / rollSegments;
                float u1 = (float) (seg + 1) / rollSegments;

                putQuadUniformColor(matrix, buffer,
                        x0, y0, z0, u0, 0.0f,
                        x1, y0, z0, u0, 1.0f,
                        x1, y1, z1, u1, 1.0f,
                        x0, y1, z1, u1, 0.0f,
                        rollColor, 0.0f, ny, nz, light, overlay);
            }

            float sheetTopZ = rodCenterZ + currentRadius;
            float botY = rodCenterY - visibleLength;

            float swayFactor = 1.0f - state.openProgress;
            float botZ = sheetTopZ + (state.swayZ * swayFactor);

            if (visibleLength > 0.01f) {
                int verticalSlices = Math.max(4, Math.round(visibleLength * 12));
                float sliceHeight = visibleLength / verticalSlices;

                for (int s = 0; s < verticalSlices; s++) {
                    float localFrac0 = (float) s / verticalSlices;
                    float localFrac1 = (float) (s + 1) / verticalSlices;

                    float vProg0 = Mth.lerp(localFrac0, state.openProgress, 1.0f);
                    float vProg1 = Mth.lerp(localFrac1, state.openProgress, 1.0f);

                    float yTopSlice = rodCenterY - (s * sliceHeight);
                    float yBotSlice = rodCenterY - ((s + 1) * sliceHeight);

                    float zTopSlice = Mth.lerp(localFrac0, sheetTopZ, botZ);
                    float zBotSlice = Mth.lerp(localFrac1, sheetTopZ, botZ);

                    float[] c0 = getBlendedColor(state, vProg0);
                    float[] c1 = getBlendedColor(state, vProg1);

                    float uvV0 = vProg0 * totalLength;
                    float uvV1 = vProg1 * totalLength;

                    putQuad(matrix, buffer,
                            x0, yTopSlice, zTopSlice, 0.0f, uvV0, c0,
                            x1, yTopSlice, zTopSlice, (float) state.span, uvV0, c0,
                            x1, yBotSlice, zBotSlice, (float) state.span, uvV1, c1,
                            x0, yBotSlice, zBotSlice, 0.0f, uvV1, c1,
                            0.0f, 0.0f, 1.0f, light, overlay);

                    float[] bc0 = new float[]{c0[0] * 0.95f, c0[1] * 0.95f, c0[2] * 0.95f};
                    float[] bc1 = new float[]{c1[0] * 0.95f, c1[1] * 0.95f, c1[2] * 0.95f};

                    putQuad(matrix, buffer,
                            x1, yTopSlice, zTopSlice - BASE_THICKNESS, (float) state.span, uvV0, bc0,
                            x0, yTopSlice, zTopSlice - BASE_THICKNESS, 0.0f, uvV0, bc0,
                            x0, yBotSlice, zBotSlice - BASE_THICKNESS, 0.0f, uvV1, bc1,
                            x1, yBotSlice, zBotSlice - BASE_THICKNESS, (float) state.span, uvV1, bc1,
                            0.0f, 0.0f, -1.0f, light, overlay);
                }
            }

            float barHeight = 0.024f;
            float barThickHalf = BASE_THICKNESS * 0.5f;

            float barYTop = botY + barHeight;

            float barZF = botZ + barThickHalf;
            float barZB = botZ - BASE_THICKNESS - barThickHalf;

            float[] endColor = getBlendedColor(state, 1.0f);
            float[] barColor = new float[]{endColor[0] * 0.85f, endColor[1] * 0.85f, endColor[2] * 0.85f};
            float[] bottomCapColor = new float[]{barColor[0] * 0.9f, barColor[1] * 0.9f, barColor[2] * 0.9f};

            putQuadUniformColor(matrix, buffer,
                    x0, barYTop, barZF, 0.0f, 0.0f,
                    x1, barYTop, barZF, 1.0f, 0.0f,
                    x1, botY, barZF, 1.0f, 0.1f,
                    x0, botY, barZF, 0.0f, 0.1f,
                    barColor, 0.0f, 0.0f, 1.0f, light, overlay);

            putQuadUniformColor(matrix, buffer,
                    x1, barYTop, barZB, 1.0f, 0.0f,
                    x0, barYTop, barZB, 0.0f, 0.0f,
                    x0, botY, barZB, 0.0f, 0.1f,
                    x1, botY, barZB, 1.0f, 0.1f,
                    barColor, 0.0f, 0.0f, -1.0f, light, overlay);

            putQuadUniformColor(matrix, buffer,
                    x0, botY, barZF, 0.0f, 0.0f,
                    x1, botY, barZF, 1.0f, 0.0f,
                    x1, botY, barZB, 1.0f, 0.05f,
                    x0, botY, barZB, 0.0f, 0.05f,
                    bottomCapColor, 0.0f, -1.0f, 0.0f, light, overlay);
        });
    }

    private void renderDrapesMesh(CurtainRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
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
                    float[] botC = getBlendedColor(state, vProgressBot);

                    putQuad(matrix, buffer,
                            x0, y0, z0 + BASE_THICKNESS, u0, v0, topC,
                            x1, y1, z1 + BASE_THICKNESS, u1, v0, topC,
                            x2, y2, z2 + BASE_THICKNESS, u1, v1, botC,
                            x3, y3, z3 + BASE_THICKNESS, u0, v1, botC,
                            0.0f, 0.0f, 1.0f, packedLight, packedOverlay);

                    float[] btC = new float[]{topC[0] * 0.95f, topC[1] * 0.95f, topC[2] * 0.95f};
                    float[] bbC = new float[]{botC[0] * 0.95f, botC[1] * 0.95f, botC[2] * 0.95f};

                    putQuad(matrix, buffer,
                            x1, y1, z1 - BASE_THICKNESS, u1, v0, btC,
                            x0, y0, z0 - BASE_THICKNESS, u0, v0, btC,
                            x3, y3, z3 - BASE_THICKNESS, u0, v1, bbC,
                            x2, y2, z2 - BASE_THICKNESS, u1, v1, bbC,
                            0.0f, 0.0f, -1.0f, packedLight, packedOverlay);
                }
            }

            float[] topEdgeColor = getBlendedColor(state, 0.0f);
            float[] topEdgeShaded = new float[]{topEdgeColor[0] * 0.85f, topEdgeColor[1] * 0.85f, topEdgeColor[2] * 0.85f};

            for (int ix = 0; ix < w - 1; ix++) {
                float u0 = (float) (ix % CurtainBlockEntity.NODES_PER_BLOCK) / (float) CurtainBlockEntity.NODES_PER_BLOCK;
                float u1 = (float) ((ix % CurtainBlockEntity.NODES_PER_BLOCK) + 1) / (float) CurtainBlockEntity.NODES_PER_BLOCK;

                float x0 = state.meshX[ix][0];
                float y0 = state.meshY[ix][0];
                float z0 = ROD_Z + state.meshZ[ix][0];
                float x1 = state.meshX[ix + 1][0];
                float y1 = state.meshY[ix + 1][0];
                float z1 = ROD_Z + state.meshZ[ix + 1][0];

                putQuadUniformColor(matrix, buffer,
                        x0, y0, z0 - BASE_THICKNESS, u0, 0.0f,
                        x1, y1, z1 - BASE_THICKNESS, u1, 0.0f,
                        x1, y1, z1 + BASE_THICKNESS, u1, 0.1f,
                        x0, y0, z0 + BASE_THICKNESS, u0, 0.1f,
                        topEdgeShaded, 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
            }

            int bottom = h - 1;
            float[] bottomEdgeColor = getBlendedColor(state, 1.0f);
            float[] botEdgeShaded = new float[]{bottomEdgeColor[0] * 0.70f, bottomEdgeColor[1] * 0.70f, bottomEdgeColor[2] * 0.70f};

            for (int ix = 0; ix < w - 1; ix++) {
                float u0 = (float) (ix % CurtainBlockEntity.NODES_PER_BLOCK) / (float) CurtainBlockEntity.NODES_PER_BLOCK;
                float u1 = (float) ((ix % CurtainBlockEntity.NODES_PER_BLOCK) + 1) / (float) CurtainBlockEntity.NODES_PER_BLOCK;

                float x0 = state.meshX[ix][bottom];
                float y0 = state.meshY[ix][bottom];
                float z0 = ROD_Z + state.meshZ[ix][bottom];
                float x1 = state.meshX[ix + 1][bottom];
                float y1 = state.meshY[ix + 1][bottom];
                float z1 = ROD_Z + state.meshZ[ix + 1][bottom];

                putQuadUniformColor(matrix, buffer,
                        x0, y0, z0 + BASE_THICKNESS, u0, 0.9f,
                        x1, y1, z1 + BASE_THICKNESS, u1, 0.9f,
                        x1, y1, z1 - BASE_THICKNESS, u1, 1.0f,
                        x0, y0, z0 - BASE_THICKNESS, u0, 1.0f,
                        botEdgeShaded, 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
            }

            for (int iy = 0; iy < h - 1; iy++) {
                float totalVProgress0 = ((float) iy / (h - 1)) * (float) state.length;
                float totalVProgress1 = ((float) (iy + 1) / (h - 1)) * (float) state.length;

                float v0 = totalVProgress0 - (float) Math.floor(totalVProgress0);
                float v1 = totalVProgress1 - (float) Math.floor(totalVProgress0);

                float vProgressTop = (float) iy / (h - 1);
                float vProgressBot = (float) (iy + 1) / (h - 1);

                float[] topC = getBlendedColor(state, vProgressTop);
                float[] botC = getBlendedColor(state, vProgressBot);

                float[] sideTop = new float[]{topC[0] * 0.80f, topC[1] * 0.80f, topC[2] * 0.80f};
                float[] sideBot = new float[]{botC[0] * 0.80f, botC[1] * 0.80f, botC[2] * 0.80f};

                float x0 = state.meshX[0][iy];
                float y0 = state.meshY[0][iy];
                float z0 = ROD_Z + state.meshZ[0][iy];
                float x1 = state.meshX[0][iy + 1];
                float y1 = state.meshY[0][iy + 1];
                float z1 = ROD_Z + state.meshZ[0][iy + 1];

                putQuad(matrix, buffer,
                        x0, y0, z0 - BASE_THICKNESS, 0.0f, v0, sideTop,
                        x0, y0, z0 + BASE_THICKNESS, 0.1f, v0, sideTop,
                        x1, y1, z1 + BASE_THICKNESS, 0.1f, v1, sideBot,
                        x1, y1, z1 - BASE_THICKNESS, 0.0f, v1, sideBot,
                        0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
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
                float[] botC = getBlendedColor(state, vProgressBot);

                float[] sideTop = new float[]{topC[0] * 0.80f, topC[1] * 0.80f, topC[2] * 0.80f};
                float[] sideBot = new float[]{botC[0] * 0.80f, botC[1] * 0.80f, botC[2] * 0.80f};

                float x0 = state.meshX[lastX][iy];
                float y0 = state.meshY[lastX][iy];
                float z0 = ROD_Z + state.meshZ[lastX][iy];
                float x1 = state.meshX[lastX][iy + 1];
                float y1 = state.meshY[lastX][iy + 1];
                float z1 = ROD_Z + state.meshZ[lastX][iy + 1];

                putQuad(matrix, buffer,
                        x0, y0, z0 + BASE_THICKNESS, 0.0f, v0, sideTop,
                        x0, y0, z0 - BASE_THICKNESS, 0.1f, v0, sideTop,
                        x1, y1, z1 - BASE_THICKNESS, 0.1f, v1, sideBot,
                        x1, y1, z1 + BASE_THICKNESS, 0.0f, v1, sideBot,
                        0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
            }
        });
    }
}