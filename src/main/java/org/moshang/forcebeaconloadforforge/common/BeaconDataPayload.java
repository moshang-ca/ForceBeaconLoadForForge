package org.moshang.forcebeaconloadforforge.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.moshang.forcebeaconloadforforge.client.ForceBeaconLoadClient;

public record BeaconDataPayload(CompoundTag tag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BeaconDataPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ForceBeaconLoadForForge.MODID, "beacon_data"));

    public static final StreamCodec<FriendlyByteBuf, BeaconDataPayload> STREAM_CODEC = StreamCodec.ofMember(
            (paylad, buf) -> buf.writeNbt(paylad.tag),
                buf -> new BeaconDataPayload(buf.readNbt())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeaconDataPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if(ctx.flow().isClientbound()) {
                ForceBeaconLoadClient.handlePacket(payload.tag());
            }
        });
    }
}
