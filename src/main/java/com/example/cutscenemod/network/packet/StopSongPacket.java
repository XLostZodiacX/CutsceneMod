package com.example.cutscenemod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopSongPacket {

    public StopSongPacket() {
    }

    public static void encode(StopSongPacket packet, FriendlyByteBuf buf) {
        // no payload
    }

    public static StopSongPacket decode(FriendlyByteBuf buf) {
        return new StopSongPacket();
    }

    public static void handle(StopSongPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.cutscenemod.client.ClientSongPlayer.stop())
        );
        ctx.setPacketHandled(true);
    }
}
