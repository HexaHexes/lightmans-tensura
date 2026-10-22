package org.bitwisemadness.lightmanstensura.economy;

import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Converts a single item stack out of Tensura's retired coin/pouch economy.
 *
 * <p>Coins are swapped 1:1 by tier (splitting if the replacement's max stack size
 * is smaller), pouches become wallets with their stored coins transferred into the
 * wallet's storage, and non-retired stacks get their embedded contents (bundle /
 * shulker contents) converted in place so nothing that is already stored is missed.
 */
public final class StackConversion {

    /** Shulker boxes always expose 27 slots regardless of how full the stack is. */
    private static final int SHULKER_SLOTS = 27;

    /**
     * @param primary replacement for the input stack; the SAME instance when nothing changed
     * @param extra   produced stacks that no longer fit the original slot (coins split past
     *                max stack size, pouch contents beyond wallet capacity)
     * @param changed whether anything was converted at all
     */
    public record ConversionResult(ItemStack primary, List<ItemStack> extra, boolean changed) {}

    private StackConversion() {}

    public static ConversionResult convert(ItemStack stack) {
        if ( stack.isEmpty()) {
            return new ConversionResult(stack, List.of(), false);
        }
        Item source = stack.getItem();
        Item target = CoinMap.mapItem(source);
        if (target == source) {
            return convertContained(stack);
        }
        if (CoinMap.isPouch(source)) {
            return convertPouch(stack, target);
        }
        return splitCoin(stack, target);
    }

    /**
     * Trade-path conversion: a merchant offer holds a single result stack, so any
     * overflow is folded back in instead of being handed out separately. Coin
     * splits become one oversized stack (vanilla carries such stacks in NBT fine);
     * pouch overflow is appended to the wallet's storage.
     */
    public static ItemStack convertTrade(ItemStack stack) {
        ConversionResult result = convert(stack);
        if (result.extra().isEmpty()) {
            return result.primary();
        }
        ItemStack primary = result.primary();
        if (primary.getItem() instanceof WalletItem) {
            WalletFiller.appendAll(primary, result.extra());
            return primary;
        }
        int total = primary.getCount();
        for (ItemStack overflow : result.extra()) {
            total += overflow.getCount();
        }
        primary.setCount(total);
        return primary;
    }

    /** Not a retired item itself: convert whatever retired items it stores. */
    private static ConversionResult convertContained(ItemStack stack) {
        List<ItemStack> extras = new ArrayList<>();
        boolean changed = false;

        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) {
            List<ItemStack> converted = new ArrayList<>();
            boolean any = false;
            for (ItemStack inner : bundle.itemsCopy()) {
                ConversionResult result = convert(inner);
                // bundles are weight limited, not slot limited: inner splits stay inside
                converted.add(result.primary());
                converted.addAll(result.extra());
                if (result.changed()) {
                    any = true;
                }
            }
            if (any) {
                stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(mergeStacks(converted)));
                changed = true;
            }
        }

        ItemContainerContents shulker = stack.get(DataComponents.CONTAINER);
        if (shulker != null && shulker.getSlots() > 0) {
            List<ItemStack> converted = new ArrayList<>();
            boolean any = false;
            for (int i = 0; i < shulker.getSlots(); i++) {
                ItemStack inner = shulker.getStackInSlot(i);
                if (inner.isEmpty()) {
                    continue;
                }
                ConversionResult result = convert(inner);
                converted.add(result.primary());
                converted.addAll(result.extra());
                if (result.changed()) {
                    any = true;
                }
            }
            if (any) {
                List<ItemStack> merged = mergeStacks(converted);
                if (merged.size() > SHULKER_SLOTS) {
                    extras.addAll(new ArrayList<>(merged.subList(SHULKER_SLOTS, merged.size())));
                    merged = new ArrayList<>(merged.subList(0, SHULKER_SLOTS));
                }
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(merged));
                changed = true;
            }
        }

        return new ConversionResult(stack, extras, changed);
    }

    private static ConversionResult convertPouch(ItemStack stack, Item walletItem) {
        ItemStack wallet = new ItemStack(walletItem, 1);
        wallet.applyComponents(stack.getComponentsPatch());
        wallet.remove(DataComponents.BUNDLE_CONTENTS);

        List<ItemStack> contents = new ArrayList<>();
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStack inner : bundle.itemsCopy()) {
                ConversionResult result = convert(inner);
                contents.add(result.primary());
                contents.addAll(result.extra());
            }
        }
        List<ItemStack> overflow = WalletFiller.fill(wallet, contents);
        return new ConversionResult(wallet, overflow, true);
    }

    private static ConversionResult splitCoin(ItemStack stack, Item target) {
        int max = new ItemStack(target).getMaxStackSize();
        int remaining = stack.getCount();
        List<ItemStack> parts = new ArrayList<>();
        while (remaining > 0) {
            int put = Math.min(remaining, max);
            ItemStack part = new ItemStack(target, put);
            part.applyComponents(stack.getComponentsPatch());
            parts.add(part);
            remaining -= put;
        }
        List<ItemStack> extra = parts.size() > 1 ? new ArrayList<>(parts.subList(1, parts.size())) : List.of();
        return new ConversionResult(parts.get(0), extra, true);
    }

    private static List<ItemStack> mergeStacks(List<ItemStack> input) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : input) {
            if (stack == null || stack.isEmpty() || out.contains(stack)) {
                continue;
            }
            int remaining = stack.getCount();
            for (ItemStack merged : out) {
                if (remaining == 0) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(merged, stack)) {
                    int move = Math.min(merged.getMaxStackSize() - merged.getCount(), remaining);
                    if (move > 0) {
                        merged.grow(move);
                        remaining -= move;
                    }
                }
            }
            if (remaining > 0) {
                out.add(remaining == stack.getCount() ? stack : stack.copyWithCount(remaining));
            }
        }
        return out;
    }
}
