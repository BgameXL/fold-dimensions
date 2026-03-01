package com.bgame.fold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FoldCommon {
    public static final String MOD_ID = "fold_dimensions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("[FoldDimensions] Inicializando mod...");
        LOGGER.info("[FoldDimensions] Listo.");
    }

    private FoldCommon() {}
}