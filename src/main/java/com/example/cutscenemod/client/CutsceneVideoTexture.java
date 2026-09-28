package com.example.cutscenemod.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

/**
 * Texture ที่เราคุมค่า GL id เอง แทนที่จะให้ Minecraft โหลดจากไฟล์ resource ปกติ
 * ทุกเฟรมของวิดีโอจะถูกอัปโหลดตรงเข้า texture นี้ด้วย glTexSubImage2D
 *
 * หมายเหตุ: ฟอร์แมตพิกเซลจาก VLCJ (RV32) เรียงแบบ BGRA จึงอัปโหลดด้วย GL_BGRA ตรงๆ
 * โดยไม่ต้องสลับช่องสีเอง (ประหยัดเวลา CPU)
 */
public class CutsceneVideoTexture extends AbstractTexture {

    private int width = -1;
    private int height = -1;
    private boolean created = false;

    public CutsceneVideoTexture() {
    }

    private void ensureCreated(int w, int h) {
        if (created && w == width && h == height) return;

        if (created) {
            super.releaseId();
            created = false;
        }

        this.width = w;
        this.height = h;
        this.id = GlStateManager._genTexture();
        GlStateManager._bindTexture(this.id);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        created = true;
    }

    /**
     * เรียกจาก render thread เท่านั้น (เช่นภายใน RenderGuiEvent.Pre)
     */
    public void uploadFrame(ByteBuffer frame, int w, int h) {
        ensureCreated(w, h);
        GlStateManager._bindTexture(this.id);
        frame.rewind();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, w, h,
                GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, frame);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isReady() {
        return created;
    }

    @Override
    public void load(net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        // ไม่มีอะไรให้โหลดจากไฟล์ resource ปกติ, เนื้อหาถูกอัปเดตแบบ dynamic ผ่าน uploadFrame()
    }

    @Override
    public void releaseId() {
        if (created) {
            super.releaseId();
            created = false;
        }
    }

    // ชื่อสำหรับลงทะเบียนกับ TextureManager
    public static final ResourceLocation LOCATION =
            new ResourceLocation("cutscenemod", "dynamic/cutscene_video");
}
