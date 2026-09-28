package com.example.cutscenemod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * เล่นวิดีโอคัตซีนฝั่ง client ด้วย vlcj แบบ direct rendering (ไม่ใช้ Swing เลย):
 * factory -> EmbeddedMediaPlayer -> CallbackVideoSurface(เฟรมถูกส่งเข้า RenderCallback)
 *
 * decode รันบน thread ของ libVLC เราแค่ copy ไบต์ล่าสุดเก็บไว้ แล้ว render thread ของ Minecraft
 * มาหยิบไปอัปโหลดเป็น texture (ดู pollFrameForRender)
 */
public class ClientCutscenePlayer {

    private static EmbeddedMediaPlayer mediaPlayer;

    private static final ReentrantLock frameLock = new ReentrantLock();
    private static ByteBuffer latestFrame;
    private static int frameWidth;
    private static int frameHeight;
    private static volatile boolean frameDirty = false;

    private static final AtomicBoolean active = new AtomicBoolean(false);

    public static boolean isActive() {
        return active.get();
    }

    public static synchronized void open(String cutsceneName) {
        File file = ModPaths.cutsceneFile(cutsceneName);
        if (!file.exists()) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("§c[cutscenemod] ไม่พบไฟล์คัตซีน: " + file.getAbsolutePath()));
            return;
        }

        stop(false);

        try {
            MediaPlayerFactory factory = VlcSupport.videoFactory();
            mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

            BufferFormatCallback bufferFormatCallback = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                    return new RV32BufferFormat(sourceWidth, sourceHeight);
                }

                @Override
                public void allocatedBuffers(ByteBuffer[] buffers) {
                    // no-op
                }
            };

            RenderCallback renderCallback = new RenderCallback() {
                @Override
                public void display(MediaPlayer mp, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
                    frameLock.lock();
                    try {
                        ByteBuffer src = nativeBuffers[0];
                        int size = src.capacity();
                        if (latestFrame == null || latestFrame.capacity() != size) {
                            latestFrame = ByteBuffer.allocateDirect(size);
                        }
                        frameWidth = bufferFormat.getWidth();
                        frameHeight = bufferFormat.getHeight();

                        src.rewind();
                        latestFrame.clear();
                        latestFrame.put(src);
                        latestFrame.flip();
                        frameDirty = true;
                    } finally {
                        frameLock.unlock();
                    }
                }
            };

            mediaPlayer.videoSurface().set(new CallbackVideoSurface(
                    bufferFormatCallback, renderCallback, true, VideoSurfaceAdapters.getVideoSurfaceAdapter()));

            mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void finished(MediaPlayer mp) {
                    // ห้ามสั่ง stop/release ตรงๆ ใน event thread ของ libVLC (deadlock) -> ส่งไปทำบน client thread
                    Minecraft.getInstance().execute(() -> stop(true));
                }

                @Override
                public void error(MediaPlayer mp) {
                    VlcSupport.report("VLC เล่นไฟล์คัตซีนไม่ได้: " + file.getName(), null);
                    Minecraft.getInstance().execute(() -> stop(false));
                }
            });

            boolean started = mediaPlayer.media().play(file.getAbsolutePath());
            if (!started) {
                throw new IllegalStateException("VLC ปฏิเสธการเล่นไฟล์: " + file.getName());
            }
            active.set(true);
        } catch (Throwable t) {
            VlcSupport.report("เปิดคัตซีนไม่สำเร็จ", t);
            stop(false);
        }
    }

    public static synchronized void stop(boolean finishedNaturally) {
        active.set(false);
        frameDirty = false;

        if (mediaPlayer != null) {
            try {
                mediaPlayer.controls().stop();
                mediaPlayer.release();
            } catch (Throwable t) {
                VlcSupport.LOGGER.warn("[cutscenemod] error while stopping cutscene", t);
            }
            mediaPlayer = null;
        }
    }

    /** เรียกจาก render thread เท่านั้น */
    public static CutsceneVideoTexture pollFrameForRender(CutsceneVideoTexture texture) {
        if (!active.get()) return null;
        if (!frameDirty) return texture.isReady() ? texture : null;

        frameLock.lock();
        try {
            if (latestFrame != null) {
                texture.uploadFrame(latestFrame, frameWidth, frameHeight);
                frameDirty = false;
            }
        } finally {
            frameLock.unlock();
        }
        return texture;
    }
}
