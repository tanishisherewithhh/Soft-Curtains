package com.tanishisherewith.client.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class SoftCurtainsDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(CurtainModelProvider::new);
        pack.addProvider(CurtainRecipeProvider::new);
        pack.addProvider(CurtainLootTableProvider::new);
    }
}