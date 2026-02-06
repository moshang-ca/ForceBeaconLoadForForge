package org.moshang.forcebeaconloadforforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("removal")
public class ForceBeaconLoadClient {
    private static final Map<ResourceLocation, Map<BlockPos, BeaconBlockEntity>> beaconMaps = new ConcurrentHashMap<>();

    public static void handlePacket(CompoundTag nbt) {
        if(nbt == null) return;

        String levelStr = nbt.getString("level");
        if(levelStr.isEmpty()) return;
        ResourceLocation level = new ResourceLocation(levelStr);

        ListTag listTag = nbt.getList("beacons", 10);
        Map<BlockPos, BeaconBlockEntity> beacons = new HashMap<>();

        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag tag = listTag.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BeaconBlockEntity beacon = new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());
            beacon.load(tag);
            beacons.put(pos, beacon);
        }
        beaconMaps.put(level, beacons);

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if(clientLevel != null && clientLevel.dimension().location().equals(level)) {
            beacons.values().forEach( beacon -> beacon.setLevel(clientLevel) );
        }
    }
}
