package com.rickg.angelicascale.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.rickg.angelicascale.client.RenderHookState;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    public abstract void renderWorld(float partialTicks, long finishTimeNano);

    @Redirect(
        method = "updateCameraAndRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V"))
    private void angelicascale$wrapWorldRender(EntityRenderer instance, float partialTicks, long finishTimeNano) {
        RenderHookState.onBeforeWorldRender((EntityRenderer) (Object) this, partialTicks);
        try {
            this.renderWorld(partialTicks, finishTimeNano);
        } finally {
            RenderHookState.onAfterWorldRender((EntityRenderer) (Object) this, partialTicks);
        }
    }

    @Redirect(
        method = "renderWorld",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V", remap = false))
    private void angelicascale$redirectWorldViewport(int x, int y, int width, int height) {
        RenderHookState.applyWorldViewport(x, y, width, height);
    }
}
