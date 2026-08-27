package com.tanishisherewith.client;

import com.tanishisherewith.client.renderer.CurtainBlockEntityRenderer;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class SoftCurtainsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(CurtainsBlockEntities.CURTAIN_BE_TYPE, CurtainBlockEntityRenderer::new);
	}
}