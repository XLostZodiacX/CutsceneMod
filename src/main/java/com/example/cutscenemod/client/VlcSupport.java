package com.example.cutscenemod.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.io.File;
import java.nio.file.Files;

/**
 * ตัวช่วยกลางสำหรับ VLC: หา libVLC, สร้าง factory, และแจ้ง error ให้เห็นในแชท + log
 * (เดิม exception ถูกกลืนเงียบๆ ทำให้ผู้เล่นไม่รู้ว่าทำไมไม่เล่น)
 *
 * ถ้า VLC ติดตั้งในโฟลเดอร์แปลกๆ ที่หาไม่เจออัตโนมัติ ให้สร้างไฟล์
 *   .minecraft/config/cutscenemod/vlc_path.txt
 * ใส่พาธโฟลเดอร์ที่มี libvlc.dll ไว้บรรทัดเดียว เช่น  C:\Program Files\VideoLAN\VLC
 */
public class VlcSupport {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean nativeFound = false;
    private static Throwable lastDiscoveryError = null;
    private static MediaPlayerFactory videoFactory;
    private static MediaPlayerFactory audioFactory;

    private static synchronized boolean ensureNative() {
        if (nativeFound) return true;

        try {
            File pathFile = FMLPaths.CONFIGDIR.get().resolve("cutscenemod").resolve("vlc_path.txt").toFile();
            if (pathFile.exists()) {
                String custom = Files.readString(pathFile.toPath()).trim();
                if (!custom.isEmpty()) {
                    System.setProperty("jna.library.path", custom);
                    LOGGER.info("[cutscenemod] using custom VLC path: {}", custom);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[cutscenemod] failed reading vlc_path.txt", t);
        }

        lastDiscoveryError = null;
        try {
            nativeFound = new NativeDiscovery().discover();
        } catch (Throwable t) {
            // เก็บ error จริงไว้ (เช่น NoClassDefFoundError = ไลบรารีไม่ครบใน jar) ไม่ใช่แค่บอกว่า "ไม่พบ VLC"
            LOGGER.error("[cutscenemod] libVLC discovery threw", t);
            lastDiscoveryError = t;
            nativeFound = false;
        }
        LOGGER.info("[cutscenemod] libVLC found = {}", nativeFound);
        return nativeFound;
    }

    private static IllegalStateException discoveryFailure() {
        if (lastDiscoveryError != null) {
            return new IllegalStateException(
                    "เรียก libVLC ไม่สำเร็จ (ไม่ใช่แค่หาไม่เจอ): " + lastDiscoveryError, lastDiscoveryError);
        }
        return new IllegalStateException(
                "ไม่พบ libVLC - ต้องเป็น VLC 64-bit และพาธถูกต้อง (config/cutscenemod/vlc_path.txt)");
    }

    /** factory สำหรับวิดีโอ (ใช้กับ callback video surface) */
    public static synchronized MediaPlayerFactory videoFactory() {
        if (!ensureNative()) {
            throw discoveryFailure();
        }
        if (videoFactory == null) {
            videoFactory = new MediaPlayerFactory("--quiet");
        }
        return videoFactory;
    }

    /** factory สำหรับเพลง: ปิดวิดีโอ กันไม่ให้ VLC เด้งหน้าต่างวิดีโอของตัวเองเวลาเล่นไฟล์ mp4 */
    public static synchronized MediaPlayerFactory audioFactory() {
        if (!ensureNative()) {
            throw discoveryFailure();
        }
        if (audioFactory == null) {
            audioFactory = new MediaPlayerFactory("--quiet", "--no-video");
        }
        return audioFactory;
    }

    /** แจ้ง error ในแชทของผู้เล่น (ทำงานบน client thread) และลง log พร้อม stacktrace */
    public static void report(String message, Throwable t) {
        LOGGER.error("[cutscenemod] " + message, t);
        String detail = (t == null) ? "" : " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")";
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().gui != null) {
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("§c[cutscenemod] " + message + detail));
            }
        });
    }
}
