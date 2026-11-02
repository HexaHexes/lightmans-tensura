package org.bitwisemadness.lightmanstensura.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The economy datapack this mod ships (issue #22), as tables.
 *
 * <p>This class exists only so that {@link JsonFilesProvider} can write the JSON
 * the game actually loads. The runtime never reads it: vanilla consumes the
 * recipes, advancements, loot tables and tags, and Gear Evolution consumes the
 * {@code addon_chains}, which means the generated files in
 * {@code src/generated/resources} <em>are</em> the economy swap rather than a
 * copy of one held in Java. A datapack that omits a file therefore un-does that
 * piece of the swap, instead of a Java fallback quietly restoring it.</p>
 *
 * <p>{@link #build()} closes the loop by validating the built pack against the
 * contract {@code EconomyGameTests} asserts in the headless server - the wallet
 * ladder's hand-offs, the disabled pouch recipes, the coin tag replacement - so
 * a table edit fails the datagen run with a clear message instead of three CI
 * jobs later.</p>
 */
public final class EconomyPackContent {

    /** Where the wallet chains ship, inside the jar (GE reads addon_chains in any namespace). */
    private static final String WALLET_DIRECTORY = "data/lightmans_tensura/addon_chains";

    /** Same-path overrides that retire Tensura's own pouch recipes. */
    private static final String POUCH_RECIPE_DIRECTORY = "data/tensura/recipe";

    /** Same-path overrides that retire the pouch recipes' unlock advancements. */
    private static final String POUCH_ADVANCEMENT_DIRECTORY = "data/tensura/advancement/recipes/misc";

    /** New advancements slotted into Tensura's advancement tree. */
    private static final String ADVANCEMENT_DIRECTORY = "data/tensura/advancement";

    /** Same-path overrides that pay Tensura's chest loot in Lightman's coins. */
    private static final String LOOT_DIRECTORY = "data/tensura/loot_table/chests";

    /** Replaces Tensura's {@code tensura:coins} tag with Lightman's coins. */
    private static final String COINS_TAG_PATH = "data/tensura/tags/item/coins.json";

    /** Appends the wallets to the environmental-indestructibility tag. */
    private static final String ENV_TAG_PATH = "data/minecraft/tags/item/indestructible_by_environmental_cause.json";

    /**
     * The wallet ladder at Gear Evolution's canonical material progression:
     * leather 550, copper 1350, iron 3200, gold 5000, diamond 6400 (the
     * emerald rung spans the wallets' own band), netherite 7800, then the
     * magisteel tail 18000/52000. The climb rate is flat 0.003 through
     * netherite, then the tail's 0.01/0.02.
     */
    private static final List<WalletTier> LADDER = List.of(
            new WalletTier("wallet_leather", "lightmanscurrency:wallet_leather",
                    0, 550, 0.003, "lightmanscurrency:wallet_copper"),
            new WalletTier("wallet_copper", "lightmanscurrency:wallet_copper",
                    550, 1350, 0.003, "lightmanscurrency:wallet_iron"),
            new WalletTier("wallet_iron", "lightmanscurrency:wallet_iron",
                    1350, 3200, 0.003, "lightmanscurrency:wallet_gold"),
            new WalletTier("wallet_gold", "lightmanscurrency:wallet_gold",
                    3200, 5000, 0.003, "lightmanscurrency:wallet_emerald"),
            new WalletTier("wallet_emerald", "lightmanscurrency:wallet_emerald",
                    5000, 6400, 0.003, "lightmanscurrency:wallet_diamond"),
            new WalletTier("wallet_diamond", "lightmanscurrency:wallet_diamond",
                    6400, 7800, 0.003, "lightmanscurrency:wallet_netherite"),
            new WalletTier("wallet_netherite", "lightmanscurrency:wallet_netherite",
                    7800, 17500, 0.003, "lightmanscurrency:wallet_nether_star"),
            new WalletTier("wallet_nether_star", "lightmanscurrency:wallet_nether_star",
                    17500, 18000, 0.01, "lightmanscurrency:wallet_ender_dragon"),
            new WalletTier("wallet_ender_dragon", "lightmanscurrency:wallet_ender_dragon",
                    18000, 52000, 0.02, null));

    /**
     * The five pouches the pack retires, each paired with the monster leather
     * its disabled recipe and unlock advancement used to demand.
     */
    private static final List<PouchRetirement> POUCHES = List.of(
            new PouchRetirement("pouch_a", "tensura:monster_leather_a"),
            new PouchRetirement("pouch_b", "tensura:monster_leather_b"),
            new PouchRetirement("pouch_c", "tensura:monster_leather_c"),
            new PouchRetirement("pouch_d", "tensura:monster_leather_d"),
            new PouchRetirement("pouch_special_a", "tensura:monster_leather_special_a"));

    /** The two coin-collecting advancements that hang off delightful_trade. */
    private static final List<CoinAdvancement> COIN_ADVANCEMENTS = List.of(
            new CoinAdvancement("my_precious", "tensura:delightful_trade",
                    "lightmanscurrency:coin_gold", false),
            new CoinAdvancement("million_dollar", "tensura:my_precious",
                    "lightmanscurrency:coin_emerald", true));

    private static final List<LootTableFile> LOOT_TABLES = List.of(
            new LootTableFile("dwarf_merchant", List.of(
                    new LootPool(0.0, 3.0, 16.0, List.of(
                            new LootEntry("minecraft:leather", 35, null),
                            new LootEntry("minecraft:book", 35, null),
                            new LootEntry("minecraft:paper", 35, null),
                            new LootEntry("minecraft:writable_book", 35, null),
                            new LootEntry("minecraft:book", 25,
                                    new EnchantWithLevels(5.0, "#minecraft:on_random_loot")),
                            new LootEntry("minecraft:air", 15, null))),
                    new LootPool(0.0, 0.0, 8.0, List.of(
                            new LootEntry("lightmanscurrency:coin_copper", 30, null),
                            new LootEntry("lightmanscurrency:coin_iron", 10, null),
                            new LootEntry("minecraft:air", 60, null))),
                    new LootPool(0.0, 3.0, 20.0, List.of(
                            new LootEntry("minecraft:copper_ingot", 50, null),
                            new LootEntry("minecraft:iron_nugget", 25, null),
                            new LootEntry("minecraft:flint", 25, null),
                            new LootEntry("minecraft:stick", 50, null),
                            new LootEntry("tensura:thatch", 50, null),
                            new LootEntry("minecraft:wheat", 50, null),
                            new LootEntry("minecraft:air", 25, null))))),
            new LootTableFile("lizardman_throne", List.of(
                    new LootPool(0.0, 2.0, 5.0, List.of(
                            new LootEntry("minecraft:mangrove_roots", null, null))),
                    new LootPool(0.0, 1.0, 4.0, List.of(
                            new LootEntry("tensura:thatch", null, null))),
                    new LootPool(0.0, 1.0, 4.0, List.of(
                            new LootEntry("minecraft:lily_pad", 50, null),
                            new LootEntry("minecraft:muddy_mangrove_roots", 30, null))),
                    new LootPool(0.0, 3.0, 8.0, List.of(
                            new LootEntry("minecraft:cod", 35, null),
                            new LootEntry("minecraft:salmon", 35, null))),
                    new LootPool(0.0, 1.0, 4.0, List.of(
                            new LootEntry("lightmanscurrency:coin_gold", null, null),
                            new LootEntry("lightmanscurrency:coin_iron", 10, null),
                            new LootEntry("lightmanscurrency:coin_copper", 30, null),
                            new LootEntry("minecraft:diamond", 40, null),
                            new LootEntry("minecraft:emerald", 50, null),
                            new LootEntry("minecraft:air", 75, null))),
                    new LootPool(0.0, 1.0, 3.0, List.of(
                            new LootEntry("tensura:dragon_essence", 10, null),
                            new LootEntry("tensura:spear_schematic", 25, null),
                            new LootEntry("minecraft:trident", 30, null),
                            new LootEntry("minecraft:golden_helmet", 50, null),
                            new LootEntry("minecraft:air", 75, null))))));

    private EconomyPackContent() {
    }

    /** Every file of the shipped pack, keyed by its path inside the jar. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (WalletTier tier : LADDER) {
            files.put(WALLET_DIRECTORY + "/" + tier.file() + ".json", walletJson(tier));
        }
        for (PouchRetirement pouch : POUCHES) {
            files.put(POUCH_RECIPE_DIRECTORY + "/" + pouch.pouch() + ".json", pouchRecipeJson(pouch));
            files.put(POUCH_ADVANCEMENT_DIRECTORY + "/" + pouch.pouch() + ".json",
                    pouchAdvancementJson(pouch));
        }
        files.put(ADVANCEMENT_DIRECTORY + "/delightful_trade.json", delightfulTradeJson());
        for (CoinAdvancement advancement : COIN_ADVANCEMENTS) {
            files.put(ADVANCEMENT_DIRECTORY + "/" + advancement.file() + ".json",
                    coinAdvancementJson(advancement));
        }
        for (LootTableFile table : LOOT_TABLES) {
            files.put(LOOT_DIRECTORY + "/" + table.file() + ".json", lootTableJson(table));
        }
        files.put(COINS_TAG_PATH, coinsTagJson());
        files.put(ENV_TAG_PATH, envTagJson());
        validate(files);
        return files;
    }

    /**
     * One wallet ladder rung: the file name, the wallet item, its EP band and
     * climb rate, and the tier it evolves into - {@code null} on the top rung,
     * which is terminal.
     */
    private record WalletTier(String file, String item, int minEp, int maxEp, double epGain,
            String evolution) {
    }

    /**
     * One retired pouch: its id suffix under {@code tensura:} and the monster
     * leather its disabled recipe and unlock advancement used to demand.
     */
    private record PouchRetirement(String pouch, String leather) {
    }

    /**
     * One coin-collecting advancement: the file name, the advancement it hangs
     * off, the coin its criteria and icon demand, and whether it is hidden
     * ({@code delightful_trade} is written separately - its trigger is
     * Tensura's trade event, not an inventory change).
     */
    private record CoinAdvancement(String file, String parent, String icon, boolean hidden) {
    }

    /** An {@code enchant_with_levels} function on a loot entry. */
    private record EnchantWithLevels(double levels, String options) {
    }

    /**
     * One loot entry: the item, its weight ({@code null} omits the key, which
     * loot tables read as weight 1), and an optional enchant function.
     */
    private record LootEntry(String item, Integer weight, EnchantWithLevels enchant) {
    }

    /** One loot pool: uniform rolls between the bounds, over the entries. */
    private record LootPool(double bonusRolls, double rollMin, double rollMax, List<LootEntry> entries) {
    }

    /** One chest loot table override; its random sequence is derived from the name. */
    private record LootTableFile(String file, List<LootPool> pools) {
    }

    private static JsonObject walletJson(WalletTier tier) {
        JsonObject json = new JsonObject();
        json.addProperty("item", tier.item());
        json.addProperty("epGain", tier.epGain());
        json.addProperty("maxEP", tier.maxEp());
        json.addProperty("minEP", tier.minEp());
        if (tier.evolution() != null) {
            json.addProperty("evolution", tier.evolution());
        }
        return json;
    }

    private static JsonObject pouchRecipeJson(PouchRetirement pouch) {
        JsonObject json = new JsonObject();
        json.add("neoforge:conditions", falseCondition());
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "misc");
        json.addProperty("group", "pouches");
        JsonObject key = new JsonObject();
        JsonObject leather = new JsonObject();
        leather.addProperty("item", pouch.leather());
        key.add("L", leather);
        JsonObject string = new JsonObject();
        string.addProperty("item", "minecraft:string");
        key.add("S", string);
        json.add("key", key);
        JsonArray pattern = new JsonArray();
        pattern.add(" S ");
        pattern.add("L L");
        pattern.add(" L ");
        json.add("pattern", pattern);
        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", "tensura:" + pouch.pouch());
        json.add("result", result);
        return json;
    }

    private static JsonObject pouchAdvancementJson(PouchRetirement pouch) {
        String leatherName = pouch.leather().substring(pouch.leather().indexOf(':') + 1);
        JsonObject json = new JsonObject();
        json.add("neoforge:conditions", falseCondition());
        json.addProperty("parent", "minecraft:recipes/root");
        JsonObject criteria = new JsonObject();
        criteria.add("has_" + leatherName, inventoryChangedCriteria(pouch.leather()));
        criteria.add("has_string", inventoryChangedCriteria("minecraft:string"));
        criteria.add("has_the_recipe", recipeUnlockedCriteria("tensura:" + pouch.pouch()));
        json.add("criteria", criteria);
        json.add("requirements", requirements("has_the_recipe", "has_string", "has_" + leatherName));
        JsonObject rewards = new JsonObject();
        JsonArray recipes = new JsonArray();
        recipes.add("tensura:" + pouch.pouch());
        rewards.add("recipes", recipes);
        json.add("rewards", rewards);
        return json;
    }

    private static JsonObject delightfulTradeJson() {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "tensura:reincarnated");
        JsonObject criteria = new JsonObject();
        JsonObject trade = new JsonObject();
        JsonObject conditions = new JsonObject();
        JsonObject item = new JsonObject();
        item.addProperty("items", "#tensura:coins");
        conditions.add("item", item);
        trade.add("conditions", conditions);
        trade.addProperty("trigger", "tensura:trade");
        criteria.add("trade", trade);
        json.add("criteria", criteria);
        json.add("display", display("delightful_trade", "lightmanscurrency:coin_copper", null, false));
        json.add("requirements", requirements("trade"));
        json.addProperty("sends_telemetry_event", true);
        return json;
    }

    private static JsonObject coinAdvancementJson(CoinAdvancement advancement) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", advancement.parent());
        JsonObject criteria = new JsonObject();
        criteria.add("coin", inventoryChangedCriteria(advancement.icon()));
        json.add("criteria", criteria);
        json.add("display", display(advancement.file(), advancement.icon(), "goal", advancement.hidden()));
        json.add("requirements", requirements("coin"));
        json.addProperty("sends_telemetry_event", true);
        return json;
    }

    private static JsonObject display(String file, String icon, String frame, boolean hidden) {
        JsonObject display = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("translate", "tensura.advancements." + file + ".description");
        display.add("description", description);
        if (frame != null) {
            display.addProperty("frame", frame);
        }
        if (hidden) {
            display.addProperty("hidden", true);
        }
        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("count", 1);
        iconJson.addProperty("id", icon);
        display.add("icon", iconJson);
        JsonObject title = new JsonObject();
        title.addProperty("translate", "tensura.advancements." + file + ".title");
        display.add("title", title);
        return display;
    }

    private static JsonObject inventoryChangedCriteria(String item) {
        JsonObject criterion = new JsonObject();
        JsonObject conditions = new JsonObject();
        JsonArray items = new JsonArray();
        JsonObject filter = new JsonObject();
        filter.addProperty("items", item);
        items.add(filter);
        conditions.add("items", items);
        criterion.add("conditions", conditions);
        criterion.addProperty("trigger", "minecraft:inventory_changed");
        return criterion;
    }

    private static JsonObject recipeUnlockedCriteria(String recipe) {
        JsonObject criterion = new JsonObject();
        JsonObject conditions = new JsonObject();
        conditions.addProperty("recipe", recipe);
        criterion.add("conditions", conditions);
        criterion.addProperty("trigger", "minecraft:recipe_unlocked");
        return criterion;
    }

    private static JsonArray requirements(String... names) {
        JsonArray row = new JsonArray();
        for (String name : names) {
            row.add(name);
        }
        JsonArray requirements = new JsonArray();
        requirements.add(row);
        return requirements;
    }

    private static JsonArray falseCondition() {
        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:false");
        conditions.add(condition);
        return conditions;
    }

    private static JsonObject lootTableJson(LootTableFile table) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:chest");
        JsonArray pools = new JsonArray();
        for (LootPool pool : table.pools()) {
            pools.add(poolJson(pool));
        }
        json.add("pools", pools);
        json.addProperty("random_sequence", "tensura:chests/" + table.file());
        return json;
    }

    private static JsonObject poolJson(LootPool pool) {
        JsonObject json = new JsonObject();
        json.addProperty("bonus_rolls", pool.bonusRolls());
        JsonArray entries = new JsonArray();
        for (LootEntry entry : pool.entries()) {
            entries.add(entryJson(entry));
        }
        json.add("entries", entries);
        JsonObject rolls = new JsonObject();
        rolls.addProperty("type", "minecraft:uniform");
        rolls.addProperty("max", pool.rollMax());
        rolls.addProperty("min", pool.rollMin());
        json.add("rolls", rolls);
        return json;
    }

    private static JsonObject entryJson(LootEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:item");
        if (entry.enchant() != null) {
            JsonArray functions = new JsonArray();
            JsonObject function = new JsonObject();
            function.addProperty("function", "minecraft:enchant_with_levels");
            function.addProperty("levels", entry.enchant().levels());
            function.addProperty("options", entry.enchant().options());
            functions.add(function);
            json.add("functions", functions);
        }
        json.addProperty("name", entry.item());
        if (entry.weight() != null) {
            json.addProperty("weight", entry.weight());
        }
        return json;
    }

    private static JsonObject coinsTagJson() {
        JsonObject json = new JsonObject();
        json.addProperty("replace", true);
        json.add("values", values("lightmanscurrency:coin_copper", "lightmanscurrency:coin_iron",
                "lightmanscurrency:coin_gold", "lightmanscurrency:coin_emerald",
                "lightmanscurrency:coin_diamond", "lightmanscurrency:coin_netherite"));
        return json;
    }

    private static JsonObject envTagJson() {
        JsonObject json = new JsonObject();
        json.add("values", values("#tensura:hihiirokane_items",
                "tensura:shadow_storage", "tensura:marionette_heart", "tensura:daemon_essence",
                "tensura:dragon_essence", "tensura:elemental_essence", "tensura:zane_blood",
                "tensura:pouch_special_a", "tensura:moonlight", "tensura:ruhk",
                "tensura:orb_of_domination", "lightmanscurrency:wallet_leather",
                "lightmanscurrency:wallet_copper", "lightmanscurrency:wallet_iron",
                "lightmanscurrency:wallet_gold", "lightmanscurrency:wallet_emerald",
                "lightmanscurrency:wallet_diamond", "lightmanscurrency:wallet_netherite",
                "lightmanscurrency:wallet_nether_star", "lightmanscurrency:wallet_ender_dragon"));
        return json;
    }

    private static JsonArray values(String... entries) {
        JsonArray values = new JsonArray();
        for (String entry : entries) {
            values.add(entry);
        }
        return values;
    }

    /** Fails the datagen run when the built pack would not behave as the pack promises. */
    private static void validate(Map<String, JsonObject> files) {
        List<String> errors = new ArrayList<>();
        if (files.size() != 26) {
            errors.add("expected 26 files, built " + files.size());
        }
        validateWalletLadder(errors);
        validatePouchRetirement(files, errors);
        validateAdvancements(files, errors);
        validateLootTables(files, errors);
        validateTags(files, errors);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("the shipped economy datapack is broken: "
                    + String.join("; ", errors));
        }
    }

    /** The ladder must hand off rung to rung and end on a terminal magisteel tail. */
    private static void validateWalletLadder(List<String> errors) {
        for (int index = 0; index < LADDER.size(); index++) {
            WalletTier tier = LADDER.get(index);
            if (!tier.item().startsWith("lightmanscurrency:")) {
                errors.add(tier.item() + ": wallet items must live in Lightman's Currency namespace");
            }
            if (tier.minEp() >= tier.maxEp()) {
                errors.add(tier.item() + ": empty EP band " + tier.minEp() + ".." + tier.maxEp());
            }
            if (tier.epGain() <= 0.0) {
                errors.add(tier.item() + ": epGain must be positive, was " + tier.epGain());
            }
            if (index == LADDER.size() - 1) {
                if (tier.evolution() != null) {
                    errors.add(tier.item() + " is the top rung but still evolves");
                }
                continue;
            }
            WalletTier next = LADDER.get(index + 1);
            if (tier.maxEp() != next.minEp()) {
                errors.add(tier.item() + " hands off at " + tier.maxEp() + " but " + next.item()
                        + " starts at " + next.minEp());
            }
            if (!next.item().equals(tier.evolution())) {
                errors.add(tier.item() + " evolves to " + tier.evolution() + ", expected the next rung "
                        + next.item());
            }
        }
    }

    /** Every retired pouch needs its disabled recipe and its disabled unlock advancement. */
    private static void validatePouchRetirement(Map<String, JsonObject> files, List<String> errors) {
        for (PouchRetirement pouch : POUCHES) {
            JsonObject recipe = files.get(POUCH_RECIPE_DIRECTORY + "/" + pouch.pouch() + ".json");
            if (recipe == null) {
                errors.add(pouch.pouch() + ": recipe file missing");
                continue;
            }
            if (!isDisabledByFalseCondition(recipe)) {
                errors.add(pouch.pouch() + ": recipe must carry the neoforge:false condition");
            }
            JsonElement result = recipe.get("result");
            if (result == null || !result.getAsJsonObject().get("id").getAsString()
                    .equals("tensura:" + pouch.pouch())) {
                errors.add(pouch.pouch() + ": recipe result does not retire tensura:" + pouch.pouch());
            }
            JsonObject advancement = files.get(POUCH_ADVANCEMENT_DIRECTORY + "/" + pouch.pouch() + ".json");
            if (advancement == null) {
                errors.add(pouch.pouch() + ": recipe unlock advancement missing");
            } else if (!isDisabledByFalseCondition(advancement)) {
                errors.add(pouch.pouch() + ": recipe unlock advancement must be disabled");
            }
        }
    }

    /** The three economy advancements must hang off Tensura's tree in order. */
    private static void validateAdvancements(Map<String, JsonObject> files, List<String> errors) {
        Map<String, String> parents = Map.of(
                "delightful_trade", "tensura:reincarnated",
                "my_precious", "tensura:delightful_trade",
                "million_dollar", "tensura:my_precious");
        for (Map.Entry<String, String> advancement : parents.entrySet()) {
            JsonObject json = files.get(ADVANCEMENT_DIRECTORY + "/" + advancement.getKey() + ".json");
            if (json == null || !advancement.getValue().equals(json.get("parent").getAsString())) {
                errors.add(advancement.getKey() + ": must hang off " + advancement.getValue());
            }
        }
    }

    /** Both chest overrides must keep their sequence and pay Lightman's coins. */
    private static void validateLootTables(Map<String, JsonObject> files, List<String> errors) {
        for (LootTableFile table : LOOT_TABLES) {
            JsonObject json = files.get(LOOT_DIRECTORY + "/" + table.file() + ".json");
            if (json == null) {
                errors.add(table.file() + ": loot table missing");
                continue;
            }
            String sequence = json.get("random_sequence").getAsString();
            if (!("tensura:chests/" + table.file()).equals(sequence)) {
                errors.add(table.file() + ": random_sequence is " + sequence);
            }
            boolean paysCoins = false;
            for (LootPool pool : table.pools()) {
                for (LootEntry entry : pool.entries()) {
                    if (entry.item().startsWith("lightmanscurrency:")) {
                        paysCoins = true;
                    }
                }
            }
            if (!paysCoins) {
                errors.add(table.file() + ": the override must pay Lightman's coins");
            }
        }
    }

    /**
     * The tag contract {@code EconomyGameTests} asserts: {@code tensura:coins}
     * replaces Tensura's tag with exactly Lightman's six main-chain coins, and
     * the environmental-cause tag appends every wallet without dropping
     * Tensura's own entries.
     */
    private static void validateTags(Map<String, JsonObject> files, List<String> errors) {
        JsonObject coins = files.get(COINS_TAG_PATH);
        if (coins == null) {
            errors.add("tensura:coins tag missing");
        } else {
            if (!coins.has("replace") || !coins.get("replace").getAsBoolean()) {
                errors.add("tensura:coins must replace Tensura's own tag");
            }
            List<String> want = List.of(
                    "lightmanscurrency:coin_copper", "lightmanscurrency:coin_iron",
                    "lightmanscurrency:coin_gold", "lightmanscurrency:coin_emerald",
                    "lightmanscurrency:coin_diamond", "lightmanscurrency:coin_netherite");
            List<String> got = stringValues(coins);
            if (!got.equals(want)) {
                errors.add("tensura:coins is " + got + ", expected " + want);
            }
        }
        JsonObject env = files.get(ENV_TAG_PATH);
        if (env == null) {
            errors.add("the environmental-cause tag is missing");
        } else {
            if (env.has("replace")) {
                errors.add("the environmental-cause tag must append (no replace key)");
            }
            List<String> got = stringValues(env);
            for (WalletTier tier : LADDER) {
                if (!got.contains(tier.item())) {
                    errors.add(tier.item() + " missing from the environmental-cause tag");
                }
            }
            for (String kept : List.of("tensura:pouch_special_a", "tensura:moonlight")) {
                if (!got.contains(kept)) {
                    errors.add(kept + " lost from the environmental-cause tag");
                }
            }
        }
    }

    private static List<String> stringValues(JsonObject tag) {
        List<String> got = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(value -> got.add(value.getAsString()));
        return got;
    }

    private static boolean isDisabledByFalseCondition(JsonObject json) {
        JsonElement conditions = json.get("neoforge:conditions");
        if (conditions == null || !conditions.isJsonArray()) {
            return false;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            if (condition.isJsonObject() && condition.getAsJsonObject().has("type")
                    && "neoforge:false".equals(condition.getAsJsonObject().get("type").getAsString())) {
                return true;
            }
        }
        return false;
    }
}
