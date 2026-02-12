package org.moshang.forcebeaconloadforforge.mixin.client;

import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {
    @Inject(method = "getViewDistance", at = @At("HEAD"), cancellable = true)
    private void changeViewDistance(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(65535);
    }
}
