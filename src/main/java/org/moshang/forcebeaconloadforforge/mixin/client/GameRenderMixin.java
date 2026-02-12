package org.moshang.forcebeaconloadforforge.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRenderMixin {
    @Shadow private float renderDistance;

    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    private void changeFarPlane(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(renderDistance * 4 + 3000);
    }
}
