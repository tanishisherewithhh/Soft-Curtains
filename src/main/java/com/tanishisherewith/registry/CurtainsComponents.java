package com.tanishisherewith.registry;

import com.tanishisherewith.SoftCurtainsMain;
import com.tanishisherewith.entity.CurtainStyle;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class CurtainsComponents {
    public static final Supplier<DataComponentType<CurtainStyle>> CURTAIN_STYLE = register("curtain_style",
            () -> DataComponentType.<CurtainStyle>builder()
                    .persistent(CurtainStyle.CODEC)
                    .networkSynchronized(CurtainStyle.STREAM_CODEC)
                    .build());

    private static <T> Supplier<DataComponentType<T>> register(String name, Supplier<DataComponentType<T>> component) {
        DataComponentType<T> registered = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SoftCurtainsMain.MOD_ID, name), component.get());
        return () -> registered;
    }

    public static void register() {
    }
}