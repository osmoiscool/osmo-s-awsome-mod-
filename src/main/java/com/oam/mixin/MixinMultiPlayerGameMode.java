package com.oam.mixin;

import com.oam.features.LeapHelper;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void oam$useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        LeapHelper.onItemUsed(player.getItemInHand(hand));
    }
}
