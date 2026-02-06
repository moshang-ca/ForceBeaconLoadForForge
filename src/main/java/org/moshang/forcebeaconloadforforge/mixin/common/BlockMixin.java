package org.moshang.forcebeaconloadforforge.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.forcebeaconloadforforge.common.ForceBeaconLoadForForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"))
    private static void updateBeaconData(BlockState pOldState, BlockState pNewState, LevelAccessor pLevel, BlockPos pPos, int pFlags, CallbackInfo ci) {
        if(pLevel instanceof ServerLevel serverLevel) {
            if(pOldState.getBlock() == Blocks.BEACON) {
                ForceBeaconLoadForForge.getBeaconData(serverLevel).remove(serverLevel, pPos);
            }
            if(pNewState.getBlock() == Blocks.BEACON || pOldState.getBlock() == Blocks.BEACON) {
                ForceBeaconLoadForForge.getBeaconData(serverLevel).invalidate();
            }
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void updateBeaconDate(LevelAccessor pLevel, BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if(pLevel instanceof ServerLevel serverLevel) {
            ForceBeaconLoadForForge.getBeaconData(serverLevel).invalidate();
        }
    }
}
