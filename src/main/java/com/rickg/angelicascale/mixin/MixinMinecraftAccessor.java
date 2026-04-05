package com.rickg.angelicascale.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MixinMinecraftAccessor {

    @Accessor("framebufferMc")
    void angelicascale$setFramebufferMc(Framebuffer framebuffer);
}
