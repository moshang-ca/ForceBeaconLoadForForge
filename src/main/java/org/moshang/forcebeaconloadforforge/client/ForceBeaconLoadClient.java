package org.moshang.forcebeaconloadforforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.moshang.forcebeaconloadforforge.common.LevelBeaconData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ForceBeaconLoadClient {
    private static final Map<ResourceLocation, Map<BlockPos, BeaconBlockEntity>> beaconMaps = new ConcurrentHashMap<>();

    public static void handlePacket(CompoundTag nbt) {
        if(nbt == null) return;

        String levelStr = nbt.getString("level");
        if(levelStr.isEmpty()) return;
        ResourceLocation level = ResourceLocation.parse(levelStr);

        ClientLevel clientLevel = Minecraft.getInstance().level;
        HolderLookup.Provider provider = clientLevel != null ? clientLevel.registryAccess() : RegistryAccess.EMPTY;
        Map<BlockPos, BeaconBlockEntity> beacons = new LevelBeaconData(nbt, provider).getBeacons();

        beaconMaps.put(level, beacons);
        setLevel(Minecraft.getInstance().level);
    }

    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        beaconMaps.clear();
    }

    public static void setLevel(ClientLevel level) {
        beaconMaps.forEach((dim, map) -> {
            if(dim.equals(level.dimension().location())) {
                map.forEach((pos, beacon) -> beacon.setLevel(level));
            }
        });
    }

    public static List<BeaconBlockEntity> getBeacons(Level level) {
        if(level == null) return List.of();
        var map = beaconMaps.getOrDefault(level.dimension().location(), new HashMap<>());
        return map.values().stream().filter(entity -> entity.getLevel() == level).toList();
    }
}
