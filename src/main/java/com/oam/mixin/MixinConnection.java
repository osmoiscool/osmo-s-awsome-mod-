package com.oam.mixin;

import com.oam.OAMEvents;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void oam$onReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (OAMEvents.onPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void oam$onSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        OAMEvents.onPacketSend(packet);
    }
}
