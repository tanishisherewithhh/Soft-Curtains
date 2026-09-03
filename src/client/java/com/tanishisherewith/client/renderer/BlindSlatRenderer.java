package com.tanishisherewith.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class BlindSlatRenderer {
    private static final float SLAT_THICKNESS = 0.010f;

    public record Layout(float x0, float x1, float y, float z, float pitchRad, float depth) {}

    public static void renderSlat(PoseStack.Pose matrix, VertexConsumer buffer, Layout layout,
                                  float r, float g, float b, int light, int overlay) {
        float cos = (float) Math.cos(layout.pitchRad);
        float sin = (float) Math.sin(layout.pitchRad);

        float halfD = layout.depth * 0.5f;
        float halfT = SLAT_THICKNESS * 0.5f;

        float dyF = -halfD * sin;
        float dzF =  halfD * cos;
        float dyB =  halfD * sin;
        float dzB = -halfD * cos;

        float normY = cos;
        float normZ = sin;

        float yFT = layout.y + dyF + halfT * cos;
        float zFT = layout.z + dzF + halfT * sin;
        float yBT = layout.y + dyB + halfT * cos;
        float zBT = layout.z + dzB + halfT * sin;

        float yFB = layout.y + dyF - halfT * cos;
        float zFB = layout.z + dzF - halfT * sin;
        float yBB = layout.y + dyB - halfT * cos;
        float zBB = layout.z + dzB - halfT * sin;

        float br = r * 0.85f;
        float bg = g * 0.85f;
        float bb = b * 0.85f;

        buffer.addVertex(matrix, layout.x0, yBT, zBT).setColor(r, g, b, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, normY, normZ);
        buffer.addVertex(matrix, layout.x1, yBT, zBT).setColor(r, g, b, 1.0f).setUv(0.9f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, normY, normZ);
        buffer.addVertex(matrix, layout.x1, yFT, zFT).setColor(r, g, b, 1.0f).setUv(0.9f, 0.2f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, normY, normZ);
        buffer.addVertex(matrix, layout.x0, yFT, zFT).setColor(r, g, b, 1.0f).setUv(0.1f, 0.2f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, normY, normZ);
        
        buffer.addVertex(matrix, layout.x0, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.2f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, -normY, -normZ);
        buffer.addVertex(matrix, layout.x1, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.2f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, -normY, -normZ);
        buffer.addVertex(matrix, layout.x1, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, -normY, -normZ);
        buffer.addVertex(matrix, layout.x0, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, -normY, -normZ);

        buffer.addVertex(matrix, layout.x0, yFT, zFT).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(matrix, layout.x1, yFT, zFT).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(matrix, layout.x1, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(matrix, layout.x0, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, 1.0f);

        buffer.addVertex(matrix, layout.x1, yBT, zBT).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, -1.0f);
        buffer.addVertex(matrix, layout.x0, yBT, zBT).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, -1.0f);
        buffer.addVertex(matrix, layout.x0, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, -1.0f);
        buffer.addVertex(matrix, layout.x1, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.9f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 0.0f, 0.0f, -1.0f);

        buffer.addVertex(matrix, layout.x0, yBT, zBT).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, -1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x0, yFT, zFT).setColor(br, bg, bb, 1.0f).setUv(0.15f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, -1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x0, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.15f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, -1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x0, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, -1.0f, 0.0f, 0.0f);

        buffer.addVertex(matrix, layout.x1, yFT, zFT).setColor(br, bg, bb, 1.0f).setUv(0.15f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x1, yBT, zBT).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.1f).setOverlay(overlay).setLight(light).setNormal(matrix, 1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x1, yBB, zBB).setColor(br, bg, bb, 1.0f).setUv(0.1f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 1.0f, 0.0f, 0.0f);
        buffer.addVertex(matrix, layout.x1, yFB, zFB).setColor(br, bg, bb, 1.0f).setUv(0.15f, 0.15f).setOverlay(overlay).setLight(light).setNormal(matrix, 1.0f, 0.0f, 0.0f);
    }
}