package com.farmhand;

import com.farmhand.handler.FarmHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
	public static final String MOD_ID = "farmhand";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FarmHandler.register();
		LOGGER.info("FarmHelper mod initialized!");
	}
}
