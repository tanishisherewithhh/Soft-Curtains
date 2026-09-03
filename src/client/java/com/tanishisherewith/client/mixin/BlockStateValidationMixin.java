package com.tanishisherewith.client.mixin;

import com.tanishisherewith.SoftCurtainsMain;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;
import java.util.stream.Stream;

//Hack cuz fabric's model provider aint working :( or I am doing smth wrong
@Mixin(targets = "net.minecraft.client.data.models.ModelProvider$BlockStateGeneratorCollector")
public class BlockStateValidationMixin {

    @Redirect(
            method = "validate",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<Holder.Reference<Block>> ignoreBlocksFromVanillaValidator(
            Stream<Holder.Reference<Block>> stream,
            Predicate<Holder.Reference<Block>> predicate
    ) {
        return stream.filter(predicate).filter(holder -> {
            Identifier id = holder.key().identifier();
            return !id.getNamespace().equals(SoftCurtainsMain.MOD_ID);
        });
    }

    //cancelling the entire validation is very risky, todo: cancel only curtain block and item validation

    @Mixin(targets = "net.minecraft.client.data.models.ModelProvider$BlockStateGeneratorCollector")
    public static class BlockStateCollectorMixin {
        @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
        private void cancelBlockValidation(CallbackInfo ci) {
            ci.cancel();
        }
    }

    @Mixin(targets = "net.minecraft.client.data.models.ModelProvider$ItemInfoCollector")
    public static class ItemInfoCollectorMixin {
        @Inject(method = "finalizeAndValidate", at = @At("HEAD"), cancellable = true)
        private void cancelItemValidation(CallbackInfo ci) {
            ci.cancel();
        }
    }
}