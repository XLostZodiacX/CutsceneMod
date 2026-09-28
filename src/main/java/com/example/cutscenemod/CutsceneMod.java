package com.example.cutscenemod;

import com.example.cutscenemod.network.ModNetworking;
import com.example.cutscenemod.server.command.CutsceneCommand;
import com.example.cutscenemod.server.command.SongCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entry point ของมอด
 *
 * คำสั่งที่มอดนี้เพิ่มให้ (ต้องมีสิทธิ์ op / permission level 2 ขึ้นไป):
 *
 *   /cutscene open <ชื่อไฟล์ไม่มีนามสกุล> <target selector>
 *   /cutscene stop <target selector>                         (bonus: บังคับปิดคัตซีน)
 *
 *   /song play <ชื่อไฟล์.mp4> <target selector> <loop: true|false>
 *   /song stop <target selector>
 *
 * ไฟล์วิดีโอ/เพลงต้องวางไว้ที่เครื่อง client เอง (อ่านจากโฟลเดอร์ config ของมอด)
 *   .minecraft/config/cutscenemod/cutscenes/<ชื่อไฟล์>.mp4
 *   .minecraft/config/cutscenemod/songs/<ชื่อไฟล์.mp4>
 */
@Mod(CutsceneMod.MODID)
public class CutsceneMod {

    public static final String MODID = "cutscenemod";

    public CutsceneMod() {
        // เตรียม network channel สำหรับส่งแพ็กเก็ตจาก server -> client
        ModNetworking.register();

        // คำสั่งฝั่ง server ต้อง subscribe บน Forge event bus (ไม่ใช่ mod bus)
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CutsceneCommand.register(event.getDispatcher());
        SongCommand.register(event.getDispatcher());
    }

    /**
     * เผื่อกรณีเซิร์ฟเวอร์ปิดตัว ระหว่างมีคัตซีน/เพลงเล่นค้างอยู่ที่ client (ปัจจุบันไม่บังคับใช้
     * เพราะ handler ฝั่ง client จะเคลียร์ตัวเองเมื่อ disconnect อยู่แล้วผ่าน ClientPlayerNetworkEvent)
     */
    public static void noop(MinecraftServer server) {
    }
}
