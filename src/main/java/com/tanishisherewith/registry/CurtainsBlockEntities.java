package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.entity.CurtainBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CurtainsBlockEntities {
    public static BlockEntityType<CurtainBlockEntity> CURTAIN_BE_TYPE;

    public static void register() {
        CURTAIN_BE_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, "curtain_be"),
                FabricBlockEntityTypeBuilder.create(CurtainBlockEntity::new, CurtainBlocks.CURTAIN_ROD).build()
        );
    }
}
