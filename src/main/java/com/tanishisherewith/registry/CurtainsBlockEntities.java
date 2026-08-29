package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.entity.CurtainBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;

import java.util.HashSet;
import java.util.Set;

public class CurtainsBlockEntities {
    public static final BlockEntityType<CurtainBlockEntity> CURTAIN = register(
            "curtain",
            CurtainBlockEntity::new,
            new HashSet<>(CurtainsBlocks.ROD_BLOCKS.values())
    );

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String name,
            BlockEntitySupplier<T> factory,
            Set<Block> blocks
    ) {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name),
                new BlockEntityType<>(factory, blocks)
        );
    }

    public static void register() {
    }
}