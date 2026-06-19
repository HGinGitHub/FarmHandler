package com.farmhand;

import com.farmhand.config.FarmhandConfig;
import com.farmhand.handler.FarmHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
	public static final String MOD_ID = "farmhand";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static FarmhandConfig CONFIG;

	@Override
	public void onInitialize() {
		// 注册配置
		AutoConfig.register(FarmhandConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(FarmhandConfig.class).getConfig();

		FarmHandler.register();
		LOGGER.info("FarmHelper mod initialized! maxOperation={}", CONFIG.maxOperation);
	}
}
