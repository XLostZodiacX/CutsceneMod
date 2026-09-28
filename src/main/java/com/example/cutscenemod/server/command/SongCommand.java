package com.example.cutscenemod.server.command;

import com.example.cutscenemod.network.ModNetworking;
import com.example.cutscenemod.network.packet.PlaySongPacket;
import com.example.cutscenemod.network.packet.StopSongPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

/**
 * /song play <file.mp4> <targets> <loop>
 * /song stop <targets>
 */
public class SongCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("song")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("play")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("loop", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    String file = StringArgumentType.getString(ctx, "file");
                                                    boolean loop = BoolArgumentType.getBool(ctx, "loop");
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                                    for (ServerPlayer player : targets) {
                                                        ModNetworking.CHANNEL.send(
                                                                PacketDistributor.PLAYER.with(() -> player),
                                                                new PlaySongPacket(file, loop)
                                                        );
                                                    }

                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "เล่นเพลง '" + file + "' (loop=" + loop + ") ให้ " + targets.size() + " ผู้เล่นแล้ว"
                                                    ), true);
                                                    return targets.size();
                                                })))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer player : targets) {
                                        ModNetworking.CHANNEL.send(
                                                PacketDistributor.PLAYER.with(() -> player),
                                                new StopSongPacket()
                                        );
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "หยุดเพลงให้ " + targets.size() + " ผู้เล่นแล้ว"
                                    ), true);
                                    return targets.size();
                                })))
        );
    }
}
