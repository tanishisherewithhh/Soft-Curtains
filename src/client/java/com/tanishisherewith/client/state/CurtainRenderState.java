package com.tanishisherewith.client.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

public class CurtainRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public DyeColor color = DyeColor.WHITE;
    public boolean isOpen = false;
    public float sway = 0.0f;
    public int length = 2;
}