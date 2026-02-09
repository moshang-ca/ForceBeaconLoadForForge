package org.moshang.forcebeaconloadforforge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class SodiumWorldRendererMixin {
    @Shadow
    private static void renderBlockEntity(PoseStack matrices, RenderBuffers bufferBuilders,
                                          Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                          float tickDelta, MultiBufferSource.BufferSource immediate,
                                          double x, double y, double z,
                                          BlockEntityRenderDispatcher dispatcher, BlockEntity entity) {}

    @Shadow private ClientLevel world;

    @Inject(method = "renderGlobalBlockEntities", at = @At("HEAD"))
    void renderAllCachedBeacons(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double x, double y, double z, BlockEntityRenderDispatcher blockEntityRenderer, CallbackInfo ci) {
        ForceBeaconLoadClient.getBeacons(world).forEach(
                entity -> renderBlockEntity(
                        matrices, bufferBuilders, blockBreakingProgressions,
                        tickDelta, immediate, x, y, z, blockEntityRenderer, entity
                )
        );
    }
}
