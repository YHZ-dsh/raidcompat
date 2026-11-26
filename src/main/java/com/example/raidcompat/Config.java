package com.example.raidcompat;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All knobs are here so the search can be tuned without rebuilding.
 * Config file: {@code config/raidcompat-common.toml}
 */
public final class Config {

    public static final ModConfigSpec SPEC;

    /** Master switch; set false to fall back to vanilla 1.21.1 behaviour. */
    public static final ModConfigSpec.BooleanValue ENABLED;

    /** 1.21.2 rule: spawn positions must be within this many blocks vertically of the raid center. */
    public static final ModConfigSpec.IntValue MAX_VERTICAL_DELTA;

    /** Horizontal search radius, centered on the raid center. 1.21.2 uses 96. */
    public static final ModConfigSpec.IntValue MAX_HORIZONTAL_RADIUS;

    /**
     * Number of distance steps. 1.21.2 spreads its checks over the 300 tick wave cooldown
     * (8 checks every 5 ticks = 480 checks) and moves from far to near as the cooldown drains.
     * We reproduce the same distribution in one pass: steps x attemptsPerStep = 480 by default.
     */
    public static final ModConfigSpec.IntValue SEARCH_STEPS;

    /** Checks per distance step. */
    public static final ModConfigSpec.IntValue ATTEMPTS_PER_STEP;

    /** Log a warning when a whole wave cannot find any spawn position. */
    public static final ModConfigSpec.BooleanValue LOG_MISSES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("1.21.2 raid spawn search backport").push("raid");

        ENABLED = builder
                .comment("Use the 1.21.2 spawn search instead of the vanilla 1.21.1 one.")
                .define("enabled", true);

        MAX_VERTICAL_DELTA = builder
                .comment("Maximum vertical distance between a spawn position and the raid center.",
                         "1.21.2 uses 96. Lower this if raiders still appear on far-away surfaces.")
                .defineInRange("maxVerticalDelta", 96, 1, 384);

        MAX_HORIZONTAL_RADIUS = builder
                .comment("Horizontal radius searched around the raid center.")
                .defineInRange("maxHorizontalRadius", 96, 8, 256);

        SEARCH_STEPS = builder
                .comment("Distance steps, far to near.")
                .defineInRange("searchSteps", 60, 1, 600);

        ATTEMPTS_PER_STEP = builder
                .comment("Random position checks per distance step. steps x attempts should stay near 480",
                         "to match the 1.21.2 amount of work.")
                .defineInRange("attemptsPerStep", 8, 1, 64);

        LOG_MISSES = builder
                .comment("Log when a wave cannot find any valid spawn position.")
                .define("logFailedSearches", false);

        builder.pop();

        SPEC = builder.build();
    }

    private Config() {
    }
}