package org.moshang.forcebeaconloadforforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;

@Mod(ForceBeaconLoadForForge.MODID)
public class ForceBeaconLoadForForge {
    public static final String MODID = "forcebeaconloadforforge";

    public ForceBeaconLoadForForge(IEventBus bus) {
        NeoForge.EVENT_BUS.register(this);
        bus.addListener(this::registerPacket);

        if(FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ForceBeaconLoadClient::onClientPlayerLogin);
        }

    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            sendBeaconDataToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            sendBeaconDataToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for(ServerLevel level : event.getServer().getAllLevels()) {
            LevelBeaconData beaconData = getBeaconData(level);
            beaconData.tick(level);

            if(level.getGameTime() % 80 == 0) {
                for(ServerPlayer player : level.players()) {
                    for(var beacon : beaconData.getBeacons().values()) {
                        if(shouldApplyEffectToPlayer(beacon, player)) {
                            addEffectToPlayer(beacon, player);
                        }
                    }
                }
            }
        }
    }

    public void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                BeaconDataPayload.TYPE,
                BeaconDataPayload.STREAM_CODEC,
                BeaconDataPayload::handle
        );
    }

    public static LevelBeaconData getBeaconData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new LevelBeaconData(new CompoundTag(), level.registryAccess()),
                        LevelBeaconData::new
                ),
                "beacons"
        );
    }

    public static void sendBeaconDataToPlayer(ServerPlayer player, boolean check) {
        if(player.level() instanceof ServerLevel serverLevel) {
            ResourceLocation worldId = serverLevel.dimension().location();
            LevelBeaconData beaconData = getBeaconData(serverLevel);
            if(check) beaconData.check(serverLevel);
            CompoundTag nbt = beaconData.save(new CompoundTag(), serverLevel.registryAccess());
            nbt.putString("level", worldId.toString());
            PacketDistributor.sendToPlayer(player, new BeaconDataPayload(nbt));
        }
    }

    public static void sendToAllPlayers(ServerLevel level) {
        getBeaconData(level).check(level);
        level.players().forEach( player -> sendBeaconDataToPlayer(player,false));
    }

    public static boolean shouldApplyEffectToPlayer(BeaconBlockEntity beacon, ServerPlayer player) {
        int level = beacon.levels;
        if(level <= 0) return false;
        BlockPos beaconPos = beacon.getBlockPos();
        var distance = beaconPos.distToCenterSqr(player.getX(), beaconPos.getY(), player.getZ());

        return distance < Math.pow(level * 50 + 100, 2);
    }

    public static void addEffectToPlayer(BeaconBlockEntity beacon, ServerPlayer player) {
        BlockPos pos = BlockPos.containing(player.position());
        BeaconBlockEntity.applyEffects(player.level(), pos, beacon.levels, beacon.primaryPower, beacon.secondaryPower);
    }
}
