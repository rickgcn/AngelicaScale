package com.rickg.angelicascale;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static final double MIN_WORLD_RENDER_SCALE = 0.25D;
    private static final double MAX_WORLD_RENDER_SCALE = 1.0D;
    private static Configuration configuration;
    private static File configurationFile;

    public static boolean enableWorldRenderScaling = true;
    public static double worldRenderScale = 0.75D;

    public static void synchronizeConfiguration(File configFile) {
        configurationFile = configFile;
        configuration = new Configuration(configFile);
        loadFromConfiguration();
    }

    private static void loadFromConfiguration() {
        enableWorldRenderScaling = configuration.getBoolean(
            "enableWorldRenderScaling",
            Configuration.CATEGORY_GENERAL,
            enableWorldRenderScaling,
            "Enable low-resolution world rendering. GUI remains full resolution.");

        worldRenderScale = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "worldRenderScale",
                worldRenderScale,
                "Scale factor for 3D world rendering only. 1.0 keeps native resolution.",
                MIN_WORLD_RENDER_SCALE,
                MAX_WORLD_RENDER_SCALE)
            .getDouble(worldRenderScale);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static void setWorldRenderScalingEnabled(boolean enabled) {
        enableWorldRenderScaling = enabled;
    }

    public static void setWorldRenderScale(double scale) {
        worldRenderScale = clampScale(scale);
    }

    public static void save() {
        if (configuration == null) {
            if (configurationFile == null) {
                return;
            }

            configuration = new Configuration(configurationFile);
        }

        configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "enableWorldRenderScaling",
                enableWorldRenderScaling,
                "Enable low-resolution world rendering. GUI remains full resolution.")
            .set(enableWorldRenderScaling);

        configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "worldRenderScale",
                worldRenderScale,
                "Scale factor for 3D world rendering only. 1.0 keeps native resolution.",
                MIN_WORLD_RENDER_SCALE,
                MAX_WORLD_RENDER_SCALE)
            .set(worldRenderScale);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static boolean isWorldScalingEnabled() {
        return enableWorldRenderScaling && worldRenderScale < 0.999D;
    }

    public static int getScaledDimension(int originalDimension) {
        return Math
            .max(1, Math.min(originalDimension, (int) Math.round(originalDimension * clampScale(worldRenderScale))));
    }

    public static double getMinWorldRenderScale() {
        return MIN_WORLD_RENDER_SCALE;
    }

    public static double getMaxWorldRenderScale() {
        return MAX_WORLD_RENDER_SCALE;
    }

    public static double clampScale(double scale) {
        return Math.max(MIN_WORLD_RENDER_SCALE, Math.min(MAX_WORLD_RENDER_SCALE, scale));
    }
}
