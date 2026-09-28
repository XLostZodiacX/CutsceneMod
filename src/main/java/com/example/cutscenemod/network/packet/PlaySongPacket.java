package com.example.cutscenemod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaySongPacket {

    private final String songFile;
    private final boolean loop;

    public PlaySongPacket(String songFile, boolean loop) {
        this.songFile = songFile;
        this.loop = loop;
    }

    public static void encode(PlaySongPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.songFile);
        buf.writeBoolean(packet.loop);
    }

    public static PlaySongPacket decode(FriendlyByteBuf buf) {
        return new PlaySongPacket(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(PlaySongPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.cutscenemod.client.ClientSongPlayer.play(packet.songFile, packet.loop))
        );
        ctx.setPacketHandled(true);
    }
}
