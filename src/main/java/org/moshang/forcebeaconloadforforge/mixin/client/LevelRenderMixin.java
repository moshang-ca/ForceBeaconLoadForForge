package org.moshang.forcebeaconloadforforge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(LevelRenderer.class)
public class LevelRenderMixin {
    @Shadow @Final private Set<BlockEntity> globalBlockEntities;
    @Shadow private ClientLevel level;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderAllBeacons(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime,
                                  boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer,
                                  LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        globalBlockEntities.removeIf(entity -> entity instanceof BeaconBlockEntity);
        globalBlockEntities.addAll(ForceBeaconLoadClient.getBeacons(level));
    }
}
