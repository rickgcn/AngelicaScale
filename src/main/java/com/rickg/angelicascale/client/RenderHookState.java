package com.rickg.angelicascale.client;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;

import org.lwjgl.opengl.GL11;

import com.rickg.angelicascale.AngelicaScaleMod;
import com.rickg.angelicascale.Config;
import com.rickg.angelicascale.mixin.MixinMinecraftAccessor;

public final class RenderHookState {

    private enum BackendMode {
        NONE,
        FIXED_FUNCTION,
        IRIS_MAIN_SWAP
    }

    private static boolean initialized = false;
    private static boolean loggedEnabled = false;
    private static boolean loggedShaderBypass = false;
    private static boolean loggedFramebufferBypass = false;
    private static boolean loggedPipelineRebind = false;
    private static boolean loggedIrisPipelineEnabled = false;
    private static boolean loggedViewportFallback = false;
    private static int scaledViewportWidth = -1;
    private static int scaledViewportHeight = -1;
    private static Framebuffer scaledSceneFramebuffer;
    private static Framebuffer nativeMainFramebuffer;
    private static BackendMode activeMode = BackendMode.NONE;

    private static Method irisApiGetInstance;
    private static Method irisApiIsShaderPackInUse;
    private static boolean irisLookupAttempted = false;
    private static Method irisGetPipelineManager;
    private static Method irisDestroyPipeline;
    private static boolean irisPipelineLookupAttempted = false;
    private static Method angelicaGlViewport;
    private static boolean angelicaViewportLookupAttempted = false;
    private static int irisPreparedWidth = -1;
    private static int irisPreparedHeight = -1;

    private RenderHookState() {}

    public static void init() {
        if (!initialized) {
            initialized = true;
            AngelicaScaleMod.LOG.info("Client render scaling initialized.");
        }
    }

    public static void onBeforeWorldRender(EntityRenderer renderer, float partialTicks) {
        if (!Config.isWorldScalingEnabled()) {
            markIrisPipelineDirty();
            return;
        }

        if (!OpenGlHelper.isFramebufferEnabled()) {
            if (!loggedFramebufferBypass) {
                loggedFramebufferBypass = true;
                AngelicaScaleMod.LOG.warn("Framebuffer unsupported; render scaling disabled.");
            }
            return;
        }

        boolean irisShaderPackInUse = isIrisShaderPackInUse();
        ShaderGroup shaderGroup = renderer.theShaderGroup;

        if (!irisShaderPackInUse && shaderGroup != null) {
            if (!loggedShaderBypass) {
                loggedShaderBypass = true;
                AngelicaScaleMod.LOG.info("Vanilla shader pipeline detected; render scaling bypassed.");
            }
            markIrisPipelineDirty();
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int scaledWidth = Config.getScaledDimension(mc.displayWidth);
        int scaledHeight = Config.getScaledDimension(mc.displayHeight);

        if (scaledWidth >= mc.displayWidth && scaledHeight >= mc.displayHeight) {
            markIrisPipelineDirty();
            return;
        }

        ensureFramebuffer(scaledWidth, scaledHeight);

        scaledViewportWidth = scaledWidth;
        scaledViewportHeight = scaledHeight;

        if (irisShaderPackInUse) {
            nativeMainFramebuffer = mc.getFramebuffer();
            ((MixinMinecraftAccessor) mc).angelicascale$setFramebufferMc(scaledSceneFramebuffer);
            activeMode = BackendMode.IRIS_MAIN_SWAP;
            ensureIrisPipelineForScaledMain(scaledWidth, scaledHeight);
            bindScaledSceneFramebuffer();

            if (!loggedIrisPipelineEnabled) {
                loggedIrisPipelineEnabled = true;
                AngelicaScaleMod.LOG
                    .info("Iris shader pipeline detected; swapping main framebuffer for render scaling.");
            }
        } else {
            activeMode = BackendMode.FIXED_FUNCTION;
            bindScaledSceneFramebuffer();
        }

        if (!loggedEnabled) {
            loggedEnabled = true;
            AngelicaScaleMod.LOG
                .info("World render scaling active at {}x (GUI stays native resolution).", Config.worldRenderScale);
        }
    }

    public static void onAfterWorldRender(EntityRenderer renderer, float partialTicks) {
        if (activeMode == BackendMode.NONE) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        try {
            if (activeMode == BackendMode.IRIS_MAIN_SWAP && nativeMainFramebuffer != null) {
                ((MixinMinecraftAccessor) mc).angelicascale$setFramebufferMc(nativeMainFramebuffer);
            }

            Framebuffer outputFramebuffer = nativeMainFramebuffer != null ? nativeMainFramebuffer : mc.getFramebuffer();
            int outputWidth = outputFramebuffer.framebufferWidth;
            int outputHeight = outputFramebuffer.framebufferHeight;

            outputFramebuffer.bindFramebuffer(false);
            setViewport(0, 0, outputWidth, outputHeight);

            if (scaledSceneFramebuffer != null) {
                scaledSceneFramebuffer.framebufferRender(outputWidth, outputHeight);
            }
        } finally {
            activeMode = BackendMode.NONE;
            nativeMainFramebuffer = null;
            scaledViewportWidth = -1;
            scaledViewportHeight = -1;
        }
    }

    public static boolean rebindScaledWorldFramebufferForFixedPipeline() {
        if (activeMode != BackendMode.FIXED_FUNCTION || scaledSceneFramebuffer == null
            || scaledViewportWidth <= 0
            || scaledViewportHeight <= 0) {
            return false;
        }

        bindScaledSceneFramebuffer();

        if (!loggedPipelineRebind) {
            loggedPipelineRebind = true;
            AngelicaScaleMod.LOG.info("Rebinding scaled framebuffer after Angelica fixed-function pipeline setup.");
        }

        return true;
    }

    private static void ensureFramebuffer(int width, int height) {
        if (scaledSceneFramebuffer == null) {
            scaledSceneFramebuffer = new Framebuffer(width, height, true);
            scaledSceneFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            scaledSceneFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
            return;
        }

        if (scaledSceneFramebuffer.framebufferWidth != width || scaledSceneFramebuffer.framebufferHeight != height) {
            scaledSceneFramebuffer.createBindFramebuffer(width, height);
            scaledSceneFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            scaledSceneFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
    }

    public static void applyWorldViewport(int x, int y, int width, int height) {
        if (activeMode == BackendMode.FIXED_FUNCTION && scaledViewportWidth > 0 && scaledViewportHeight > 0) {
            setViewport(x, y, scaledViewportWidth, scaledViewportHeight);
            return;
        }

        setViewport(x, y, width, height);
    }

    private static void bindScaledSceneFramebuffer() {
        scaledSceneFramebuffer.bindFramebuffer(false);
        setViewport(0, 0, scaledViewportWidth, scaledViewportHeight);
    }

    private static void ensureIrisPipelineForScaledMain(int scaledWidth, int scaledHeight) {
        if (irisPreparedWidth == scaledWidth && irisPreparedHeight == scaledHeight) {
            return;
        }

        try {
            if (!irisPipelineLookupAttempted) {
                irisPipelineLookupAttempted = true;
                Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
                irisGetPipelineManager = irisClass.getMethod("getPipelineManager");
                Class<?> pipelineManagerClass = Class.forName("net.coderbot.iris.pipeline.PipelineManager");
                irisDestroyPipeline = pipelineManagerClass.getMethod("destroyPipeline");
            }

            if (irisGetPipelineManager == null || irisDestroyPipeline == null) {
                return;
            }

            Object pipelineManager = irisGetPipelineManager.invoke(null);
            irisDestroyPipeline.invoke(pipelineManager);
            irisPreparedWidth = scaledWidth;
            irisPreparedHeight = scaledHeight;
            AngelicaScaleMod.LOG
                .info("Recreating Iris pipeline for scaled framebuffer {}x{}.", scaledWidth, scaledHeight);
        } catch (ClassNotFoundException ignored) {
            markIrisPipelineDirty();
        } catch (ReflectiveOperationException e) {
            markIrisPipelineDirty();
            AngelicaScaleMod.LOG.debug("Failed to recreate Iris pipeline for scaled rendering.", e);
        }
    }

    private static void markIrisPipelineDirty() {
        irisPreparedWidth = -1;
        irisPreparedHeight = -1;
    }

    private static void setViewport(int x, int y, int width, int height) {
        try {
            if (!angelicaViewportLookupAttempted) {
                angelicaViewportLookupAttempted = true;
                Class<?> glStateManagerClass = Class.forName("com.gtnewhorizons.angelica.glsm.GLStateManager");
                angelicaGlViewport = glStateManagerClass
                    .getMethod("glViewport", int.class, int.class, int.class, int.class);
            }

            if (angelicaGlViewport != null) {
                angelicaGlViewport.invoke(null, x, y, width, height);
                return;
            }
        } catch (ClassNotFoundException ignored) {} catch (ReflectiveOperationException e) {
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
