package com.example.raidcompat;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Backports the Minecraft 1.21.2 raid spawn-position search to 1.21.1.
 *
 * <p>Everything interesting lives in {@link RaidSpawnCompat} (the search itself) and
 * {@code com.example.raidcompat.mixin.RaidMixin} (the hook that swaps it in).
 */
@Mod(RaidCompatMod.MOD_ID)
public final class RaidCompatMod {

    public static final String MOD_ID = "raidcompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RaidCompatMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[raidcompat] 1.21.2 raid spawn search is active on 1.21.1");
    }
}