package com.tanishisherewith.client.mixin;


import com.tanishisherewith.client.CurtainDragController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, name = "xo")
    private double cancelHorizontalCameraTurn(double xo) {
        if ((Object) this instanceof LocalPlayer && CurtainDragController.isDragging()) {
            return 0.0;
        }
        return xo;
    }
}