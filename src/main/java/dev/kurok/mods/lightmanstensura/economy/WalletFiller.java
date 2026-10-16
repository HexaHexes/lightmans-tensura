package dev.kurok.mods.lightmanstensura.economy;

import io.github.lightman314.lightmanscurrency.common.core.ModDataComponents;
import io.github.lightman314.lightmanscurrency.common.items.data.WalletData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Writes converted coin stacks into a Lightman's wallet's {@code wallet_data}
 * component. The wallet is filled up to its (component derived) capacity;
 * anything that does not fit is returned as overflow for the caller to place.
 */
public final class WalletFiller {

    private WalletFiller() {}

    /**
     * Fills the wallet with the given coin stacks.
     *
     * @param wallet a fresh wallet stack (no wallet_data component yet is fine)
     * @param coins  converted coin stacks; empty stacks ignored
     * @return stacks that did not fit into the wallet's capacity
     */
    public static List<ItemStack> fill(ItemStack wallet, List<ItemStack> coins) {
        WalletData data = wallet.getOrDefault(ModDataComponents.WALLET_DATA.get(), WalletData.createFor(wallet));
        List<ItemStack> items = new ArrayList<>(data.items());
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack coin : coins) {
            if (coin == null || coin.isEmpty()) {
                continue;
            }
            ItemStack rest = insert(items, coin);
            if (!rest.isEmpty()) {
                overflow.add(rest);
            }
        }
        wallet.set(ModDataComponents.WALLET_DATA.get(), data.withItems(items));
        return overflow;
    }

    /**
     * Trade-path fallback: like {@link #fill} but appends anything beyond capacity
     * to the wallet's item list instead of returning it (a merchant offer result is
     * a single stack, so overflow has nowhere else to go; trade results are always
     * freshly created empty pouches, so this path is never expected to trigger).
     */
    public static void appendAll(ItemStack wallet, List<ItemStack> stacks) {
        WalletData data = wallet.getOrDefault(ModDataComponents.WALLET_DATA.get(), WalletData.createFor(wallet));
        List<ItemStack> items = new ArrayList<>(data.items());
        for (ItemStack coin : stacks) {
            if (coin == null || coin.isEmpty()) {
                continue;
            }
            ItemStack rest = insert(items, coin);
            if (!rest.isEmpty()) {
                items.add(rest);
            }
        }
        wallet.set(ModDataComponents.WALLET_DATA.get(), data.withItems(items));
    }

    private static ItemStack insert(List<ItemStack> items, ItemStack stack) {
        int remaining = stack.getCount();
        // merge into existing stacks of the same item + components
        for (ItemStack slot : items) {
            if (remaining == 0) {
                break;
            }
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining);
                if (move > 0) {
                    slot.grow(move);
                    remaining -= move;
                }
            }
        }
        // then fill empty slots
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            if (items.get(i).isEmpty()) {
                int put = Math.min(remaining, stack.getMaxStackSize());
                items.set(i, stack.copyWithCount(put));
                remaining -= put;
            }
        }
        return remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }
}
