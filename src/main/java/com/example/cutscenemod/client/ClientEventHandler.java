package com.example.cutscenemod.client;

import com.example.cutscenemod.CutsceneMod;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * รวม logic ฝั่ง client ทั้งหมดที่เกี่ยวกับการแสดงคัตซีน:
 *  - ซ่อน HUD ทั้งหมดระหว่างเล่นคัตซีน (ยกเลิก RenderGuiEvent.Pre)
 *  - วาดวิดีโอเต็มจอจากเฟรมล่าสุดของ ClientCutscenePlayer
 *  - ล็อกการเคลื่อนที่ของผู้เล่น (MovementInputUpdateEvent) และบล็อกการโต้ตอบ/เปิดหน้าจออื่นๆ
 *  - ตรวจจับการกดค้าง Space 2 วินาทีเพื่อข้ามคัตซีน พร้อมวาด UI แจ้งเตือนมุมล่างขวา
 */
@Mod.EventBusSubscriber(modid = CutsceneMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final int SKIP_HOLD_TICKS = 40; // 2 วินาที (20 tick/วิ)
    private static int holdTicks = 0;

    private static final CutsceneVideoTexture VIDEO_TEXTURE = new CutsceneVideoTexture();

    // ==================== TICK: จัดการการกดค้าง Space เพื่อข้าม ====================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ClientCutscenePlayer.isActive()) {
            holdTicks = 0;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        boolean spaceDown = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_SPACE);

        if (spaceDown) {
            holdTicks++;
            if (holdTicks >= SKIP_HOLD_TICKS) {
                holdTicks = 0;
                ClientCutscenePlayer.stop(false);
            }
        } else {
            holdTicks = 0;
        }
    }

    // ==================== RENDER: วาดวิดีโอเต็มจอ + ซ่อน HUD ====================

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!ClientCutscenePlayer.isActive()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // พื้นหลังดำเผื่อเฟรมยังไม่มา หรือ aspect ratio ไม่พอดี
        graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);

        CutsceneVideoTexture texture = ClientCutscenePlayer.pollFrameForRender(VIDEO_TEXTURE);
        if (texture != null && texture.isReady()) {
            drawFullscreenVideo(graphics, texture, screenWidth, screenHeight);
        }

        drawSkipPrompt(graphics, screenWidth, screenHeight);

        // ยกเลิก event -> ซ่อน HUD/hotbar/crosshair/ฯลฯ ของวานิลลาทั้งหมดในเฟรมนี้
        event.setCanceled(true);
    }

    private static boolean textureRegistered = false;

    /**
     * GuiGraphics#blit(ResourceLocation, ...) จะ resolve texture ที่จะ bind ผ่าน TextureManager
     * ด้วย ResourceLocation ที่ให้ไป ดังนั้นต้องลงทะเบียน CutsceneVideoTexture ของเรากับ
     * TextureManager ภายใต้ ResourceLocation เดียวกันก่อน ไม่งั้นจะได้ texture ว่าง/placeholder แทน
     */
    private static void ensureTextureRegistered() {
        if (textureRegistered) return;
        Minecraft.getInstance().getTextureManager().register(CutsceneVideoTexture.LOCATION, VIDEO_TEXTURE);
        textureRegistered = true;
    }

    private static void drawFullscreenVideo(GuiGraphics graphics, CutsceneVideoTexture texture, int screenW, int screenH) {
        ensureTextureRegistered();
        float videoAspect = (float) texture.getWidth() / (float) texture.getHeight();
        float screenAspect = (float) screenW / (float) screenH;

        int drawW, drawH, x, y;
        // letterbox/pillarbox ให้พอดีจอโดยคงสัดส่วนวิดีโอ (ไม่ยืด/บีบภาพ)
        if (screenAspect > videoAspect) {
            drawH = screenH;
            drawW = Math.round(drawH * videoAspect);
        } else {
            drawW = screenW;
            drawH = Math.round(drawW / videoAspect);
        }
        x = (screenW - drawW) / 2;
        y = (screenH - drawH) / 2;

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(CutsceneVideoTexture.LOCATION,
                x, y, drawW, drawH, 0, 0, texture.getWidth(), texture.getHeight(),
                texture.getWidth(), texture.getHeight());
    }

    private static void drawSkipPrompt(GuiGraphics graphics, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();

        int cx = screenW - 40;
        int cy = screenH - 40;
        int radius = 14;

        // ข้อความแจ้งวิธีข้าม (แสดงตลอดตอนอยู่ในคัตซีน)
        Component prompt = Component.literal("กดค้าง SPACE เพื่อข้าม");
        int textWidth = mc.font.width(prompt);
        graphics.drawString(mc.font, prompt, screenW - textWidth - 12, screenH - 64, 0xFFFFFFFF, true);

        // วงกลมพื้นหลัง (จาง)
        drawRing(graphics, cx, cy, radius, 0f, 1f, 0x55FFFFFF);

        // วงแหวนความคืบหน้า (สว่าง) ตามสัดส่วนที่กดค้างไว้
        if (holdTicks > 0) {
            float progress = Math.min(1f, holdTicks / (float) SKIP_HOLD_TICKS);
            drawRing(graphics, cx, cy, radius, 0f, progress, 0xFFFFFFFF);
        }
    }

    /**
     * วาดวงแหวน (หรือส่วนโค้งของวงแหวน) แบบง่ายด้วยการ fill สี่เหลี่ยมเล็กๆ ไล่ตามมุม
     * เริ่มจาก startFraction ถึง endFraction (0..1 ของวงกลมเต็ม, เริ่มจากด้านบนตามเข็มนาฬิกา)
     */
    private static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, float startFraction, float endFraction, int color) {
        int steps = 60;
        int start = Math.round(startFraction * steps);
        int end = Math.round(endFraction * steps);

        for (int i = start; i < end; i++) {
            double angle = (i / (double) steps) * Math.PI * 2 - Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(angle) * radius);
            int py = cy + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(px - 1, py - 1, px + 2, py + 2, color);
        }
    }

    // ==================== ล็อกการเคลื่อนที่/การกระทำของผู้เล่นระหว่างคัตซีน ====================

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientCutscenePlayer.isActive()) return;
        var input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent event) {
        if (ClientCutscenePlayer.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ClientCutscenePlayer.isActive()) return;
        // ยังอนุญาตให้เปิดเมนู pause ได้ (กด Esc เพื่อออกเกม/settings) ปิดทุกหน้าจออื่น
        if (event.getNewScreen() != null && !(event.getNewScreen() instanceof PauseScreen)) {
            event.setCanceled(true);
        }
    }

    // ==================== เคลียร์สถานะเมื่อออกจากเซิร์ฟเวอร์ ====================

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        holdTicks = 0;
        ClientCutscenePlayer.stop(false);
        ClientSongPlayer.stop();
    }
}
