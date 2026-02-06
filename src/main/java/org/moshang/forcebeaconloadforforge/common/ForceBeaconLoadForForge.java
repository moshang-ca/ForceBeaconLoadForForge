package org.moshang.forcebeaconloadforforge.common;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;
import org.slf4j.Logger;

@SuppressWarnings("removal")
@Mod(ForceBeaconLoadForForge.MODID)
public class ForceBeaconLoadForForge {
    public static final String MODID = "forcebeaconloadforforge";
    private static final Logger LOGGER = LogUtils.getLogger();
    private  static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ForceBeaconLoadForForge.MODID, "beacon_data_channel"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    public ForceBeaconLoadForForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        MinecraftForge.EVENT_BUS.addListener(ForceBeaconLoadForForge::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ForceBeaconLoadForForge::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(ForceBeaconLoadForForge::onServerTick);

        registerPacket();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            sendBeaconDataToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            sendBeaconDataToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
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
    }

    public static void registerPacket() {
        INSTANCE.registerMessage(
                0, CompoundTag.class,
                (tag, buf) -> buf.writeNbt(tag),
                FriendlyByteBuf::readNbt,
                (tag, ctx) -> {
                    ctx.get().enqueueWork( () -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForceBeaconLoadClient.handlePacket(tag)));
                    ctx.get().setPacketHandled(true);
        });
    }

    public static LevelBeaconData getBeaconData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                LevelBeaconData::new,
                () -> new LevelBeaconData(new CompoundTag()),
                "beacons"
        );
    }

    public static void sendBeaconDataToPlayer(ServerPlayer player, boolean check) {
        if(player.level() instanceof ServerLevel serverLevel) {
            ResourceLocation worldId = serverLevel.dimension().location();
            LevelBeaconData beaconData = getBeaconData(serverLevel);
            if(check) beaconData.check(serverLevel);
            CompoundTag nbt = beaconData.save(new CompoundTag());
            nbt.putString("level", worldId.toString());
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), nbt);
        }
    }

    public static void sendToAllPlayers(ServerLevel level) {
        getBeaconData(level).check(level);
        level.players().forEach( player -> sendBeaconDataToPlayer(player,false));
    }

    public static boolean shouldApplyEffectToPlayer(BeaconBlockEntity beacon, ServerPlayer player) {
        int level = beacon.levels;
        BlockPos beaconPos = beacon.getBlockPos();
        var distance = beaconPos.distToCenterSqr(player.getX(), beaconPos.getY(), player.getZ());
        System.out.printf("distance: %f\n", distance);

        return distance < Math.pow(level * 50 + 100, 2);
    }

    public static void addEffectToPlayer(BeaconBlockEntity beacon, ServerPlayer player) {
        BlockPos pos = BlockPos.containing(player.position());
        BeaconBlockEntity.applyEffects(player.level(), pos, beacon.levels, beacon.primaryPower, beacon.secondaryPower);
    }
}
