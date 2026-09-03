package com.tanishisherewith.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.data.CachedOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

// if you don't do this, data-gen crashes with "Missing blockstate definitions"
@Mixin(ModelProvider.class)
public abstract class CustomModelProviderMixin {

    @Inject(method = "run", at = @At("HEAD"))
    private void bootstrapItemModels(CachedOutput cache, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        try {
            ItemModels.bootstrap();
        } catch (Throwable ignored) {
        }
    }

    @WrapOperation(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"
            )
    )
    private void dispatchBlockStateModels(BlockModelGenerators instance, Operation<Void> original) {
        if (((Object) this) instanceof FabricModelProvider fabricProvider) {
            fabricProvider.generateBlockStateModels(instance);
        } else {
            original.call(instance);
        }
    }

    @WrapOperation(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/data/models/ItemModelGenerators;run()V"
            )
    )
    private void dispatchItemModels(ItemModelGenerators instance, Operation<Void> original) {
        if (((Object) this) instanceof FabricModelProvider fabricProvider) {
            fabricProvider.generateItemModels(instance);
        } else {
            original.call(instance);
        }
    }
}