package com.tanishisherewith;

import com.tanishisherewith.registry.CurtainBlocks;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftCurtainsMain implements ModInitializer {
	public static final String MOD_ID = "softcurtains";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		//LOGGER.info("Soft Curtains loading");
		CurtainBlocks.register();
		CurtainsBlockEntities.register();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(CurtainBlocks.CURTAIN_ROD_ITEM);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
