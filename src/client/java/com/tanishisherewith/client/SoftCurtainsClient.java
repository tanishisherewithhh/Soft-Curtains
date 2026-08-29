package com.tanishisherewith.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.tanishisherewith.client.renderer.CurtainBlockEntityRenderer;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import com.tanishisherewith.registry.CurtainsItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorResolverRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;

public class SoftCurtainsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(CurtainsBlockEntities.CURTAIN, CurtainBlockEntityRenderer::new);
		CurtainDragController.register();
	}
}