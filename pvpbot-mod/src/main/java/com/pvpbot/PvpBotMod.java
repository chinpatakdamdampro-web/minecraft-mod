package com.pvpbot;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvpBotMod implements ModInitializer {

    public static final String MOD_ID = "pvpbot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("PvP Practice Bot Mod loaded!");

        // Register commands (also registers the entity type internally)
        PvpBotCommands.register();
    }
}
