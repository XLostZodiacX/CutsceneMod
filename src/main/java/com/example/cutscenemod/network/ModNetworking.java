package com.example.cutscenemod.network;

import com.example.cutscenemod.CutsceneMod;
import com.example.cutscenemod.network.packet.OpenCutscenePacket;
import com.example.cutscenemod.network.packet.PlaySongPacket;
import com.example.cutscenemod.network.packet.StopCutscenePacket;
import com.example.cutscenemod.network.packet.StopSongPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CutsceneMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    public static void register() {
        CHANNEL.registerMessage(nextId(), OpenCutscenePacket.class,
                OpenCutscenePacket::encode, OpenCutscenePacket::decode, OpenCutscenePacket::handle);

        CHANNEL.registerMessage(nextId(), StopCutscenePacket.class,
                StopCutscenePacket::encode, StopCutscenePacket::decode, StopCutscenePacket::handle);

        CHANNEL.registerMessage(nextId(), PlaySongPacket.class,
                PlaySongPacket::encode, PlaySongPacket::decode, PlaySongPacket::handle);

        CHANNEL.registerMessage(nextId(), StopSongPacket.class,
                StopSongPacket::encode, StopSongPacket::decode, StopSongPacket::handle);
    }
}
