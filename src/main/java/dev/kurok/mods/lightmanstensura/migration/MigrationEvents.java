package dev.kurok.mods.lightmanstensura.migration;

import dev.kurok.mods.lightmanstensura.LightmansTensura;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Migration triggers. Every retired coin/pouch entering the world through chunk
 * or entity load, and every player joining, is converted; the loaded chunk set is
 * tracked so {@code /coinconvert} can re-sweep containers that were modified
 * after their chunk had already loaded.
 */
public final class MigrationEvents {

    private static final Map<Level, Set<LevelChunk>> LOADED_CHUNKS = new WeakHashMap<>();

    private MigrationEvents() {}

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MigrationService.migratePlayer(player);
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        LOADED_CHUNKS.computeIfAbsent(level, key -> new HashSet<>()).add(chunk);

        int changed = 0;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Container container) {
                BlockPos pos = blockEntity.getBlockPos();
                changed += MigrationService.migrateContainer(container,
                        stack -> MigrationService.spawnAt(level, pos, stack));
            }
        }
        if (changed > 0) {
            LightmansTensura.LOGGER.debug(
                    "Converted {} retired coin/pouch stack(s) in chunk at {}", changed, chunk.getPos());
        }
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Set<LevelChunk> chunks = LOADED_CHUNKS.get(level);
        if (chunks != null && event.getChunk() instanceof LevelChunk chunk) {
            chunks.remove(chunk);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MigrationService.migrateEntity(event.getEntity(), serverLevel);
    }

    /**
     * Re-sweeps a loaded dimension: every tracked chunk's container block entities
     * plus every loaded entity. Used by {@code /coinconvert}.
     */
    public static int sweepLevel(ServerLevel level) {
        int changed = 0;
        Set<LevelChunk> chunks = LOADED_CHUNKS.get(level);
        if (chunks != null) {
            for (LevelChunk chunk : new ArrayList<>(chunks)) {
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container container) {
                        BlockPos pos = blockEntity.getBlockPos();
                        changed += MigrationService.migrateContainer(container,
                                stack -> MigrationService.spawnAt(level, pos, stack));
                    }
                }
            }
        }
        // snapshot first: converting may spawn drop entities, mutating the live iterable
        List<Entity> entities = new ArrayList<>();
        level.getAllEntities().forEach(entities::add);
        for (Entity entity : entities) {
            changed += MigrationService.migrateEntity(entity, level);
        }
        return changed;
    }
}
