package com.bgame.fold.world.fold;

import net.minecraft.nbt.CompoundTag;
import java.util.Objects;

public final class RoomId {

    public static final int ROT_0   = 0;
    public static final int ROT_90  = 1;
    public static final int ROT_180 = 2;
    public static final int ROT_270 = 3;

    public final long        graphIndex;
    public final String      templateId;
    public final long        variationSeed;
    public final int         rotation;
    public final boolean     mirrored;
    public final CompoundTag overrideData; // reservado para Ancla (semana 11)

    public RoomId(long graphIndex, String templateId, long variationSeed,
                  int rotation, boolean mirrored) {
        this(graphIndex, templateId, variationSeed, rotation, mirrored, null);
    }

    public RoomId(long graphIndex, String templateId, long variationSeed,
                  int rotation, boolean mirrored, CompoundTag overrideData) {
        if (rotation < 0 || rotation > 3)
            throw new IllegalArgumentException("rotation debe ser 0-3, recibido: " + rotation);
        this.graphIndex    = graphIndex;
        this.templateId    = Objects.requireNonNull(templateId);
        this.variationSeed = variationSeed;
        this.rotation      = rotation;
        this.mirrored      = mirrored;
        this.overrideData  = overrideData;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("graphIndex",    graphIndex);
        tag.putString("templateId",  templateId);
        tag.putLong("variationSeed", variationSeed);
        tag.putInt("rotation",       rotation);
        tag.putBoolean("mirrored",   mirrored);
        if (overrideData != null) tag.put("overrideData", overrideData);
        return tag;
    }

    public static RoomId fromNbt(CompoundTag tag) {
        return new RoomId(
                tag.getLong("graphIndex"),
                tag.getString("templateId"),
                tag.getLong("variationSeed"),
                tag.getInt("rotation"),
                tag.getBoolean("mirrored"),
                tag.contains("overrideData") ? tag.getCompound("overrideData") : null
        );
    }

    public RoomId withOverride(CompoundTag data) {
        return new RoomId(graphIndex, templateId, variationSeed, rotation, mirrored, data);
    }

    public boolean hasOverride() { return overrideData != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomId r)) return false;
        return graphIndex == r.graphIndex;
    }

    @Override public int hashCode() { return Long.hashCode(graphIndex); }

    @Override
    public String toString() {
        return String.format("RoomId{idx=%d, tpl='%s', rot=%d, mirror=%b}",
                graphIndex, templateId, rotation, mirrored);
    }
}