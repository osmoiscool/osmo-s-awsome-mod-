package com.oam.mixin;

import com.oam.features.LeapRotationFix;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void oam$handleMovePlayerPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        LeapRotationFix.handleNoRotate(packet);
    }
}
