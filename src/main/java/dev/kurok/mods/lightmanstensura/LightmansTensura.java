package dev.kurok.mods.lightmanstensura;

import com.mojang.logging.LogUtils;
import dev.kurok.mods.lightmanstensura.migration.CoinConvertCommand;
import dev.kurok.mods.lightmanstensura.migration.MigrationEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Replaces Tensura's coin/pouch economy with Lightman's Currency.
 *
 * <p>All behaviour lives in the {@code MerchantOffer} trade mixin (vanilla funnel),
 * the optional TATA mixins (string-targeted, plugin gated), the migration event
 * handlers and the bundled datapack under {@code data/}.
 */
@Mod(LightmansTensura.MODID)
public final class LightmansTensura {

    public static final String MODID = "lightmans_tensura";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LightmansTensura(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(CoinConvertCommand::onRegisterCommands);
    }
}
