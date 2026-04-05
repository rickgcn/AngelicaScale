package com.rickg.angelicascale.client.gui.options;

import com.google.common.collect.ImmutableList;
import com.rickg.angelicascale.UpscaleAlgorithm;
import com.rickg.angelicascale.client.gui.options.storage.AngelicaScaleOptionsStorage;
import com.rickg.angelicascale.client.upscale.Fsr1Upscaler;

import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.util.StatCollector;

public final class AngelicaScaleOptionPage {

    private static final String KEY_PREFIX = "angelicascale.options.";

    private AngelicaScaleOptionPage() {}

    public static OptionPage create() {
        AngelicaScaleOptionsStorage storage = new AngelicaScaleOptionsStorage();
        boolean fsrSupported = Fsr1Upscaler.isSupportedInCurrentContext();
        UpscaleAlgorithm[] availableAlgorithms = fsrSupported
            ? new UpscaleAlgorithm[] {UpscaleAlgorithm.NEAREST, UpscaleAlgorithm.LINEAR, UpscaleAlgorithm.FSR1}
            : new UpscaleAlgorithm[] {UpscaleAlgorithm.NEAREST, UpscaleAlgorithm.LINEAR};

        OptionGroup.Builder group = OptionGroup.createBuilder()
            .add(
                OptionImpl.createBuilder(boolean.class, storage)
                    .setName(tr("enable_world_render_scaling.name"))
                    .setTooltip(tr("enable_world_render_scaling.tooltip"))
                    .setControl(TickBoxControl::new)
                    .setBinding(
                        (data, value) -> data.worldRenderScalingEnabled = value,
                        data -> data.worldRenderScalingEnabled)
                    .setImpact(OptionImpact.HIGH)
                    .build())
            .add(
                OptionImpl.createBuilder(int.class, storage)
                    .setName(tr("world_render_scale.name"))
                    .setTooltip(tr("world_render_scale.tooltip"))
                    .setControl(option -> new SliderControl(option, 25, 100, 1, ControlValueFormatter.percentage()))
                    .setBinding(
                        (data, value) -> data.worldRenderScalePercent = value,
                        data -> data.worldRenderScalePercent)
                    .setImpact(OptionImpact.HIGH)
                    .build())
            .add(
                OptionImpl.createBuilder(UpscaleAlgorithm.class, storage)
                    .setName(tr("upscale_algorithm.name"))
                    .setTooltip(tr("upscale_algorithm.tooltip"))
                    .setControl(
                        option -> new CyclingControl<UpscaleAlgorithm>(
                            option,
                            UpscaleAlgorithm.class,
                            availableAlgorithms))
                    .setBinding(
                        (data, value) -> data.upscaleAlgorithm = value,
                        data -> data.upscaleAlgorithm)
                    .setImpact(OptionImpact.MEDIUM)
                    .build());

        if (fsrSupported) {
            group.add(
                OptionImpl.createBuilder(int.class, storage)
                    .setName(tr("rcas_sharpness.name"))
                    .setTooltip(tr("rcas_sharpness.tooltip"))
                    .setControl(option -> new SliderControl(option, 0, 100, 1, ControlValueFormatter.percentage()))
                    .setBinding(
                        (data, value) -> data.fsrRcasSharpnessPercent = value,
                        data -> data.fsrRcasSharpnessPercent)
                    .setImpact(OptionImpact.MEDIUM)
                    .build());
        }

        return new OptionPage(tr("page"), ImmutableList.of(group.build()));
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(KEY_PREFIX + key);
    }
}
