package com.farmhand.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "farmhand")
public class FarmhandConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 1, max = 500)
    @ConfigEntry.Gui.PrefixText
    public int maxOperation = 80;
}
