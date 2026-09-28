package com.example.cutscenemod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenCutscenePacket {

    private final String cutsceneName;

    public OpenCutscenePacket(String cutsceneName) {
        this.cutsceneName = cutsceneName;
    }

    public static void encode(OpenCutscenePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.cutsceneName);
    }

    public static OpenCutscenePacket decode(FriendlyByteBuf buf) {
        return new OpenCutscenePacket(buf.readUtf());
    }

    public static void handle(OpenCutscenePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.cutscenemod.client.ClientCutscenePlayer.open(packet.cutsceneName))
        );
        ctx.setPacketHandled(true);
    }
}
