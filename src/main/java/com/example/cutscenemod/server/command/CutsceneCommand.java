package com.example.cutscenemod.server.command;

import com.example.cutscenemod.network.ModNetworking;
import com.example.cutscenemod.network.packet.OpenCutscenePacket;
import com.example.cutscenemod.network.packet.StopCutscenePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

/**
 * /cutscene open <file> <targets>
 * /cutscene stop <targets>
 */
public class CutsceneCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cutscene")
                .requires(src -> src.hasPermission(2)) // ต้องเป็น op
                .then(Commands.literal("open")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> {
                                            String file = StringArgumentType.getString(ctx, "file");
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                            for (ServerPlayer player : targets) {
                                                ModNetworking.CHANNEL.send(
                                                        PacketDistributor.PLAYER.with(() -> player),
                                                        new OpenCutscenePacket(file)
                                                );
                                            }

                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "เปิดคัตซีน '" + file + "' ให้ " + targets.size() + " ผู้เล่นแล้ว"
                                            ), true);
                                            return targets.size();
                                        }))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer player : targets) {
                                        ModNetworking.CHANNEL.send(
                                                PacketDistributor.PLAYER.with(() -> player),
                                                new StopCutscenePacket()
                                        );
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "บังคับปิดคัตซีนให้ " + targets.size() + " ผู้เล่นแล้ว"
                                    ), true);
                                    return targets.size();
                                })))
        );
    }
}
