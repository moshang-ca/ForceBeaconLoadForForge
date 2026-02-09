package org.moshang.forcebeaconloadforforge.mixin.client;



import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftRenderMixin {
    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setBeaconLevel(ClientLevel level, CallbackInfo ci) {
        ForceBeaconLoadClient.setLevel(level);
    }
}
