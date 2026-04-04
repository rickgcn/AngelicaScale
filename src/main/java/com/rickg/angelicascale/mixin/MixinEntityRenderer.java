package com.rickg.angelicascale.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rickg.angelicascale.client.RenderHookState;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(
        method = "updateCameraAndRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V"))
    private void angelicascale$beforeWorldRender(float partialTicks, CallbackInfo ci) {
        RenderHookState.onBeforeWorldRender((EntityRenderer) (Object) this, partialTicks);
    }

    @Inject(
        method = "updateCameraAndRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V",
            shift = At.Shift.AFTER))
    private void angelicascale$afterWorldRender(float partialTicks, CallbackInfo ci) {
        RenderHookState.onAfterWorldRender((EntityRenderer) (Object) this, partialTicks);
    }

    @Redirect(
        method = "renderWorld",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V", remap = false))
    private void angelicascale$redirectWorldViewport(int x, int y, int width, int height) {
        RenderHookState.applyWorldViewport(x, y, width, height);
    }
}
