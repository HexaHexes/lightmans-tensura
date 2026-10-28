package org.bitwisemadness.lightmanstensura.gametest;

import com.mojang.authlib.GameProfile;
import dev.gearevo.gearevolution.GearEvolutionServices;
import dev.gearevo.gearevolution.chain.ChainEvolutionResolver;
import org.bitwisemadness.lightmanstensura.economy.StackConversion;
import org.bitwisemadness.lightmanstensura.economy.StackConversion.ConversionResult;
import org.bitwisemadness.lightmanstensura.migration.MigrationService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-repo runtime coverage for the pack conversion. Run with {@code gradle runGameTestServer}:
 * NeoForge's patched server main boots a headless GameTestServer in this project's
 * {@code run/} folder with the pack's dependency mods staged from the {@code gametestMods} configuration
 * into {@code run/mods/} - no CurseForge instance is involved.
 *
 * <p>The suite proves the datapack override order (tags, disabled recipes,
 * advancement overrides, wallet gear_existence files), the {@link
 * net.minecraft.world.item.trading.MerchantOffer} conversion mixin, all three
 * migration surfaces (entity join, containers, player) and the {@code /coinconvert}
 * command at runtime. These classes are inert in production: NeoForge only registers
 * {@code @GameTestHolder} classes when {@code -Dneoforge.gameTestServer} is set.
 */
@GameTestHolder("lightmans_tensura")
@PrefixGameTestTemplate(false)
public class EconomyGameTests {

    // ------------------------------------------------------------------ tools

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .orElseThrow(() -> new GameTestAssertException("missing item: " + id));
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /**
     * A detached ServerPlayer, constructed directly: the helper's
     * {@code makeMockServerPlayerInLevel} runs a real placeNewPlayer, which makes
     * pack mods fire login and chunk-sync payloads the gametest connection rejects
     * (Lightman's Currency and ManasCore both crash on it). migratePlayer only
     * touches the inventories, so no level membership or connection is needed.
     */
    private static ServerPlayer detachedPlayer(GameTestHelper helper) {
        return new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "LTGameTest"),
                ClientInformation.createDefault());
    }

    private static Set<String> tagKeys(String tagId) {
        var holders = BuiltInRegistries.ITEM
                .getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId)))
                .orElseThrow(() -> new GameTestAssertException("missing item tag: " + tagId));
        Set<String> keys = new TreeSet<>();
        for (Holder<Item> holder : holders) {
            keys.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString());
        }
        return keys;
    }

    // ---------------------------------------------------- datapack override order

    /**
     * {@code data/tensura/tags/item/coins.json} (replace:true) must win over
     * Tensura's own pack: exactly Lightman's Currency main-chain coins.
     */
    @GameTest(template = "empty")
    public static void coinsTagContainsLightmansCoins(GameTestHelper helper) {
        Set<String> want = new TreeSet<>(List.of(
                "lightmanscurrency:coin_copper",
                "lightmanscurrency:coin_iron",
                "lightmanscurrency:coin_gold",
                "lightmanscurrency:coin_emerald",
                "lightmanscurrency:coin_diamond",
                "lightmanscurrency:coin_netherite"));
        Set<String> got = tagKeys("tensura:coins");
        require(got.equals(want), "tensura:coins tag is " + got);
        helper.succeed();
    }

    /** The env tag keeps Tensura's entries and adds all nine wallets. */
    @GameTest(template = "empty")
    public static void envTagAddsWallets(GameTestHelper helper) {
        Set<String> got = tagKeys("minecraft:indestructible_by_environmental_cause");
        require(got.contains("lightmanscurrency:wallet_leather"), "wallet_leather missing from env tag");
        require(got.contains("lightmanscurrency:wallet_ender_dragon"), "wallet_ender_dragon missing from env tag");
        require(got.contains("tensura:pouch_special_a"), "original tensura entries lost from env tag");
        require(got.contains("tensura:moonlight"), "original tensura:moonlight lost from env tag");
        helper.succeed();
    }

    /** The five pouch recipes carry {@code neoforge:false} and must not exist. */
    @GameTest(template = "empty")
    public static void pouchRecipesDisabled(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String id : List.of("tensura:pouch_d", "tensura:pouch_c", "tensura:pouch_b",
                "tensura:pouch_a", "tensura:pouch_special_a")) {
            require(recipes.byKey(ResourceLocation.parse(id)).isEmpty(), "recipe still enabled: " + id);
        }
        require(recipes.getRecipeIds().count() > 100,
                "recipe manager nearly empty (" + recipes.getRecipeIds().count() + " recipes)");
        helper.succeed();
    }

    /**
     * Our three advancement overrides parse and load; the disabled pouch recipe
     * advancements are gone along with their recipes.
     */
    @GameTest(template = "empty")
    public static void advancementOverridesLoad(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String id : List.of("tensura:delightful_trade", "tensura:million_dollar", "tensura:my_precious")) {
            require(advancements.get(ResourceLocation.parse(id)) != null, "advancement missing: " + id);
        }
        for (String id : List.of("tensura:recipes/misc/pouch_d", "tensura:recipes/misc/pouch_a")) {
            require(advancements.get(ResourceLocation.parse(id)) == null, "disabled recipe advancement present: " + id);
        }
        helper.succeed();
    }

    /**
     * Wallet tiers must be registered with Gear Evolution through {@code addon_chains}
     * (GE's mechanism for non-builtin items - its gear_existence directory only ever
     * accepts items with a hardcoded builtin chain). Asserts the tier ladder
     * (minEP/maxEP) at Gear Evolution's canonical material progression values -
     * leather 550, chainmail 1350, iron 3200, gold 5000, diamond 7800, netherite
     * 17500, then the magisteel tail 18000/52000 - the climb rates (flat 0.003
     * through netherite, then the tail's 0.01/0.02), every evolution link, and one
     * live step through GE's resolver.
     */
    @GameTest(template = "empty")
    public static void walletGearEvolutionChain(GameTestHelper helper) {
        record Tier(String file, String item, int minEp, int maxEp, double epGain, String evolution) {}
        List<Tier> tiers = List.of(
                new Tier("wallet_leather", "lightmanscurrency:wallet_leather", 0, 550, 0.003,
                        "lightmanscurrency:wallet_copper"),
                new Tier("wallet_copper", "lightmanscurrency:wallet_copper", 550, 1350, 0.003,
                        "lightmanscurrency:wallet_iron"),
                new Tier("wallet_iron", "lightmanscurrency:wallet_iron", 1350, 3200, 0.003,
                        "lightmanscurrency:wallet_gold"),
                new Tier("wallet_gold", "lightmanscurrency:wallet_gold", 3200, 5000, 0.003,
                        "lightmanscurrency:wallet_emerald"),
                new Tier("wallet_emerald", "lightmanscurrency:wallet_emerald", 5000, 6400, 0.003,
                        "lightmanscurrency:wallet_diamond"),
                new Tier("wallet_diamond", "lightmanscurrency:wallet_diamond", 6400, 7800, 0.003,
                        "lightmanscurrency:wallet_netherite"),
                new Tier("wallet_netherite", "lightmanscurrency:wallet_netherite", 7800, 17500, 0.003,
                        "lightmanscurrency:wallet_nether_star"),
                new Tier("wallet_nether_star", "lightmanscurrency:wallet_nether_star", 17500, 18000, 0.01,
                        "lightmanscurrency:wallet_ender_dragon"),
                new Tier("wallet_ender_dragon", "lightmanscurrency:wallet_ender_dragon", 18000, 52000, 0.02,
                        null));

        var chains = GearEvolutionServices.addonChains();
        for (Tier tier : tiers) {
            String where = tier.item();
            var entry = chains.get(ResourceLocation.parse(tier.item()))
                    .orElseThrow(() -> new GameTestAssertException("addon chain missing: " + where));
            require(entry.item().toString().equals(tier.item()),
                    where + " registers item " + entry.item());
            require(entry.minEP() == tier.minEp(), where + " minEP=" + entry.minEP() + " expected " + tier.minEp());
            require(entry.maxEP() == tier.maxEp(), where + " maxEP=" + entry.maxEP() + " expected " + tier.maxEp());
            require(Math.abs(entry.epGain() - tier.epGain()) < 1.0E-9,
                    where + " epGain=" + entry.epGain() + " expected " + tier.epGain());
            if (tier.evolution() == null) {
                require(entry.evolution() == null, where + " should be terminal but evolves to " + entry.evolution());
            } else {
                require(entry.evolution() != null && entry.evolution().toString().equals(tier.evolution()),
                        where + " evolves to " + entry.evolution() + " expected " + tier.evolution());
            }
        }

        // Live resolver walk: leather's next tier resolves through the addon chain.
        ResourceLocation next = ChainEvolutionResolver.resolveNext(
                ResourceLocation.parse("lightmanscurrency:wallet_leather"));
        require(next.toString().equals("lightmanscurrency:wallet_copper"),
                "resolver walks leather to " + next + " expected lightmanscurrency:wallet_copper");
        helper.succeed();
    }

    // ---------------------------------------------------------- MerchantOffer mixin

    /** A public MerchantOffer constructor funnelled into the mixin: old buy cost. */
    @GameTest(template = "empty")
    public static void tradeBuyCostConverted(GameTestHelper helper) {
        MerchantOffer offer = new MerchantOffer(
                new ItemCost(item("tensura:bronze_coin"), 3),
                new ItemStack(item("minecraft:emerald")),
                5, 2, 1.0F);
        String cost = BuiltInRegistries.ITEM.getKey(offer.getItemCostA().item().value()).toString();
        require(cost.equals("lightmanscurrency:coin_copper"), "buy cost not converted: " + cost);
        require(offer.getItemCostA().count() == 3, "cost count changed: " + offer.getItemCostA().count());
        require(offer.getResult().getItem() == item("minecraft:emerald"), "vanilla result changed");
        helper.succeed();
    }

    /** Secondary cost and the villager's result stack convert too. */
    @GameTest(template = "empty")
    public static void tradeSecondCostAndResultConverted(GameTestHelper helper) {
        MerchantOffer offer = new MerchantOffer(
                new ItemCost(item("tensura:gold_coin"), 1),
                Optional.of(new ItemCost(item("tensura:silver_coin"), 2)),
                new ItemStack(item("tensura:stellar_gold_coin"), 1),
                5, 0, 1, 1.0F, 2);
        String costA = BuiltInRegistries.ITEM.getKey(offer.getItemCostA().item().value()).toString();
        String costB = offer.getItemCostB()
                .map(c -> BuiltInRegistries.ITEM.getKey(c.item().value()).toString())
                .orElse("<none>");
        require(costA.equals("lightmanscurrency:coin_gold"), "cost A not converted: " + costA);
        require(costB.equals("lightmanscurrency:coin_iron"), "cost B not converted: " + costB);
        require(key(offer.getResult()).equals("lightmanscurrency:coin_emerald"),
                "result not converted: " + key(offer.getResult()));
        helper.succeed();
    }

    /** Vanilla inputs stay untouched, and copy() (private copy ctor) keeps conversions. */
    @GameTest(template = "empty")
    public static void tradeIdentityAndCopyPreserved(GameTestHelper helper) {
        MerchantOffer plain = new MerchantOffer(
                new ItemCost(item("minecraft:emerald"), 2),
                new ItemStack(item("minecraft:diamond")),
                5, 1, 1.0F);
        require(key(plain.getItemCostA().item().value()).equals("minecraft:emerald"), "vanilla cost changed");
        require(key(plain.getResult()).equals("minecraft:diamond"), "vanilla result changed");
        require(key(plain.copy().getResult()).equals("minecraft:diamond"), "copy changed a vanilla offer");

        MerchantOffer old = new MerchantOffer(
                new ItemCost(item("tensura:bronze_coin"), 1),
                new ItemStack(item("tensura:silver_coin")),
                5, 1, 1.0F);
        MerchantOffer copy = old.copy();
        String copyCost = BuiltInRegistries.ITEM.getKey(copy.getItemCostA().item().value()).toString();
        require(copyCost.equals("lightmanscurrency:coin_copper"), "copy lost cost conversion: " + copyCost);
        require(key(copy.getResult()).equals("lightmanscurrency:coin_iron"),
                "copy lost result conversion: " + key(copy.getResult()));
        helper.succeed();
    }

    // -------------------------------------------------------------- StackConversion

    /** 1:1 tier swap preserves total count even past max stack size; pouches become wallets. */
    @GameTest(template = "empty")
    public static void stackConversionPreservesCounts(GameTestHelper helper) {
        ConversionResult big = StackConversion.convert(new ItemStack(item("tensura:bronze_coin"), 150));
        require(big.changed(), "large coin stack reported unchanged");
        int total = big.primary().getCount();
        for (ItemStack extra : big.extra()) {
            total += extra.getCount();
        }
        require(total == 150, "count lost: " + total);
        require(key(big.primary()).equals("lightmanscurrency:coin_copper"),
                "wrong replacement: " + key(big.primary()));

        ConversionResult pouch = StackConversion.convert(new ItemStack(item("tensura:pouch_d"), 1));
        require(pouch.changed(), "pouch reported unchanged");
        require(key(pouch.primary()).equals("lightmanscurrency:wallet_leather"),
                "pouch replacement: " + key(pouch.primary()));

        ConversionResult vanilla = StackConversion.convert(new ItemStack(item("minecraft:emerald"), 3));
        require(!vanilla.changed() && vanilla.primary().getItem() == item("minecraft:emerald"),
                "vanilla stack was converted");
        helper.succeed();
    }

    // ------------------------------------------------------------------ migration

    /** EntityJoinLevelEvent converts a dropped retired coin the moment it appears. */
    @GameTest(template = "empty")
    public static void droppedCoinConvertsOnJoin(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 4, 3));
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                new ItemStack(item("tensura:bronze_coin"), 10));
        level.addFreshEntity(entity);
        require(key(entity.getItem()).equals("lightmanscurrency:coin_copper"),
                "dropped coin not converted: " + key(entity.getItem()));
        require(entity.getItem().getCount() == 10, "count changed: " + entity.getItem().getCount());
        helper.succeed();
    }

    /** Container entities (chest minecart) convert through migrateEntity. */
    @GameTest(template = "empty")
    public static void chestMinecartContentsConvert(GameTestHelper helper) {
        var level = helper.getLevel();
        var cart = helper.spawn(net.minecraft.world.entity.EntityType.CHEST_MINECART, new BlockPos(4, 2, 4));
        cart.setItem(0, new ItemStack(item("tensura:silver_coin"), 64));
        int converted = MigrationService.migrateEntity(cart, level);
        require(converted == 1, "expected 1 converted stack, got " + converted);
        require(key(cart.getItem(0)).equals("lightmanscurrency:coin_iron"),
                "minecart stack not converted: " + key(cart.getItem(0)));
        require(cart.getItem(0).getCount() == 64, "count changed: " + cart.getItem(0).getCount());
        helper.succeed();
    }

    /** Login path: inventory (coins + pouches) and ender chest, state asserted after migratePlayer. */
    @GameTest(template = "empty")
    public static void playerInventoryConvertsInPlace(GameTestHelper helper) {
        ServerPlayer player = detachedPlayer(helper);
        player.getInventory().add(new ItemStack(item("tensura:bronze_coin"), 7));
        player.getInventory().add(new ItemStack(item("tensura:pouch_d"), 1));
        player.getEnderChestInventory().setItem(0, new ItemStack(item("tensura:pouch_c"), 1));

        MigrationService.migratePlayer(player);

        int copper = 0;
        boolean wallet = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = key(stack);
            if (id.equals("lightmanscurrency:coin_copper")) {
                copper += stack.getCount();
            }
            if (id.equals("lightmanscurrency:wallet_leather")) {
                wallet = true;
            }
            require(!id.startsWith("tensura:"), "inventory still holds retired item: " + id);
        }
        require(copper == 7, "copper total is " + copper);
        require(wallet, "wallet_leather missing from inventory");
        String ender = key(player.getEnderChestInventory().getItem(0));
        require(ender.equals("lightmanscurrency:wallet_copper"), "ender chest holds: " + ender);
        helper.succeed();
    }

    /** Non-retired stacks (LC wallets, vanilla items) pass through untouched. */
    @GameTest(template = "empty")
    public static void nonRetiredStacksUntouched(GameTestHelper helper) {
        ServerPlayer player = detachedPlayer(helper);
        player.getInventory().add(new ItemStack(item("lightmanscurrency:wallet_leather"), 1));
        player.getInventory().add(new ItemStack(item("minecraft:emerald"), 5));
        int converted = MigrationService.migratePlayer(player);
        require(converted == 0, "expected nothing to convert, got " + converted);
        int wallets = 0;
        int emeralds = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (key(stack).equals("lightmanscurrency:wallet_leather")) {
                wallets += stack.getCount();
            }
            if (key(stack).equals("minecraft:emerald")) {
                emeralds += stack.getCount();
            }
        }
        require(wallets == 1 && emeralds == 5, "stacks altered: wallets=" + wallets + " emeralds=" + emeralds);
        helper.succeed();
    }

    /** /coinconvert (permission 2, driven from the console source) sweeps loaded chunk containers. */
    @GameTest(template = "empty")
    public static void coinconvertSweepsLoadedChunks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2); // helper positions are structure-relative
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(item("tensura:gold_coin"), 4));
        require(key(chest.getItem(0)).equals("tensura:gold_coin"), "chest precondition failed");

        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "/coinconvert");

        String id = key(chest.getItem(0));
        require(id.equals("lightmanscurrency:coin_gold"), "chest not swept by /coinconvert: " + id);
        require(chest.getItem(0).getCount() == 4, "count changed: " + chest.getItem(0).getCount());
        helper.succeed();
    }
}
