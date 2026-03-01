package com.bgame.fold.world.fold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class RoomTeleporter {

    public static final String DEFAULT_TEMPLATE = "fold_dimensions:room_basic";
    private static final int LOOP_THRESHOLD = 3;

    public static void crossThreshold(ServerPlayer player, ServerLevel foldLevel,
                                      RoomId currentId, RoomInstance.Direction direction,
                                      long[] playerPath) {
        RoomStorage storage = RoomStorage.get(foldLevel);
        RoomInstance currentRoom = storage.getRoom(currentId);
        if (currentRoom == null) {
            System.err.println("[FoldDimensions] crossThreshold: celda no encontrada: " + currentId);
            return;
        }

        RoomId destId = resolveDestination(storage, currentRoom, direction, playerPath);
        RoomInstance destRoom = storage.getOrCreate(destId, () -> new RoomInstance(destId));

        if (!destRoom.isGenerated()) {
            generateRoomBlocks(foldLevel, destRoom);
            destRoom.markGenerated();
            storage.setDirty();
        }

        ensureBackLink(storage, destRoom, direction.opposite(), currentId);

        BlockPos center = destRoom.getCenter();
        player.teleportTo(foldLevel,
                center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    private static RoomId resolveDestination(RoomStorage storage, RoomInstance current,
                                             RoomInstance.Direction direction, long[] playerPath) {
        if (current.hasConnection(direction)) return current.getConnection(direction);

        long globalSeed = storage.getGlobalSeed();
        long destIndex  = hashDestination(current.id.graphIndex, direction, globalSeed);

        if (RoomStorage.hasLoopRepetition(playerPath, LOOP_THRESHOLD))
            destIndex = hashDestination(current.id.graphIndex, direction, globalSeed ^ destIndex ^ 0xBAD100L);

        RoomInstance existing = storage.getRoom(destIndex);
        if (existing != null) {
            current.setConnection(direction, existing.id);
            storage.setDirty();
            return existing.id;
        }

        RoomId newId = new RoomId(destIndex, DEFAULT_TEMPLATE,
                globalSeed ^ destIndex, (int)(destIndex % 4), (destIndex % 7) == 0);
        current.setConnection(direction, newId);
        storage.setDirty();
        return newId;
    }

    private static long hashDestination(long fromIndex, RoomInstance.Direction dir, long globalSeed) {
        long h = fromIndex ^ globalSeed ^ ((long) dir.ordinal() * 0x9E3779B97F4A7C15L);
        h ^= h >>> 30; h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27; h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return Math.abs(h) % 100_000L;
    }

    private static void ensureBackLink(RoomStorage storage, RoomInstance dest,
                                       RoomInstance.Direction backDir, RoomId sourceId) {
        if (!dest.isAnchored() && !dest.hasConnection(backDir)) {
            dest.setConnection(backDir, sourceId);
            storage.setDirty();
        }
    }

    public static void generateRoomBlocks(ServerLevel level, RoomInstance room) {
        BlockPos origin = room.origin;
        int size = 16;

        // Forzar carga del chunk antes de colocar bloques
        level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);

        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++)
                level.setBlock(origin.offset(x, 0, z),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        for (int x = 0; x < size; x++) {
            level.setBlock(origin.offset(x, 1, 0),        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(x, 1, size - 1), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
        for (int z = 1; z < size - 1; z++) {
            level.setBlock(origin.offset(0,        1, z), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(size - 1, 1, z), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }
}