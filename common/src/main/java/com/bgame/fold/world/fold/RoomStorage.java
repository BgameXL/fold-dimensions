package com.bgame.fold.world.fold;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RoomStorage extends SavedData {

    private static final String DATA_NAME = "fold_dimensions_rooms";
    private static final float  ECO_DECAY = 0.0005f;

    private final Map<Long, RoomInstance> rooms = new HashMap<>();
    private long nextGraphIndex = 0L;
    private long globalSeed;

    public static RoomStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RoomStorage::load, RoomStorage::createFresh, DATA_NAME);
    }

    private static RoomStorage createFresh() {
        RoomStorage s = new RoomStorage();
        s.globalSeed = System.nanoTime() ^ 0xDEADBEEFCAFEL;
        return s;
    }

    private static RoomStorage load(CompoundTag tag) {
        RoomStorage s = new RoomStorage();
        s.globalSeed     = tag.getLong("globalSeed");
        s.nextGraphIndex = tag.getLong("nextGraphIndex");
        ListTag list = tag.getList("rooms", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            RoomInstance inst = RoomInstance.fromNbt(list.getCompound(i));
            s.rooms.put(inst.id.graphIndex, inst);
        }
        return s;
    }

    public RoomInstance getRoom(long graphIndex) { return rooms.get(graphIndex); }
    public RoomInstance getRoom(RoomId id)       { return rooms.get(id.graphIndex); }

    public RoomInstance getOrCreate(RoomId id, Supplier<RoomInstance> factory) {
        RoomInstance existing = rooms.get(id.graphIndex);
        if (existing != null) return existing;
        RoomInstance created = factory.get();
        addRoom(created);
        return created;
    }

    public void addRoom(RoomInstance inst) {
        if (rooms.containsKey(inst.id.graphIndex))
            throw new IllegalStateException("Celda ya existe: " + inst.id.graphIndex);
        rooms.put(inst.id.graphIndex, inst);
        setDirty();
    }

    public long allocateIndex() {
        long idx = nextGraphIndex++;
        setDirty();
        return idx;
    }

    public Collection<RoomInstance> allRooms() { return Collections.unmodifiableCollection(rooms.values()); }
    public int  roomCount()     { return rooms.size(); }
    public long getGlobalSeed() { return globalSeed; }

    public void tick() {
        boolean dirty = false;
        for (RoomInstance r : rooms.values()) {
            if (r.getEchoLevel() > 0f) { r.decayEcho(ECO_DECAY); dirty = true; }
        }
        if (dirty) setDirty();
    }

    public static boolean hasLoopRepetition(long[] path, int threshold) {
        if (path.length < threshold) return false;
        long last = path[path.length - 1];
        int count = 0;
        for (int i = path.length - 1; i >= 0; i--) {
            if (path[i] == last) count++; else break;
        }
        return count >= threshold;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("globalSeed",     globalSeed);
        tag.putLong("nextGraphIndex", nextGraphIndex);
        ListTag list = new ListTag();
        rooms.values().forEach(inst -> list.add(inst.toNbt()));
        tag.put("rooms", list);
        return tag;
    }
}