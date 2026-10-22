package org.bitwisemadness.lightmanstensura.mixin;

import org.bitwisemadness.lightmanstensura.economy.CoinMap;
import org.bitwisemadness.lightmanstensura.economy.StackConversion;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Universal trade funnel. Every way a {@link MerchantOffer} can come to exist -
 * the public constructors, {@code CODEC} (saved villagers/merchant NBT),
 * {@code STREAM_CODEC} (network sync) and {@code copy()} - ends up in the private
 * 10-arg constructor, so swapping its three item arguments at HEAD rewrites:
 *
 * <ul>
 *   <li>cost A (ItemCost) - the price in coins becomes the Lightman's coin of the
 *       same tier</li>
 *   <li>cost B (Optional&lt;ItemCost&gt;) - same for the optional second cost</li>
 *   <li>result (ItemStack) - coin payouts become Lightman's coins, pouches for sale
 *       become the matching wallet (contents transferred)</li>
 * </ul>
 *
 * All swaps are identity for anything not in the retired Tensura economy, so
 * other mods' trades are untouched.
 *
 * <p>Handlers are {@code static}: at a constructor's HEAD (before the
 * {@code super()} call) mixin forbids non-static handlers.</p>
 */
@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    private static final String FULL_CTOR =
            "<init>(Lnet/minecraft/world/item/trading/ItemCost;Ljava/util/Optional;Lnet/minecraft/world/item/ItemStack;IIZIIFI)V";

    @ModifyVariable(method = FULL_CTOR, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static ItemCost lightmansTensura$swapBaseCost(ItemCost cost) {
        return CoinMap.swapCost(cost);
    }

    @ModifyVariable(method = FULL_CTOR, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Optional<ItemCost> lightmansTensura$swapCostB(Optional<ItemCost> cost) {
        return cost.map(CoinMap::swapCost);
    }

    @ModifyVariable(method = FULL_CTOR, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static ItemStack lightmansTensura$swapResult(ItemStack result) {
        return StackConversion.convertTrade(result);
    }
}
