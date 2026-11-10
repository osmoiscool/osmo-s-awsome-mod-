package com.oam.mixin;

import com.oam.features.SilentSwap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class MixinInventory {
    @Inject(method = "getSelectedItem", at = @At("HEAD"), cancellable = true)
    private void redirectSelectedItem(CallbackInfoReturnable<ItemStack> cir) {
        if (!SilentSwap.INSTANCE.isSilent()) {
            return;
        }

        Inventory inventory = (Inventory) (Object) this;

        Integer slot = SilentSwap.INSTANCE.getCurrentSlot();
        cir.setReturnValue(inventory.getItem(slot));
    }

    @Inject(method = "getSelectedSlot", at = @At("HEAD"), cancellable = true)
    void getSelectedSlot(CallbackInfoReturnable<Integer> cir) {
        if (!SilentSwap.INSTANCE.isSilent()) {
            return;
        }

        cir.setReturnValue(SilentSwap.INSTANCE.getCurrentSlot());
    }
}
