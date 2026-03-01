package com.bgame.fold.fabric;

import com.bgame.fold.FoldCommon;
import com.bgame.fold.content.FoldBlocks;
import com.bgame.fold.content.ThresholdFrameBlock;
import com.bgame.fold.world.FoldDimension;
import com.bgame.fold.world.fold.RoomStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FoldFabric implements ModInitializer {

    public static Block THRESHOLD_FRAME;

    @Override
    public void onInitialize() {
        THRESHOLD_FRAME = Registry.register(
                BuiltInRegistries.BLOCK,
                new ResourceLocation(FoldCommon.MOD_ID, "threshold_frame"),
                FoldBlocks.createThresholdFrame()
        );
        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(FoldCommon.MOD_ID, "threshold_frame"),
                new BlockItem(THRESHOLD_FRAME, new Item.Properties())
        );

        FoldCommon.init();

        // Registrar el teleporter cross-dimensión para Fabric
        // En Fabric, ServerPlayer.teleportTo(ServerLevel, x, y, z, yRot, xRot) sí funciona
        ThresholdFrameBlock.crossDimensionTeleporter = (player, targetLevel) -> {
            double[] dest = ThresholdFrameBlock.pendingDestPos.get();
            if (dest == null) return;
            ThresholdFrameBlock.pendingDestPos.remove();
            player.teleportTo(targetLevel, dest[0], dest[1], dest[2],
                    player.getYRot(), player.getXRot());
        };

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel foldLevel = FoldDimension.getLevel(server);
            if (foldLevel != null) RoomStorage.get(foldLevel).tick();
        });
    }
}