package org.moshang.forcebeaconloadforforge.mixin.client;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(value = RenderSection.class, remap = false)
public class RenderSectionMixin {
    @Shadow private BlockEntity @Nullable [] globalBlockEntities;

    @Inject(method = "getGlobalBlockEntities", at = @At("HEAD"), cancellable = true)
    private void notRenderBeacons(CallbackInfoReturnable<BlockEntity[]> cir) {
        if(globalBlockEntities != null) {
            cir.setReturnValue(Arrays.stream(globalBlockEntities)
                    .filter(entity -> !(entity instanceof BeaconBlockEntity))
                    .toArray(BlockEntity[]::new));
        }
    }
}
