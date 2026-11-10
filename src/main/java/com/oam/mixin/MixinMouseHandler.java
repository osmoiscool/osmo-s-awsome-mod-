package com.oam.mixin;

import com.oam.OAMEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void oam$onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null || OAMEvents.isGuiOpen()) return;

        int button = buttonInfo.button() - 1;
        if (button == 1) button = 2;
        else if (button == 2) button = 1;

        if (OAMEvents.onMouseButton(button, action)) ci.cancel();
    }
}
