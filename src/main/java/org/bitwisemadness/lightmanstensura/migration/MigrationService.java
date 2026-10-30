package org.bitwisemadness.lightmanstensura.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bitwisemadness.lightmanstensura.LightmansTensura;
import org.bitwisemadness.lightmanstensura.economy.StackConversion;
import org.bitwisemadness.lightmanstensura.economy.StackConversion.ConversionResult;

/**
 * Full cleanup of retired coins/pouches:
 *
 * <ul>
 *   <li>player login - inventory (main/armor/offhand) and ender chest</li>
 *   <li>chunk load - every BlockEntity container in the chunk</li>
 *   <li>entity join - dropped item stacks, item frames, container minecarts/boats</li>
 *   <li>{@code /coinconvert} - sweep of everything currently loaded</li>
 * </ul>
 *
 * Pouch contents (bundle) and shulker/bundle nested storage are converted
 * recursively; anything that no longer fits is dropped at the container/player
 * instead of being destroyed.
 */
public final class MigrationService {

    private MigrationService() {}

    /** Converts every slot of a container; overflow is handed to the sink (drop it). */
    public static int migrateContainer(Container container, Consumer<ItemStack> sink) {
        int changed = 0;
        List<ItemStack> extras = new ArrayList<>();
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                continue;
            }
            ConversionResult result = StackConversion.convert(slot);
            if (result.changed()) {
                container.setItem(i, result.primary());
                changed++;
            }
            if (!result.extra().isEmpty()) {
                extras.addAll(result.extra());
            }
        }
        for (ItemStack extra : extras) {
            ItemStack rest = placeInto(container, extra);
            if (!rest.isEmpty()) {
                sink.accept(rest);
            }
        }
        if (changed > 0) {
            container.setChanged();
        }
        return changed;
    }

    /** Converts a player's inventory + ender chest; drops whatever still has no room. */
    public static int migratePlayer(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int changed = 0;
        List<ItemStack> extras = new ArrayList<>();
        changed += convertSlotList(inventory.items, extras);
        changed += convertSlotList(inventory.armor, extras);
        changed += convertSlotList(inventory.offhand, extras);

        List<ItemStack> enderOverflow = new ArrayList<>();
        changed += migrateContainer(player.getEnderChestInventory(), enderOverflow::add);

        List<ItemStack> pending = new ArrayList<>(extras);
        pending.addAll(enderOverflow);
        for (ItemStack stack : pending) {
            ItemStack rest = placeInto(inventory, stack);
            if (rest.isEmpty()) {
                continue;
            }
            rest = placeInto(player.getEnderChestInventory(), rest);
            if (rest.isEmpty()) {
                continue;
            }
            player.drop(rest, false);
        }

        if (changed > 0) {
            LightmansTensura.LOGGER.info(
                    "Converted {} retired coin/pouch stack(s) for {}", changed, player.getGameProfile().getName());
        }
        return changed;
    }

    /** Converts an entity's stored items (drops, frames, container minecarts/boats). */
    public static int migrateEntity(Entity entity, ServerLevel level) {
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                return 0;
            }
            ConversionResult result = StackConversion.convert(stack);
            if (result.changed()) {
                itemEntity.setItem(result.primary());
            }
            spawnExtras(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), result.extra());
            return result.changed() ? 1 : 0;
        }
        if (entity instanceof ItemFrame frame) {
            ItemStack stack = frame.getItem();
            if (stack.isEmpty()) {
                return 0;
            }
            ConversionResult result = StackConversion.convert(stack);
            if (result.changed()) {
                frame.setItem(result.primary());
            }
            spawnExtras(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), result.extra());
            return result.changed() ? 1 : 0;
        }
        if (entity instanceof Container container) {
            double x = entity.getX();
            double y = entity.getY() + 0.5;
            double z = entity.getZ();
            return migrateContainer(container, stack -> spawnAt(level, x, y, z, stack));
        }
        return 0;
    }

    /** Drops a stack into the world. */
    public static void spawnAt(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level, x, y, z, stack));
    }

    public static void spawnAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        spawnAt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    private static void spawnExtras(ServerLevel level, double x, double y, double z, List<ItemStack> extras) {
        for (ItemStack extra : extras) {
            spawnAt(level, x, y, z, extra);
        }
    }

    private static int convertSlotList(List<ItemStack> slots, List<ItemStack> extras) {
        int changed = 0;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty()) {
                continue;
            }
            ConversionResult result = StackConversion.convert(slot);
            if (result.changed()) {
                slots.set(i, result.primary());
                changed++;
            }
            if (!result.extra().isEmpty()) {
                extras.addAll(result.extra());
            }
        }
        return changed;
    }

    /** Merges into matching/empty slots of the container; returns what is left over. */
    public static ItemStack placeInto(Container container, ItemStack stack) {
        int remaining = stack.getCount();
        if (remaining == 0) {
            return ItemStack.EMPTY;
        }
        int size = container.getContainerSize();
        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining);
                if (move > 0) {
                    slot.grow(move);
                    remaining -= move;
                }
            }
        }
        for (int i = 0; i < size && remaining > 0; i++) {
            if (container.getItem(i).isEmpty()) {
                int put = Math.min(remaining, stack.getMaxStackSize());
                container.setItem(i, stack.copyWithCount(put));
                remaining -= put;
            }
        }
        if (remaining == stack.getCount()) {
            return stack;
        }
        container.setChanged();
        return remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }
}
