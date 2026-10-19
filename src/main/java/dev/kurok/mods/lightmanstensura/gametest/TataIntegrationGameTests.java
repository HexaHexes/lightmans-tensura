package dev.kurok.mods.lightmanstensura.gametest;

import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime coverage for the optional Tensura: All The Adventure (TATA) mixins.
 * TATA is staged from {@code testmods/} like the rest of the pack's dependency
 * mods; everything below reaches it through reflection so this project keeps
 * zero compile dependency on it - mirroring the string-targeted mixin design.
 *
 * <p>These tests exist because the production failure mode is silent: the
 * {@code TataMixinPlugin} gate swallows every throwable and skips the mixins
 * with no log output, so "contract rewards still pay tensura coins" in-game
 * was indistinguishable from a healthy state. Here the gate either resolves
 * (assertions pass) or the harness debug log shows the config being skipped.
 *
 * <p>Like {@link EconomyGameTests}, these classes are inert in production:
 * NeoForge only registers {@code @GameTestHolder} classes when
 * {@code -Dneoforge.gameTestServer} is set.
 */
@GameTestHolder("lightmans_tensura")
@PrefixGameTestTemplate(false)
public class TataIntegrationGameTests {

    private static final String MARKET_CURRENCY = "com.low.tensuraalltheadventure.data.MarketCurrency";
    private static final String CONTRACT_CATALOG = "com.low.tensuraalltheadventure.contract.ContractCatalog";

    private static Class<?> tataClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new GameTestAssertException("TATA class missing (mod not staged?): " + name + " - " + e);
        }
    }

    private static Object invoke(Object target, String method, Class<?>[] signature, Object... args) {
        try {
            return target.getClass().getMethod(method, signature).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException(
                    "reflection failed on " + target.getClass().getName() + "#" + method + " - " + e);
        }
    }

    private static Object invokeStatic(Class<?> cls, String method, Class<?>[] signature, Object... args) {
        try {
            return cls.getMethod(method, signature).invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection failed on " + cls.getName() + "#" + method + " - " + e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** {@code MarketCurrency.<constant>.registeredItem()} must resolve to the Lightman's coin of the same tier. */
    private static void assertCurrencyMaps(String constant, String expectedItemId) {
        Class<?> currency = tataClass(MARKET_CURRENCY);
        Object enumValue;
        try {
            enumValue = currency.getField(constant).get(null);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("missing MarketCurrency constant: " + constant + " - " + e);
        }
        Object result = invoke(enumValue, "registeredItem", new Class<?>[0]);
        require(result instanceof Optional<?>, constant + ".registeredItem() returned " + result);
        Optional<?> optional = (Optional<?>) result;
        require(optional.isPresent(), constant + ".registeredItem() is empty (item id failed to parse/resolve)");
        require(optional.get() instanceof net.minecraft.world.item.Item,
                constant + ".registeredItem() holds " + optional.get());
        String actual = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey((net.minecraft.world.item.Item) optional.get())
                .toString();
        require(actual.equals(expectedItemId),
                "TATA currency " + constant + " resolves to " + actual + " expected " + expectedItemId);
    }

    /**
     * Hook #1 ({@code MarketCurrencyMixin}): every TATA money surface - contract
     * coin grants, market payments, change, eligibility counts, GUI icons - goes
     * through {@code MarketCurrency.registeredItem()}, so this single redirect
     * decides whether TATA pays in Lightman's coins or retired tensura coins.
     */
    @GameTest(template = "empty")
    public static void tataCurrencyResolvesToLightmansCoins(GameTestHelper helper) {
        assertCurrencyMaps("BRONZE", "lightmanscurrency:coin_copper");
        assertCurrencyMaps("SILVER", "lightmanscurrency:coin_iron");
        assertCurrencyMaps("GOLD", "lightmanscurrency:coin_gold");
        assertCurrencyMaps("STELLAR_GOLD", "lightmanscurrency:coin_emerald");
        helper.succeed();
    }

    /**
     * Hook #2 ({@code ContractCatalogMixin}): the catalog's {@code <clinit>}
     * passes the delivery item id through {@code delivery(String, String, ...)};
     * the stellar-gold contract is the one coin-id delivery in TATA's whole
     * catalog, so it proves the arg swap ran while the class was built.
     */
    @GameTest(template = "empty")
    public static void tataContractDeliveryDemandsLightmansCoins(GameTestHelper helper) {
        Class<?> catalog = tataClass(CONTRACT_CATALOG);
        Object found = invokeStatic(catalog, "find", new Class<?>[] {String.class},
                "stellar_gold_reserve_supply");
        require(found instanceof Optional<?>, "ContractCatalog.find returned " + found);
        Optional<?> optional = (Optional<?>) found;
        require(optional.isPresent(), "contract missing from catalog: stellar_gold_reserve_supply");
        String deliveryId = (String) invoke(optional.get(), "deliveryItemId", new Class<?>[0]);
        require(deliveryId.equals("lightmanscurrency:coin_emerald"),
                "stellar_gold_reserve_supply delivery item is " + deliveryId
                        + " expected lightmanscurrency:coin_emerald (tensura:stellar_gold_coin unmapped)");
        helper.succeed();
    }
}
