package com.bgame.fold.forge;

import com.bgame.fold.FoldCommon;
import com.bgame.fold.content.FoldBlocks;
import com.bgame.fold.content.ThresholdFrameBlock;
import com.bgame.fold.world.FoldDimension;
import com.bgame.fold.world.fold.RoomStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(FoldCommon.MOD_ID)
public class FoldForge {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FoldCommon.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FoldCommon.MOD_ID);

    public static final RegistryObject<Block> THRESHOLD_FRAME =
            BLOCKS.register("threshold_frame", FoldBlocks::createThresholdFrame);
    public static final RegistryObject<Item> THRESHOLD_FRAME_ITEM =
            ITEMS.register("threshold_frame",
                    () -> new BlockItem(THRESHOLD_FRAME.get(), new Item.Properties()));

    public FoldForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FoldCommon.init();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);

        ThresholdFrameBlock.crossDimensionTeleporter = (player, targetLevel) -> {
            double[] dest = ThresholdFrameBlock.pendingDestPos.get();
            if (dest == null) return;
            ThresholdFrameBlock.pendingDestPos.remove();

            double x = dest[0], y = dest[1], z = dest[2];
            int chunkX = (int)x >> 4;
            int chunkZ = (int)z >> 4;
            net.minecraft.world.level.ChunkPos chunkPos =
                    new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);

            // Forzar carga del chunk y generar bloques
            targetLevel.getChunkSource().addRegionTicket(
                    net.minecraft.server.level.TicketType.FORCED, chunkPos, 2, chunkPos);
            targetLevel.getChunk(chunkX, chunkZ);

            final com.bgame.fold.world.fold.RoomStorage storage =
                    com.bgame.fold.world.fold.RoomStorage.get(targetLevel);
            com.bgame.fold.world.fold.RoomInstance spawnRoom = storage.getRoom(0L);
            if (spawnRoom != null && !spawnRoom.isGenerated()) {
                com.bgame.fold.world.fold.RoomTeleporter.generateRoomBlocks(targetLevel, spawnRoom);
                spawnRoom.markGenerated();
                storage.setDirty();
            }

            ITeleporter teleporter = new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                                java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    return new PortalInfo(new Vec3(x, y, z), Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }

                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld,
                                          ServerLevel destWorld, float yaw,
                                          java.util.function.Function<Boolean, Entity> repositionEntity) {
                    destWorld.getChunk(chunkX, chunkZ);
                    return repositionEntity.apply(false);
                }
            };

            player.changeDimension(targetLevel, teleporter);

            targetLevel.getChunkSource().removeRegionTicket(
                    net.minecraft.server.level.TicketType.FORCED, chunkPos, 2, chunkPos);
        };
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel foldLevel = FoldDimension.getLevel(event.getServer());
        if (foldLevel == null) return;
        RoomStorage.get(foldLevel).tick();
    }
}