package com.bgame.fold.content;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class FoldBlocks {

    public static final BlockBehaviour.Properties THRESHOLD_FRAME_PROPS =
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .isSuffocating((s, r, p) -> false)
                    .isViewBlocking((s, r, p) -> false);

    public static ThresholdFrameBlock createThresholdFrame() {
        return new ThresholdFrameBlock(THRESHOLD_FRAME_PROPS);
    }

    private FoldBlocks() {}
}