package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

import java.util.function.Supplier;

public class CurtainsComponents {
    public static final Supplier<DataComponentType<DyeColor>> CURTAIN_COLOR = register("curtain_color",
            () -> DataComponentType.<DyeColor>builder()
                    .persistent(DyeColor.CODEC)
                    .networkSynchronized(ByteBufCodecs.idMapper(DyeColor::byId, DyeColor::getId))
                    .build());

    private static <T> Supplier<DataComponentType<T>> register(String name, Supplier<DataComponentType<T>> component) {
        DataComponentType<T> registered = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name), component.get());
        return () -> registered;
    }

    public static void register() {
    }
}