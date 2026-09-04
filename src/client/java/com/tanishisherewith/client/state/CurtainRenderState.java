package com.tanishisherewith.client.state;

import com.tanishisherewith.entity.CurtainStyle;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class CurtainRenderState extends BlockEntityRenderState {
    public float[][] meshX;
    public float[][] meshY;
    public float[][] meshZ;
    public int color;
    public List<Integer> segmentColors = new ArrayList<>();
    public int span;
    public boolean expandRight;
    public int length;
    public Direction facing;
    public float openProgress;
    public CurtainStyle style = CurtainStyle.DRAPES;
    public float swayZ;
    public int[] lightLevels;
    public float minX;
    public float maxX;
}