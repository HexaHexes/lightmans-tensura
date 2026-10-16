package dev.kurok.mods.lightmanstensura.mixin.tata;

import dev.kurok.mods.lightmanstensura.economy.CoinMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * TATA's {@code ContractCatalog} builds every contract in {@code <clinit>} through
 * the private {@code delivery(String, String, int, long, long, int, AdventureRank)}
 * helper; the second argument is the delivery item id (also used as the contract
 * icon). Map it through the shared tier map: {@code tensura:stellar_gold_coin}
 * contracts (e.g. {@code stellar_gold_reserve_supply}) now demand Lightman's
 * emerald coins; every other id passes through unchanged.
 *
 * <p>String-targeted: no compile dependency on TATA; gated by {@link TataMixinPlugin}.
 */
@Mixin(targets = "com.low.tensuraalltheadventure.contract.ContractCatalog")
public abstract class ContractCatalogMixin {

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/low/tensuraalltheadventure/contract/ContractCatalog;delivery(Ljava/lang/String;Ljava/lang/String;IJJILcom/low/tensuraalltheadventure/data/AdventureRank;)Lcom/low/tensuraalltheadventure/contract/ContractDefinition;"),
            index = 1)
    private String lightmansTensura$swapDeliveryItem(String itemId) {
        return CoinMap.mapId(itemId);
    }
}
