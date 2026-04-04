package com.rickg.angelicascale.client;

import java.lang.reflect.Method;

import com.rickg.angelicascale.AngelicaScaleMod;
import com.rickg.angelicascale.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

public final class RenderHookState {

    private static boolean initialized = false;
    private static boolean renderOverrideActive = false;
    private static boolean loggedEnabled = false;
    private static boolean loggedShaderBypass = false;
    private static boolean loggedFramebufferBypass = false;
    private static boolean loggedPipelineRebind = false;
    private static boolean loggedViewportFallback = false;
    private static int scaledViewportWidth = -1;
    private static int scaledViewportHeight = -1;
    private static Framebuffer scaledWorldFramebuffer;

    private static Method irisApiGetInstance;
    private static Method irisApiIsShaderPackInUse;
    private static boolean irisLookupAttempted = false;
    private static Method angelicaGlViewport;
    private static boolean angelicaViewportLookupAttempted = false;

    private RenderHookState() {}

    public static void init() {
        if (!initialized) {
            initialized = true;
            AngelicaScaleMod.LOG.info("Client render scaling initialized.");
        }
    }

    public static void onBeforeWorldRender(EntityRenderer renderer, float partialTicks) {
        if (!Config.isWorldScalingEnabled()) {
            return;
        }

        if (!OpenGlHelper.isFramebufferEnabled()) {
            if (!loggedFramebufferBypass) {
                loggedFramebufferBypass = true;
                AngelicaScaleMod.LOG.warn("Framebuffer unsupported; render scaling disabled.");
            }
            return;
        }

        if (isShaderPipelineActive(renderer)) {
            if (!loggedShaderBypass) {
                loggedShaderBypass = true;
                AngelicaScaleMod.LOG.info("Shader pipeline detected; render scaling bypassed.");
            }
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int scaledWidth = Config.getScaledDimension(mc.displayWidth);
        int scaledHeight = Config.getScaledDimension(mc.displayHeight);

        if (scaledWidth >= mc.displayWidth && scaledHeight >= mc.displayHeight) {
            return;
        }

        ensureFramebuffer(scaledWidth, scaledHeight);

        scaledViewportWidth = scaledWidth;
        scaledViewportHeight = scaledHeight;
        bindScaledWorldFramebuffer();
        renderOverrideActive = true;

        if (!loggedEnabled) {
            loggedEnabled = true;
            AngelicaScaleMod.LOG.info(
                "World render scaling active at {}x (GUI stays native resolution).",
                Config.worldRenderScale);
        }
    }

    public static void onAfterWorldRender(EntityRenderer renderer, float partialTicks) {
        if (!renderOverrideActive) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(false);
        setViewport(0, 0, mc.displayWidth, mc.displayHeight);
        scaledWorldFramebuffer.framebufferRender(mc.displayWidth, mc.displayHeight);

        renderOverrideActive = false;
        scaledViewportWidth = -1;
        scaledViewportHeight = -1;
    }

    public static boolean rebindScaledWorldFramebufferForFixedPipeline() {
        if (!renderOverrideActive || scaledWorldFramebuffer == null || scaledViewportWidth <= 0 || scaledViewportHeight <= 0) {
            return false;
        }

        bindScaledWorldFramebuffer();

        if (!loggedPipelineRebind) {
            loggedPipelineRebind = true;
            AngelicaScaleMod.LOG.info("Rebinding scaled framebuffer after Angelica fixed-function pipeline setup.");
        }

        return true;
    }

    private static void ensureFramebuffer(int width, int height) {
        if (scaledWorldFramebuffer == null) {
            scaledWorldFramebuffer = new Framebuffer(width, height, true);
            scaledWorldFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            scaledWorldFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
            return;
        }

        if (scaledWorldFramebuffer.framebufferWidth != width || scaledWorldFramebuffer.framebufferHeight != height) {
            scaledWorldFramebuffer.createBindFramebuffer(width, height);
            scaledWorldFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            scaledWorldFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
    }

    private static boolean isShaderPipelineActive(EntityRenderer renderer) {
        ShaderGroup shaderGroup = renderer.theShaderGroup;
        return shaderGroup != null || isIrisShaderPackInUse();
    }

    public static void applyWorldViewport(int x, int y, int width, int height) {
        if (renderOverrideActive && scaledViewportWidth > 0 && scaledViewportHeight > 0) {
            setViewport(x, y, scaledViewportWidth, scaledViewportHeight);
            return;
        }

        setViewport(x, y, width, height);
    }

    private static void bindScaledWorldFramebuffer() {
        scaledWorldFramebuffer.bindFramebuffer(false);
        setViewport(0, 0, scaledViewportWidth, scaledViewportHeight);
    }

    private static void setViewport(int x, int y, int width, int height) {
        try {
            if (!angelicaViewportLookupAttempted) {
                angelicaViewportLookupAttempted = true;
                Class<?> glStateManagerClass = Class.forName("com.gtnewhorizons.angelica.glsm.GLStateManager");
                angelicaGlViewport = glStateManagerClass.getMethod("glViewport", int.class, int.class, int.class, int.class);
            }

            if (angelicaGlViewport != null) {
                angelicaGlViewport.invoke(null, x, y, width, height);
                return;
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            if (!loggedViewportFallback) {
                loggedViewportFallback = true;
                AngelicaScaleMod.LOG.debug("Falling back to raw GL11 viewport updates.", e);
            }
        }

        GL11.glViewport(x, y, width, height);
    }

    private static boolean isIrisShaderPackInUse() {
        try {
            if (!irisLookupAttempted) {
                irisLookupAttempted = true;
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiGetInstance = irisApiClass.getMethod("getInstance");
                irisApiIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            }

            if (irisApiGetInstance == null || irisApiIsShaderPackInUse == null) {
                return false;
            }

            Object irisApi = irisApiGetInstance.invoke(null);
            return Boolean.TRUE.equals(irisApiIsShaderPackInUse.invoke(irisApi));
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException e) {
            AngelicaScaleMod.LOG.debug("Failed to query Iris shader state.", e);
            return false;
        }
    }
}
