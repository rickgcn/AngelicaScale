package com.rickg.angelicascale;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static final double MIN_WORLD_RENDER_SCALE = 0.25D;
    private static final double MAX_WORLD_RENDER_SCALE = 1.0D;
    private static final int MIN_FSR_RCAS_SHARPNESS = 0;
    private static final int MAX_FSR_RCAS_SHARPNESS = 100;
    private static Configuration configuration;
    private static File configurationFile;

    public static boolean enableWorldRenderScaling = true;
    public static double worldRenderScale = 0.75D;
    public static UpscaleAlgorithm upscaleAlgorithm = UpscaleAlgorithm.LINEAR;
    public static int fsrRcasSharpnessPercent = 50;

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

        upscaleAlgorithm = UpscaleAlgorithm.fromConfigValue(
            configuration.getString(
                "upscaleAlgorithm",
                Configuration.CATEGORY_GENERAL,
                upscaleAlgorithm.name(),
                "Selects how the low-resolution world image is upscaled back to the screen.",
                UpscaleAlgorithm.configValues()));

        fsrRcasSharpnessPercent = configuration.getInt(
            "fsrRcasSharpnessPercent",
            Configuration.CATEGORY_GENERAL,
            fsrRcasSharpnessPercent,
            MIN_FSR_RCAS_SHARPNESS,
            MAX_FSR_RCAS_SHARPNESS,
            "Controls the additional RCAS sharpening pass used by FSR1. 0 disables RCAS.");

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

    public static void setUpscaleAlgorithm(UpscaleAlgorithm algorithm) {
        upscaleAlgorithm = algorithm == null ? UpscaleAlgorithm.LINEAR : algorithm;
    }

    public static void setFsrRcasSharpnessPercent(int sharpnessPercent) {
        fsrRcasSharpnessPercent = clampFsrRcasSharpnessPercent(sharpnessPercent);
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

        configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "upscaleAlgorithm",
                upscaleAlgorithm.name(),
                "Selects how the low-resolution world image is upscaled back to the screen.",
                UpscaleAlgorithm.configValues())
            .set(upscaleAlgorithm.name());

        configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "fsrRcasSharpnessPercent",
                fsrRcasSharpnessPercent,
                "Controls the additional RCAS sharpening pass used by FSR1. 0 disables RCAS.",
                MIN_FSR_RCAS_SHARPNESS,
                MAX_FSR_RCAS_SHARPNESS)
            .set(fsrRcasSharpnessPercent);

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

    public static UpscaleAlgorithm getUpscaleAlgorithm() {
        return upscaleAlgorithm;
    }

    public static int getFsrRcasSharpnessPercent() {
        return fsrRcasSharpnessPercent;
    }

    public static boolean isFsrRcasEnabled() {
        return fsrRcasSharpnessPercent > 0;
    }

    public static float getFsrRcasAttenuation() {
        return 2.0F * (100.0F - clampFsrRcasSharpnessPercent(fsrRcasSharpnessPercent)) / 100.0F;
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

    public static int clampFsrRcasSharpnessPercent(int sharpnessPercent) {
        return Math.max(MIN_FSR_RCAS_SHARPNESS, Math.min(MAX_FSR_RCAS_SHARPNESS, sharpnessPercent));
    }
}
