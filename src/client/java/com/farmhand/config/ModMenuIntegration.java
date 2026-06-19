package com.farmhand.config;

import com.farmhand.TemplateMod;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createScreen;
    }

    private Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Farmhand 配置"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        var category = builder.getOrCreateCategory(Component.literal("常规"));

        category.addEntry(entryBuilder
                .startIntSlider(
                        Component.literal("单次操作上限"),
                        TemplateMod.CONFIG.maxOperation,
                        1, 500
                )
                .setDefaultValue(80)
                .setTooltip(Component.literal("单次连锁种植或收获的最大方块数（1~500）"))
                .setSaveConsumer(value -> TemplateMod.CONFIG.maxOperation = value)
                .build()
        );

        builder.setSavingRunnable(() -> {
            me.shedaniel.autoconfig.AutoConfig.getConfigHolder(FarmhandConfig.class).save();
        });

        return builder.build();
    }
}
