package com.rickg.angelicascale.client.gui.options.storage;

import com.rickg.angelicascale.Config;
import com.rickg.angelicascale.UpscaleAlgorithm;
import com.rickg.angelicascale.client.upscale.Fsr1Upscaler;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;

public class AngelicaScaleOptionsStorage implements OptionStorage<AngelicaScaleOptionsStorage.Data> {

    private final Data data = new Data();

    public AngelicaScaleOptionsStorage() {
        this.data.worldRenderScalingEnabled = Config.enableWorldRenderScaling;
        this.data.worldRenderScalePercent = (int) Math.round(Config.clampScale(Config.worldRenderScale) * 100.0D);
        this.data.upscaleAlgorithm = sanitizeUpscaleAlgorithm(Config.getUpscaleAlgorithm());
        this.data.fsrRcasSharpnessPercent = Config.getFsrRcasSharpnessPercent();
    }

    @Override
    public Data getData() {
        return this.data;
    }

    @Override
    public void save() {
        Config.setWorldRenderScalingEnabled(this.data.worldRenderScalingEnabled);
        Config.setWorldRenderScale(this.data.worldRenderScalePercent / 100.0D);
        Config.setUpscaleAlgorithm(this.data.upscaleAlgorithm);
        Config.setFsrRcasSharpnessPercent(this.data.fsrRcasSharpnessPercent);
        Config.save();
    }

    public static class Data {

        public boolean worldRenderScalingEnabled;
        public int worldRenderScalePercent;
        public UpscaleAlgorithm upscaleAlgorithm;
        public int fsrRcasSharpnessPercent;
    }

    private static UpscaleAlgorithm sanitizeUpscaleAlgorithm(UpscaleAlgorithm algorithm) {
        if (algorithm == UpscaleAlgorithm.FSR1 && !Fsr1Upscaler.isSupportedInCurrentContext()) {
            return UpscaleAlgorithm.LINEAR;
        }

        return algorithm;
    }
}
