package com.tanishisherewith.client.data.recipe;

import com.tanishisherewith.registry.CurtainsBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class CurtainLootTableProvider extends FabricBlockLootSubProvider {
    public CurtainLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        for (Block rodBlock : CurtainsBlocks.ROD_BLOCKS.values()) {
            this.dropSelf(rodBlock);
        }
    }
}