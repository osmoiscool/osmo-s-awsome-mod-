package com.oam.mixin;

import com.oam.features.LeapHelper;
import com.oam.features.LeapRotationFix;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "handleContainerInput", at = @At("HEAD"))
    private void oam$handleContainerInput(int containerId, int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
        LeapRotationFix.click(containerId, slotId);
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void oam$useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        LeapHelper.onItemUsed(player.getItemInHand(hand));
    }
}
