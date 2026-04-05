package com.rickg.angelicascale;

import net.minecraft.util.StatCollector;

public enum UpscaleAlgorithm {

    NEAREST("Nearest", "angelicascale.algorithm.nearest"),
    LINEAR("Linear", "angelicascale.algorithm.linear"),
    FSR1("FSR1", "angelicascale.algorithm.fsr1");

    private final String legacyDisplayName;
    private final String translationKey;

    UpscaleAlgorithm(String legacyDisplayName, String translationKey) {
        this.legacyDisplayName = legacyDisplayName;
        this.translationKey = translationKey;
    }

    public String getDisplayName() {
        String translated = StatCollector.translateToLocal(this.translationKey);
        return this.translationKey.equals(translated) ? this.legacyDisplayName : translated;
    }

    @Override
    public String toString() {
        return this.getDisplayName();
    }

    public static UpscaleAlgorithm fromConfigValue(String value) {
        for (UpscaleAlgorithm algorithm : values()) {
            if (algorithm.name()
                .equalsIgnoreCase(value) || algorithm.legacyDisplayName.equalsIgnoreCase(value)) {
                return algorithm;
            }
        }

        return LINEAR;
    }

    public static String[] configValues() {
        UpscaleAlgorithm[] values = values();
        String[] result = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].name();
        }

        return result;
    }
}
