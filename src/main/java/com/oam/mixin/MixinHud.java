package com.oam.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.oam.features.SilentSwap;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public class MixinHud {

    @WrapOperation(method = "extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At(target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I", value = "INVOKE"))
    int getSelectedSlot(Inventory instance, Operation<Integer> original) {
        if (!SilentSwap.INSTANCE.isSilent()) {
            return original.call(instance);
        }

        return SilentSwap.INSTANCE.originalSlot();
    }
}
