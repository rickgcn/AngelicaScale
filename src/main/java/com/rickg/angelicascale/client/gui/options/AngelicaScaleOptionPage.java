package com.rickg.angelicascale.client.gui.options;

import com.google.common.collect.ImmutableList;
import com.rickg.angelicascale.client.gui.options.storage.AngelicaScaleOptionsStorage;

import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;

public final class AngelicaScaleOptionPage {

    private AngelicaScaleOptionPage() {}

    public static OptionPage create() {
        AngelicaScaleOptionsStorage storage = new AngelicaScaleOptionsStorage();

        return new OptionPage("Scale", ImmutableList.of(
            OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, storage)
                    .setName("Enable World Render Scaling")
                    .setTooltip("Renders the 3D world at a lower internal resolution while keeping the GUI at native resolution.")
                    .setControl(TickBoxControl::new)
                    .setBinding(
                        (data, value) -> data.worldRenderScalingEnabled = value,
                        data -> data.worldRenderScalingEnabled)
                    .setImpact(OptionImpact.HIGH)
                    .build())
                .add(OptionImpl.createBuilder(int.class, storage)
                    .setName("World Render Scale")
                    .setTooltip("Controls the internal 3D world rendering resolution. Lower values improve performance but reduce image sharpness.")
                    .setControl(option -> new SliderControl(option, 25, 100, 1, ControlValueFormatter.percentage()))
                    .setBinding(
                        (data, value) -> data.worldRenderScalePercent = value,
                        data -> data.worldRenderScalePercent)
                    .setImpact(OptionImpact.HIGH)
                    .build())
                .build()));
    }
}
