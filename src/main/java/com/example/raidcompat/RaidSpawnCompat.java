package com.example.raidcompat;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The 1.21.2 raid spawn-position search, reimplemented on top of 1.21.1 APIs.
 *
 * <p>Vanilla 1.21.2 rules this class mirrors:
 * <ul>
 *   <li>positions are checked from far away to close to the raid center,</li>
 *   <li>the vertical distance to the raid center must be &lt; 96 blocks,</li>
 *   <li>only the topmost non-air block of a column can be used,</li>
 *   <li>the chunk must be generated and entity-ticking,</li>
 *   <li>the block underneath must be a valid spawn surface (glass, leaves, ... do not count),</li>
 *   <li>two blocks of air are needed above it.</li>
 * </ul>
 *
 * <p>Known simplifications vs. real 1.21.2:
 * <ul>
 *   <li>the search runs in one pass instead of being spread over the 300 tick cooldown
 *       (the sampled distribution of positions is the same),</li>
 *   <li>positions inside a village subchunk are not explicitly rejected,</li>
 *   <li>the last-resort groups (6 x 20 checks at distance 0) are folded into the final steps.</li>
 * </ul>
 */
public final class RaidSpawnCompat {

    private static final double TWO_PI = Math.PI * 2.0D;

    private RaidSpawnCompat() {
    }

    /**
     * @param level  level the raid is ticking in
     * @param center current raid center
     * @param random level random
     * @return a valid spawn position, or empty if the whole search failed
     */
    public static Optional<BlockPos> findSpawnPos(ServerLevel level, BlockPos center, RandomSource random) {
        final int maxVertical = Config.MAX_VERTICAL_DELTA.get();
        final int maxRadius = Config.MAX_HORIZONTAL_RADIUS.get();
        final int steps = Config.SEARCH_STEPS.get();
        final int attemptsPerStep = Config.ATTEMPTS_PER_STEP.get();

        for (int step = 0; step < steps; step++) {
            // 1.21.2 reduces the checked distance as the wave cooldown drains: far first, near last.
            double radius = maxRadius * (1.0D - (double) step / (double) steps);

            for (int attempt = 0; attempt < attemptsPerStep; attempt++) {
                double angle = random.nextDouble() * TWO_PI;
                int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
                int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);

                BlockPos candidate = topmostNonAir(level, x, z);
                if (candidate == null) {
                    continue;
                }
                if (Math.abs(candidate.getY() - center.getY()) >= maxVertical) {
                    continue;
                }
                if (!isUsable(level, candidate)) {
                    continue;
                }
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * The y coordinate of the first air block above the topmost non-air block of the column,
     * i.e. where a raider would stand. Returns null when the chunk is not loaded.
     */
    private static BlockPos topmostNonAir(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean isUsable(ServerLevel level, BlockPos pos) {
        // Entity-ticking chunk only: raid spawns never happen in non-ticking chunks.
        if (!level.isPositionEntityTicking(pos)) {
            return false;
        }

        BlockState floor = level.getBlockState(pos.below());
        if (floor.isAir()) {
            return false;
        }
        // Glass and leaves are not valid spawn surfaces in 1.21.2.
        if (floor.is(BlockTags.LEAVES) || floor.is(Blocks.GLASS) || floor.is(Blocks.TINTED_GLASS)) {
            return false;
        }
        if (!floor.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }

        // A raider needs two blocks of air.
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}