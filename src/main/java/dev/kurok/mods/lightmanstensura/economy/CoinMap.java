package dev.kurok.mods.lightmanstensura.economy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.ItemCost;

/**
 * The canonical 9-entry tier swap between Tensura's retired economy and
 * Lightman's Currency. Reused by the trade mixin, the TATA mixins and the
 * migration service so every surface maps identically:
 *
 * <pre>
 * bronze_coin      -&gt; coin_copper     pouch_d          -&gt; wallet_leather
 * silver_coin      -&gt; coin_iron       pouch_c          -&gt; wallet_copper
 * gold_coin        -&gt; coin_gold       pouch_b          -&gt; wallet_iron
 * stellar_gold_coin-&gt; coin_emerald    pouch_a          -&gt; wallet_gold
 *                                    pouch_special_a  -&gt; wallet_netherite
 * </pre>
 */
public final class CoinMap {

    /** Resource id string of the retired Tensura item to the Lightman's Currency replacement. */
    public static final Map<String, String> ID_MAP = Map.ofEntries(
            Map.entry("tensura:bronze_coin", "lightmanscurrency:coin_copper"),
            Map.entry("tensura:silver_coin", "lightmanscurrency:coin_iron"),
            Map.entry("tensura:gold_coin", "lightmanscurrency:coin_gold"),
            Map.entry("tensura:stellar_gold_coin", "lightmanscurrency:coin_emerald"),
            Map.entry("tensura:pouch_d", "lightmanscurrency:wallet_leather"),
            Map.entry("tensura:pouch_c", "lightmanscurrency:wallet_copper"),
            Map.entry("tensura:pouch_b", "lightmanscurrency:wallet_iron"),
            Map.entry("tensura:pouch_a", "lightmanscurrency:wallet_gold"),
            Map.entry("tensura:pouch_special_a", "lightmanscurrency:wallet_netherite"));

    private static final Set<String> POUCH_IDS = Set.of(
            "tensura:pouch_d",
            "tensura:pouch_c",
            "tensura:pouch_b",
            "tensura:pouch_a",
            "tensura:pouch_special_a");

    private static final Map<Item, Item> ITEM_CACHE = new ConcurrentHashMap<>();

    private CoinMap() {}

    /** Maps any resource id; unknown ids pass through unchanged. */
    public static String mapId(String id) {
        return ID_MAP.getOrDefault(id, id);
    }

    /**
     * Maps an item instance to its replacement. Identity for everything that is not
     * a retired Tensura coin/pouch. Built lazily (first use happens in-game, well
     * after registries are frozen) and cached.
     */
    public static Item mapItem(Item item) {
        if (item == null) {
            return null;
        }
        return ITEM_CACHE.computeIfAbsent(item, source -> {
            String target = ID_MAP.get(BuiltInRegistries.ITEM.getKey(source).toString());
            if (target == null) {
                return source;
            }
            return BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.parse(target))
                    .orElse(source);
        });
    }

    public static boolean isPouch(Item item) {
        return item != null && POUCH_IDS.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    /** Swaps a trade cost's item while preserving count and component predicate. */
    public static ItemCost swapCost(ItemCost cost) {
        if (cost == null) {
            return null;
        }
        Item source = cost.item().value();
        Item target = mapItem(source);
        if (target == source) {
            return cost;
        }
        return new ItemCost(BuiltInRegistries.ITEM.wrapAsHolder(target), cost.count(), cost.components());
    }
}
