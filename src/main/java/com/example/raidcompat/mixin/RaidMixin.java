package com.example.raidcompat.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.raidcompat.Config;
import com.example.raidcompat.RaidCompatMod;
import com.example.raidcompat.RaidSpawnCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;

/**
 * Replaces {@code Raid#findSpawnPos()} (the vanilla 1.21.1 three-ring search) with the
 * 1.21.2 search implemented in {@link RaidSpawnCompat}.
 *
 * <p>Integration points to verify against your exact NeoForge / mapping version:
 * <ul>
 *   <li>method name {@code findSpawnPos} on {@code net.minecraft.world.entity.raid.Raid},</li>
 *   <li>the private {@code ServerLevel level} field.</li>
 * </ul>
 * See README.md ("如果编译不过") for how to check both in ten seconds.
 */
@Mixin(Raid.class)
public abstract class RaidMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "findSpawnPos", at = @At("HEAD"), cancellable = true)
    private void raidcompat$findSpawnPos(CallbackInfoReturnable<Optional<BlockPos>> cir) {
        if (!Config.ENABLED.get()) {
            return;
        }

        Raid self = (Raid) (Object) this;
        BlockPos center = self.getCenter();
        Optional<BlockPos> pos = RaidSpawnCompat.findSpawnPos(this.level, center, this.level.random);

        if (pos.isEmpty() && Config.LOG_MISSES.get()) {
            RaidCompatMod.LOGGER.warn(
                    "[raidcompat] {}: no valid raid spawn position within {} blocks horizontally / {} vertically of {}",
                    this.level.dimension().location(), Config.MAX_HORIZONTAL_RADIUS.get(),
                    Config.MAX_VERTICAL_DELTA.get(), center);
        }

        cir.setReturnValue(pos);
    }
}