package dev.kurok.mods.lightmanstensura.mixin.tata;

import dev.kurok.mods.lightmanstensura.economy.CoinMap;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TATA resolves a currency's coin item lazily in {@code MarketCurrency.registeredItem()}
 * via {@code ResourceLocation.tryParse(itemId)}. Redirect that parse through the
 * shared tier map so TATA's market payments, acceptance checks and rendered icons
 * all look for the Lightman's coin of the same tier. The enum's serialized form
 * ({@code serializedName}, {@code bronzeValue}) is deliberately untouched - TATA's
 * 1/100/10^4/10^6 bronze accounting matches the 100:1 Lightman ladder exactly.
 *
 * <p>String-targeted: no compile dependency on TATA; gated by {@link TataMixinPlugin}.
 */
@Mixin(targets = "com.low.tensuraalltheadventure.data.MarketCurrency")
public abstract class MarketCurrencyMixin {

    @Redirect(
            method = "registeredItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;tryParse(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation lightmansTensura$mapCurrencyId(String id) {
        return ResourceLocation.tryParse(CoinMap.mapId(id));
    }
}
