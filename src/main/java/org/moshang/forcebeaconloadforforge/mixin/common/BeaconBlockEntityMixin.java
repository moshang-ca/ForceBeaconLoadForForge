package org.moshang.forcebeaconloadforforge.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import org.moshang.forcebeaconloadforforge.common.ForceBeaconLoadForForge;
import org.moshang.forcebeaconloadforforge.common.api.HasLevelShrink;
import org.moshang.forcebeaconloadforforge.common.api.IsLevelValid;
import org.moshang.forcebeaconloadforforge.common.api.UpdateLevelShrink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin implements IsLevelValid, HasLevelShrink, UpdateLevelShrink {
    @Shadow public int levels;
    @Unique boolean forcebeaconloadforforge$isLevelValid = false;
    @Unique int forcebeaconloadforforge$levelShrink = 0;

    @Unique
    private int forcebeaconloadforforge$calLevelShrink() {
        BeaconBlockEntity entity = (BeaconBlockEntity) (Object) this;
        Level level = entity.getLevel();
        if(level == null) return 0;
        BlockPos pos = entity.getBlockPos();
        for(int i = 0; i < this.levels; i++) {
            BlockPos detectPos = new BlockPos(pos.getX(), pos.getY() + i + 1, pos.getZ());
            BlockState state = level.getBlockState(detectPos);
            boolean shrink = state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES);
            if(!shrink) return i;
        }
        return levels;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void invalidateBeaconPower(Level pLevel, BlockPos pPos, BlockState pState, BeaconBlockEntity pBlockEntity, CallbackInfo ci) {
        if(!pLevel.isClientSide && pBlockEntity.levels <= 0) {
            pBlockEntity.primaryPower = null;
            pBlockEntity.secondaryPower = null;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void updateBeaconData(Level pLevel, BlockPos pPos, BlockState pState, BeaconBlockEntity pBlockEntity, CallbackInfo ci){
        if(pLevel instanceof ServerLevel serverLevel) {
            ForceBeaconLoadForForge.getBeaconData(serverLevel).add(serverLevel, pBlockEntity);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;updateBase(Lnet/minecraft/world/level/Level;III)I"))
    private static void onLevelUpdate(Level pLevel, BlockPos pPos, BlockState pState, BeaconBlockEntity pBlockEntity, CallbackInfo ci) {
        ((UpdateLevelShrink)pBlockEntity).forcebeaconloadforforge$updateLevelShrink();
        ((IsLevelValid)pBlockEntity).setForcebeaconloadforforge$isLevelValid(true);
    }

    @Inject(method = "updateBase", at = @At("RETURN"))
    private static void updateBeaconData(Level pLevel, int pX, int pY, int pZ, CallbackInfoReturnable<Integer> cir) {
        if(pLevel instanceof ServerLevel serverLevel) {
            ForceBeaconLoadForForge.getBeaconData(serverLevel).invalidate();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V"))
    private static void notAddPlayerEffects(Level player, BlockPos player1, int d0, Holder<MobEffect> i, Holder<MobEffect> j) {}

    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private static AABB changePlayerEffectDistance(AABB instance, double pValue) {
        return instance.inflate(pValue);
    }

    @Override
    public int forcebeaconloadforforge$levelShrink() {
        return forcebeaconloadforforge$levelShrink;
    }

    @Override
    public void setForcebeaconloadforforge$levelShrink(int level) {
        this.forcebeaconloadforforge$levelShrink = level;
    }

    @Override
    public boolean forcebeaconloadforforge$isLevelValid() {
        return forcebeaconloadforforge$isLevelValid;
    }

    @Override
    public void setForcebeaconloadforforge$isLevelValid(boolean isValid) {
        this.forcebeaconloadforforge$isLevelValid = isValid;
    }

    @Override
    public void forcebeaconloadforforge$updateLevelShrink() {
        forcebeaconloadforforge$levelShrink = forcebeaconloadforforge$calLevelShrink();
    }

    @Inject(method = "saveAdditional", at = @At("HEAD"))
    void writeExtra(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putInt("levelShrink", forcebeaconloadforforge$levelShrink);
    }

    @Inject(method = "loadAdditional", at = @At("HEAD"))
    void readExtra(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        forcebeaconloadforforge$levelShrink = tag.getInt("levelShrink");
    }
}
