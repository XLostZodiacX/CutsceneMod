package com.example.cutscenemod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.io.File;

/**
 * เล่นเพลงแบบเสียงอย่างเดียว ใช้ factory ที่ปิดวิดีโอ (--no-video) เพื่อไม่ให้ VLC เด้งหน้าต่างวิดีโอ
 * ขึ้นมาเวลาเล่นไฟล์ .mp4
 */
public class ClientSongPlayer {

    private static MediaPlayer player;
    private static String currentPath;
    private static boolean loop;

    public static synchronized void play(String songFileName, boolean shouldLoop) {
        File file = ModPaths.songFile(songFileName);
        if (!file.exists()) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("§c[cutscenemod] ไม่พบไฟล์เพลง: " + file.getAbsolutePath()));
            return;
        }

        stop();

        try {
            currentPath = file.getAbsolutePath();
            loop = shouldLoop;
            player = VlcSupport.audioFactory().mediaPlayers().newMediaPlayer();

            player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void finished(MediaPlayer mp) {
                    if (loop && currentPath != null) {
                        // เล่นซ้ำ: ต้องสั่งผ่าน submit (คนละ thread กับ event thread ของ libVLC)
                        String path = currentPath;
                        mp.submit(() -> mp.media().play(path));
                    } else {
                        Minecraft.getInstance().execute(ClientSongPlayer::stop);
                    }
                }

                @Override
                public void error(MediaPlayer mp) {
                    VlcSupport.report("VLC เล่นไฟล์เพลงไม่ได้: " + file.getName(), null);
                    Minecraft.getInstance().execute(ClientSongPlayer::stop);
                }
            });

            boolean started = player.media().play(currentPath);
            if (!started) {
                throw new IllegalStateException("VLC ปฏิเสธการเล่นไฟล์: " + file.getName());
            }
        } catch (Throwable t) {
            VlcSupport.report("เล่นเพลงไม่สำเร็จ", t);
            stop();
        }
    }

    public static synchronized void stop() {
        currentPath = null;
        if (player != null) {
            try {
                player.controls().stop();
                player.release();
            } catch (Throwable t) {
                VlcSupport.LOGGER.warn("[cutscenemod] error while stopping song", t);
            }
            player = null;
        }
    }
}
