package com.bgame.fold.world.fold;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class RoomInstance {

    public static final int CELL_SPAN = 128;

    public final RoomId   id;
    public final BlockPos origin;
    private boolean generated;

    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN;
        public Direction opposite() {
            return switch (this) {
                case NORTH -> SOUTH; case SOUTH -> NORTH;
                case EAST  -> WEST;  case WEST  -> EAST;
                case UP    -> DOWN;  case DOWN  -> UP;
            };
        }
    }

    private final Map<Direction, RoomId> connections = new EnumMap<>(Direction.class);
    private float   echoLevel;
    private float   stability;
    private boolean anchored;

    public RoomInstance(RoomId id) {
        this.id        = id;
        this.origin    = computeOrigin(id.graphIndex);
        this.generated = false;
        this.echoLevel = 0f;
        this.stability = 1f;
        this.anchored  = false;
    }

    public static BlockPos computeOrigin(long graphIndex) {
        return new BlockPos((int)(graphIndex * CELL_SPAN), 64, 0);
    }

    public BlockPos getCenter() {
        // origin.y = 64 (suelo de piedra)
        // Y=65 es la superficie pisable, el jugador necesita llegar a Y=66 para estar parado
        return origin.offset(CELL_SPAN / 2, 2, CELL_SPAN / 2);
    }

    public void setConnection(Direction dir, RoomId destination) {
        if (anchored) throw new IllegalStateException("Celda anclada: " + id);
        connections.put(dir, destination);
    }

    public RoomId  getConnection(Direction dir)  { return connections.get(dir); }
    public boolean hasConnection(Direction dir)  { return connections.containsKey(dir); }
    public Map<Direction, RoomId> getAllConnections() { return Collections.unmodifiableMap(connections); }

    public boolean isGenerated()  { return generated; }
    public void markGenerated()   { this.generated = true; }

    public float getEchoLevel()   { return echoLevel; }
    public void addEcho(float v)  { echoLevel = Math.min(1f, echoLevel + v); }
    public void decayEcho(float v){ echoLevel = Math.max(0f, echoLevel - v); }

    public float getStability()   { return stability; }
    public void setStability(float s) { stability = Math.max(0f, Math.min(1f, s)); }

    public boolean isAnchored()   { return anchored; }
    public void setAnchored(boolean a) { this.anchored = a; }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("id", id.toNbt());
        tag.putBoolean("generated", generated);
        tag.putFloat("echoLevel",   echoLevel);
        tag.putFloat("stability",   stability);
        tag.putBoolean("anchored",  anchored);
        ListTag list = new ListTag();
        connections.forEach((dir, dest) -> {
            CompoundTag e = new CompoundTag();
            e.putString("dir", dir.name());
            e.put("dest", dest.toNbt());
            list.add(e);
        });
        tag.put("connections", list);
        return tag;
    }

    public static RoomInstance fromNbt(CompoundTag tag) {
        RoomInstance inst = new RoomInstance(RoomId.fromNbt(tag.getCompound("id")));
        inst.generated = tag.getBoolean("generated");
        inst.echoLevel = tag.getFloat("echoLevel");
        inst.stability = tag.getFloat("stability");
        inst.anchored  = tag.getBoolean("anchored");
        ListTag list   = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            inst.connections.put(Direction.valueOf(e.getString("dir")),
                    RoomId.fromNbt(e.getCompound("dest")));
        }
        return inst;
    }

    @Override
    public String toString() {
        return String.format("RoomInstance{%s, origin=%s, gen=%b, eco=%.2f, stab=%.2f}",
                id, origin, generated, echoLevel, stability);
    }
}