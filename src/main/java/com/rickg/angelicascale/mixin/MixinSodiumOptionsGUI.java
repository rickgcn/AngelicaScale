package com.rickg.angelicascale.mixin;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rickg.angelicascale.client.gui.options.AngelicaScaleOptionPage;

import me.flashyreese.mods.reeses_sodium_options.client.gui.ReeseSodiumVideoOptionsScreen;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;

@Mixin(value = SodiumOptionsGUI.class, remap = false)
public abstract class MixinSodiumOptionsGUI {

    @Shadow
    protected List<OptionPage> pages;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void angelicascale$addReeseScalePage(GuiScreen prevScreen, CallbackInfo ci) {
        if (!((Object) this instanceof ReeseSodiumVideoOptionsScreen)) {
            return;
        }

        for (OptionPage page : this.pages) {
            if ("Scale".equals(page.getName())) {
                return;
            }
        }

        int insertIndex = this.pages.size();

        if (insertIndex > 0 && this.pages.get(insertIndex - 1)
            .getGroups()
            .isEmpty()) {
            insertIndex--;
        }

        this.pages.add(insertIndex, AngelicaScaleOptionPage.create());
    }
}
