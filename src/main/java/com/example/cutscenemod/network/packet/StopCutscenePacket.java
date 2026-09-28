package com.example.cutscenemod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopCutscenePacket {

    public StopCutscenePacket() {
    }

    public static void encode(StopCutscenePacket packet, FriendlyByteBuf buf) {
        // no payload
    }

    public static StopCutscenePacket decode(FriendlyByteBuf buf) {
        return new StopCutscenePacket();
    }

    public static void handle(StopCutscenePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.cutscenemod.client.ClientCutscenePlayer.stop(false))
        );
        ctx.setPacketHandled(true);
    }
}
