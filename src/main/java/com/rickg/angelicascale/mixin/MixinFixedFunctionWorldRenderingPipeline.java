package com.rickg.angelicascale.mixin;

import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rickg.angelicascale.client.RenderHookState;

@Pseudo
@Mixin(targets = "net.coderbot.iris.pipeline.FixedFunctionWorldRenderingPipeline", remap = false)
public abstract class MixinFixedFunctionWorldRenderingPipeline {

    @Inject(method = "beginLevelRendering", at = @At("HEAD"), cancellable = true, remap = false)
    private void angelicascale$keepScaledFramebuffer(CallbackInfo ci) {
        if (!RenderHookState.rebindScaledWorldFramebufferForFixedPipeline()) {
            return;
        }

        GL20.glUseProgram(0);
        ci.cancel();
    }
}
