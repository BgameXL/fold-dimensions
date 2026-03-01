package com.bgame.fold.content;

import com.bgame.fold.world.FoldDimension;
import com.bgame.fold.world.fold.RoomId;
import com.bgame.fold.world.fold.RoomInstance;
import com.bgame.fold.world.fold.RoomStorage;
import com.bgame.fold.world.fold.RoomTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ThresholdFrameBlock extends Block {

    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long>   lastTeleport = new ConcurrentHashMap<>();
    private static final Map<UUID, long[]> playerPaths  = new ConcurrentHashMap<>();
    private static final int PATH_HISTORY_SIZE = 5;

    /**
     * Callback inyectado por el loader (Forge o Fabric) para el teleport cross-dimensión.
     * Forge necesita ITeleporter; Fabric puede usar FTB Utilities o su propia API.
     * Se registra en FoldForge.init() / FoldFabric.onInitialize().
     *
     * BiConsumer<ServerPlayer, ServerLevel> donde:
     *   - ServerPlayer: jugador a teletransportar
     *   - ServerLevel:  dimensión destino (el jugador ya sabe su posición objetivo via pendingPos)
     */
    public static BiConsumer<ServerPlayer, ServerLevel> crossDimensionTeleporter = null;

    /**
     * Posición destino pendiente — se fija antes de llamar al teleporter
     * para que el callback loader-específico sepa dónde colocar al jugador.
     */
    public static final ThreadLocal<double[]> pendingDestPos = new ThreadLocal<>();

    public ThresholdFrameBlock(Properties properties) {
        super(properties);
    }

    // Forma visual — un marco de 2px en los bordes del bloque completo
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),   // base
            Block.box(0, 14, 0, 16, 16, 16),  // techo
            Block.box(0, 2, 0, 2, 14, 16),    // lado izquierdo
            Block.box(14, 2, 0, 16, 14, 16)   // lado derecho
    );

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Sin colisión física — el jugador pasa a través y entityInside se dispara
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        ServerLevel serverLevel = (ServerLevel) level;

        long now  = serverLevel.getGameTime();
        Long last = lastTeleport.get(player.getUUID());
        if (last != null && now - last < TELEPORT_COOLDOWN_TICKS) return;
        lastTeleport.put(player.getUUID(), now);

        if (serverLevel.dimension().equals(FoldDimension.FOLD_LEVEL)) {
            handleTeleportInFold(player, serverLevel);
        } else {
            handleEnterFold(player, serverLevel);
        }
    }

    // -------------------------------------------------------------------------
    // Entrada a la dimensión fold desde el overworld
    // -------------------------------------------------------------------------

    private void handleEnterFold(ServerPlayer player, ServerLevel currentLevel) {
        ServerLevel foldLevel = FoldDimension.getLevel(currentLevel.getServer());
        if (foldLevel == null) {
            System.err.println("[FoldDimensions] ERROR: Dimensión fold no encontrada.");
            return;
        }
        if (crossDimensionTeleporter == null) {
            System.err.println("[FoldDimensions] ERROR: crossDimensionTeleporter es null.");
            return;
        }

        RoomStorage storage = RoomStorage.get(foldLevel);
        System.out.println("[FoldDimensions] roomCount=" + storage.roomCount());

        if (storage.roomCount() == 0) {
            System.out.println("[FoldDimensions] Creando spawn room...");
            long idx = storage.allocateIndex();
            RoomId spawnId = new RoomId(idx, RoomTeleporter.DEFAULT_TEMPLATE,
                    storage.getGlobalSeed(), RoomId.ROT_0, false);
            storage.addRoom(new RoomInstance(spawnId));
        }

        RoomInstance spawnRoom = storage.getRoom(0L);
        if (spawnRoom == null) {
            System.err.println("[FoldDimensions] ERROR: spawnRoom es null después de crearla.");
            return;
        }

        // Los bloques se generan en FoldForge tras el teleport (delayed 2 ticks)
        // para que el chunk esté cargado por la presencia del jugador

        BlockPos center = spawnRoom.getCenter();
        System.out.println("[FoldDimensions] Teleportando a " + center);
        pendingDestPos.set(new double[]{center.getX() + 0.5, center.getY(), center.getZ() + 0.5});
        crossDimensionTeleporter.accept(player, foldLevel);
    }

    // -------------------------------------------------------------------------
    // Movimiento entre celdas dentro de la dimensión fold
    // -------------------------------------------------------------------------

    private void handleTeleportInFold(ServerPlayer player, ServerLevel foldLevel) {
        RoomStorage storage = RoomStorage.get(foldLevel);
        RoomId currentId = findRoomAtPos(storage, player.blockPosition());
        if (currentId == null) {
            System.err.println("[FoldDimensions] Jugador fuera de celda: " + player.blockPosition());
            return;
        }
        long[] path = playerPaths.getOrDefault(player.getUUID(), new long[0]);
        RoomTeleporter.crossThreshold(player, foldLevel, currentId, RoomInstance.Direction.NORTH, path);
        updatePath(player, currentId.graphIndex);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RoomId findRoomAtPos(RoomStorage storage, BlockPos pos) {
        long est = Math.max(0, pos.getX() / RoomInstance.CELL_SPAN);
        for (long idx = est - 1; idx <= est + 1; idx++) {
            if (idx < 0) continue;
            RoomInstance room = storage.getRoom(idx);
            if (room == null) continue;
            BlockPos o = room.origin;
            if (pos.getX() >= o.getX() && pos.getX() < o.getX() + RoomInstance.CELL_SPAN &&
                    pos.getZ() >= o.getZ() && pos.getZ() < o.getZ() + RoomInstance.CELL_SPAN)
                return room.id;
        }
        return null;
    }

    private static void updatePath(ServerPlayer player, long newIndex) {
        long[] cur = playerPaths.getOrDefault(player.getUUID(), new long[0]);
        int newSize = Math.min(cur.length + 1, PATH_HISTORY_SIZE);
        long[] updated = new long[newSize];
        System.arraycopy(cur, Math.max(0, cur.length - PATH_HISTORY_SIZE + 1), updated, 0, newSize - 1);
        updated[newSize - 1] = newIndex;
        playerPaths.put(player.getUUID(), updated);
    }
}