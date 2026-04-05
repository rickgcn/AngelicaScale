package com.rickg.angelicascale.client.upscale;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;

import com.rickg.angelicascale.AngelicaScaleMod;
import com.rickg.angelicascale.Config;

public final class Fsr1Upscaler {

    private static final String VERTEX_SHADER = "/assets/angelicascale/shaders/program/fullscreen.vert";
    private static final String EASU_FRAGMENT_SHADER = "/assets/angelicascale/shaders/program/fsr1_easu.frag";
    private static final String RCAS_FRAGMENT_SHADER = "/assets/angelicascale/shaders/program/fsr1_rcas.frag";

    private final FsrShaderProgram easuProgram = new FsrShaderProgram("FSR1 EASU", VERTEX_SHADER, EASU_FRAGMENT_SHADER);
    private final FsrShaderProgram rcasProgram = new FsrShaderProgram("FSR1 RCAS", VERTEX_SHADER, RCAS_FRAGMENT_SHADER);

    private final int[] easuConst0 = new int[4];
    private final int[] easuConst1 = new int[4];
    private final int[] easuConst2 = new int[4];
    private final int[] easuConst3 = new int[4];
    private final int[] rcasConst0 = new int[4];

    private boolean supportChecked;
    private boolean supported;
    private boolean loggedFallback;
    private Framebuffer intermediaryFramebuffer;

    public static boolean isSupportedInCurrentContext() {
        if (!OpenGlHelper.func_153193_b()) {
            return false;
        }

        try {
            ContextCapabilities capabilities = GLContext.getCapabilities();
            boolean textureGatherSupported = capabilities.OpenGL40 || capabilities.GL_ARB_texture_gather;
            return capabilities.OpenGL42 && textureGatherSupported;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean render(Framebuffer sourceFramebuffer, Framebuffer outputFramebuffer, int outputWidth, int outputHeight) {
        if (!this.ensureSupport()) {
            return false;
        }

        if (!this.easuProgram.ensureLoaded() || !this.rcasProgram.ensureLoaded()) {
            this.logFallback("FSR1 shader compilation failed; falling back to linear upscaling.");
            return false;
        }

        fillEasuConstants(
            this.easuConst0,
            this.easuConst1,
            this.easuConst2,
            this.easuConst3,
            sourceFramebuffer.framebufferWidth,
            sourceFramebuffer.framebufferHeight,
            sourceFramebuffer.framebufferTextureWidth,
            sourceFramebuffer.framebufferTextureHeight,
            outputWidth,
            outputHeight);

        if (Config.isFsrRcasEnabled()) {
            this.ensureIntermediaryFramebuffer(outputWidth, outputHeight);
            this.renderEasuPass(sourceFramebuffer, this.intermediaryFramebuffer, outputWidth, outputHeight);
            fillRcasConstants(this.rcasConst0, Config.getFsrRcasAttenuation());
            this.renderRcasPass(this.intermediaryFramebuffer, outputFramebuffer, outputWidth, outputHeight);
        } else {
            this.renderEasuPass(sourceFramebuffer, outputFramebuffer, outputWidth, outputHeight);
        }

        return true;
    }

    public void destroy() {
        this.easuProgram.destroy();
        this.rcasProgram.destroy();

        if (this.intermediaryFramebuffer != null) {
            this.intermediaryFramebuffer.deleteFramebuffer();
            this.intermediaryFramebuffer = null;
        }
    }

    private void renderEasuPass(Framebuffer sourceFramebuffer, Framebuffer targetFramebuffer, int outputWidth, int outputHeight) {
        this.beginPass(targetFramebuffer, outputWidth, outputHeight);

        try {
            this.easuProgram.use();
            this.easuProgram.setUniform1i("uInputTexture", 0);
            this.easuProgram.setUniform4i("uConst0", this.easuConst0);
            this.easuProgram.setUniform4i("uConst1", this.easuConst1);
            this.easuProgram.setUniform4i("uConst2", this.easuConst2);
            this.easuProgram.setUniform4i("uConst3", this.easuConst3);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceFramebuffer.framebufferTexture);
            drawFullscreenQuad(outputWidth, outputHeight);
        } finally {
            this.endPass();
        }
    }

    private void renderRcasPass(Framebuffer sourceFramebuffer, Framebuffer targetFramebuffer, int outputWidth, int outputHeight) {
        this.beginPass(targetFramebuffer, outputWidth, outputHeight);

        try {
            this.rcasProgram.use();
            this.rcasProgram.setUniform1i("uInputTexture", 0);
            this.rcasProgram.setUniform4i("uConst0", this.rcasConst0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceFramebuffer.framebufferTexture);
            drawFullscreenQuad(outputWidth, outputHeight);
        } finally {
            this.endPass();
        }
    }

    private void beginPass(Framebuffer targetFramebuffer, int width, int height) {
        targetFramebuffer.bindFramebuffer(false);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColorMask(true, true, true, true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private void endPass() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        OpenGlHelper.func_153161_d(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private boolean ensureSupport() {
        if (this.supportChecked) {
            return this.supported;
        }

        this.supportChecked = true;

        if (!isSupportedInCurrentContext()) {
            this.logFallback("FSR1 requires OpenGL 4.2 with shader support and textureGather; falling back to linear upscaling.");
            this.supported = false;
            return false;
        }

        this.supported = true;
        return true;
    }

    private void ensureIntermediaryFramebuffer(int width, int height) {
        if (this.intermediaryFramebuffer == null) {
            this.intermediaryFramebuffer = new Framebuffer(width, height, false);
            this.intermediaryFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            this.intermediaryFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
            return;
        }

        if (this.intermediaryFramebuffer.framebufferWidth != width
            || this.intermediaryFramebuffer.framebufferHeight != height) {
            this.intermediaryFramebuffer.createBindFramebuffer(width, height);
            this.intermediaryFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            this.intermediaryFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
    }

    private void logFallback(String message) {
        if (!this.loggedFallback) {
            this.loggedFallback = true;
            AngelicaScaleMod.LOG.warn(message);
        }
    }

    private static void drawFullscreenQuad(int width, int height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(-1);
        tessellator.addVertex(0.0D, height, 0.0D);
        tessellator.addVertex(width, height, 0.0D);
        tessellator.addVertex(width, 0.0D, 0.0D);
        tessellator.addVertex(0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    private static void fillEasuConstants(
        int[] const0,
        int[] const1,
        int[] const2,
        int[] const3,
        float inputViewportWidth,
        float inputViewportHeight,
        float inputTextureWidth,
        float inputTextureHeight,
        float outputWidth,
        float outputHeight) {
        const0[0] = floatBits(inputViewportWidth / outputWidth);
        const0[1] = floatBits(inputViewportHeight / outputHeight);
        const0[2] = floatBits(0.5F * inputViewportWidth / outputWidth - 0.5F);
        const0[3] = floatBits(0.5F * inputViewportHeight / outputHeight - 0.5F);

        const1[0] = floatBits(1.0F / inputTextureWidth);
        const1[1] = floatBits(1.0F / inputTextureHeight);
        const1[2] = floatBits(1.0F / inputTextureWidth);
        const1[3] = floatBits(-1.0F / inputTextureHeight);

        const2[0] = floatBits(-1.0F / inputTextureWidth);
        const2[1] = floatBits(2.0F / inputTextureHeight);
        const2[2] = floatBits(1.0F / inputTextureWidth);
        const2[3] = floatBits(2.0F / inputTextureHeight);

        const3[0] = floatBits(0.0F);
        const3[1] = floatBits(4.0F / inputTextureHeight);
        const3[2] = 0;
        const3[3] = 0;
    }

    private static void fillRcasConstants(int[] const0, float sharpnessAttenuation) {
        float clampedAttenuation = Math.max(0.0F, sharpnessAttenuation);
        const0[0] = floatBits((float) Math.pow(2.0D, -clampedAttenuation));
        const0[1] = 0;
        const0[2] = 0;
        const0[3] = 0;
    }

    private static int floatBits(float value) {
        return Float.floatToRawIntBits(value);
    }
}
