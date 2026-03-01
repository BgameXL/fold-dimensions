package com.bgame.fold.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public final class FoldDimension {

    public static final ResourceKey<Level> FOLD_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(com.bgame.fold.FoldCommon.MOD_ID, "fold")
    );

    public static final ResourceKey<DimensionType> FOLD_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(com.bgame.fold.FoldCommon.MOD_ID, "fold")
    );

    public static ServerLevel getLevel(MinecraftServer server) {
        return server.getLevel(FOLD_LEVEL);
    }

    private FoldDimension() {}
}