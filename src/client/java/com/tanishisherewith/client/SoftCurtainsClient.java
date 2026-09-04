package com.tanishisherewith.client;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.client.data.style.CurtainStyleProperty;
import com.tanishisherewith.client.renderer.CurtainBlockEntityRenderer;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;

public class SoftCurtainsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SelectItemModelProperties.ID_MAPPER.put(
				SoftCurtainsMain.id("curtain_style"),
				CurtainStyleProperty.TYPE
		);
		BlockEntityRenderers.register(CurtainsBlockEntities.CURTAIN, CurtainBlockEntityRenderer::new);
		CurtainDragController.register();
	}
}