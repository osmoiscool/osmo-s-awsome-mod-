package com.oam.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.oam.features.SilentSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonHandsAndItems.class)
public class MixinFirstPersonHandsAndItems {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    ItemStack getMainHandItem(LocalPlayer instance, Operation<ItemStack> original) {
        if (!SilentSwap.INSTANCE.isSilent() || instance != Minecraft.getInstance().player) {
            return original.call(instance);
        }

        return SilentSwap.INSTANCE.originalStack();
    }
}
