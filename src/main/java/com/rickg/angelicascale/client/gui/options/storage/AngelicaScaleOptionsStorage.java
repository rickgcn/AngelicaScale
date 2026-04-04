package com.rickg.angelicascale.client.gui.options.storage;

import com.rickg.angelicascale.Config;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;

public class AngelicaScaleOptionsStorage implements OptionStorage<AngelicaScaleOptionsStorage.Data> {

    private final Data data = new Data();

    public AngelicaScaleOptionsStorage() {
        this.data.worldRenderScalingEnabled = Config.enableWorldRenderScaling;
        this.data.worldRenderScalePercent = (int) Math.round(Config.clampScale(Config.worldRenderScale) * 100.0D);
    }

    @Override
    public Data getData() {
        return this.data;
    }

    @Override
    public void save() {
        Config.setWorldRenderScalingEnabled(this.data.worldRenderScalingEnabled);
        Config.setWorldRenderScale(this.data.worldRenderScalePercent / 100.0D);
        Config.save();
    }

    public static class Data {

        public boolean worldRenderScalingEnabled;
        public int worldRenderScalePercent;
    }
}
