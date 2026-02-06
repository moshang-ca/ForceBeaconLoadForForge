package org.moshang.forcebeaconloadforforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.moshang.forcebeaconloadforforge.common.api.IsLevelValid;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LevelBeaconData extends SavedData {
    private final Map<BlockPos, BeaconBlockEntity> beacons;
    private long lastSendTime = 0L;
    public LevelBeaconData(CompoundTag nbt) {
        beacons = BeaconInfoUtils.readBeacons(nbt).stream().collect(Collectors.toMap(BeaconBlockEntity::getBlockPos, Function.identity()));
    }

    public void tick(ServerLevel level) {
        if(level.getGameTime() - lastSendTime > 100) {
            ForceBeaconLoadForForge.sendToAllPlayers(level);
            lastSendTime = level.getGameTime();
        }
    }

    public void add(ServerLevel level, BeaconBlockEntity beacon) {
        if(beacons.get(beacon.getBlockPos()) != beacon && BeaconInfoUtils.isValid(beacon)) {
            beacons.put(beacon.getBlockPos(), beacon);
            setDirty();
            ForceBeaconLoadForForge.sendToAllPlayers(level);
            lastSendTime = level.getGameTime();
        }
    }

    public void remove(ServerLevel level, BlockPos pos) {
        if(beacons.remove(pos) != null) {
            setDirty();
            ForceBeaconLoadForForge.sendToAllPlayers(level);
            lastSendTime = level.getGameTime();
        }
    }

    public void check(Level level) {
        beacons.entrySet().removeIf( entry -> {
            if(!level.isLoaded(entry.getKey())) return false;
            else return level.getBlockEntity(entry.getKey()) != entry.getValue();
        } );
        // Here are some conditional compiling or debugging codes in vanilla version.
    }

    public boolean isEmpty() {
        return beacons.isEmpty();
    }

    public void invalidate() {
        this.lastSendTime = 0L;
    }
    public Map<BlockPos, BeaconBlockEntity> getBeacons() { return Collections.unmodifiableMap(beacons); }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        BeaconInfoUtils.saveBeacons(beacons.values(), compoundTag);
        return compoundTag;
    }

    static class BeaconInfoUtils {
        public static boolean isValid(BeaconBlockEntity beacon) {
            if(beacon instanceof IsLevelValid) {
                return ((IsLevelValid) beacon).forcebeaconloadforforge$isLevelValid();
            }
            return false;
        }

        private static void saveBeacons(Collection<BeaconBlockEntity> beacons, CompoundTag nbt) {
            final var listTag = new ListTag();
            for(var beacon : beacons) {
                var tag = new CompoundTag();
                beacon.saveAdditional(tag);
                tag.putInt("x", beacon.getBlockPos().getX());
                tag.putInt("y", beacon.getBlockPos().getY());
                tag.putInt("z", beacon.getBlockPos().getZ());
                var sections = new ListTag();
                for(var section : beacon.beamSections) {
                    var secNbt = new CompoundTag();
                    secNbt.putInt("h", section.height);
                    secNbt.putFloat("c0", section.getColor()[0]);
                    secNbt.putFloat("c1", section.getColor()[1]);
                    secNbt.putFloat("c2", section.getColor()[2]);
                    sections.add(secNbt);
                }
                tag.put("sections", sections);
                listTag.add(tag);
            }
            nbt.put("beacons", listTag);
        }

        private static List<BeaconBlockEntity> readBeacons(CompoundTag nbt) {
            final var listTag = nbt.getList("beacons", 10);
            return IntStream.range(0, listTag.size())
                    .mapToObj( i -> {
                        var tag = listTag.getCompound(i);
                        var pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                        var segNbt = tag.getList("sections", 10);
                        var sections = IntStream.range(0, segNbt.size())
                                .mapToObj( it -> {
                                    var segTag = segNbt.getCompound(it);
                                    int height = segTag.getInt("h");
                                    var beamSection = new BeaconBlockEntity.BeaconBeamSection(
                                            new float[]{segTag.getFloat("c0"), segTag.getFloat("c1"), segTag.getFloat("c2")});
                                    beamSection.height = height;
                                    return beamSection;
                                }).toList();
                        BeaconBlockEntity beacon = new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());
                        beacon.load(tag);
                        beacon.levels = tag.getInt("Levels");
                        beacon.beamSections = sections;
                        return beacon;
                    }).toList();
        }
    }
}
