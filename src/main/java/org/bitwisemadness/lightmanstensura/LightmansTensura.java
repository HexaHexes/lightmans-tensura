package org.bitwisemadness.lightmanstensura;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.bitwisemadness.lightmanstensura.datagen.EconomyPackContent;
import org.bitwisemadness.lightmanstensura.datagen.JsonFilesProvider;
import org.bitwisemadness.lightmanstensura.migration.CoinConvertCommand;
import org.bitwisemadness.lightmanstensura.migration.MigrationEvents;
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
        modBus.addListener(LightmansTensura::gatherData);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(MigrationEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(CoinConvertCommand::onRegisterCommands);
    }

    /** Feeds the shipped economy datapack ({@link EconomyPackContent}) into datagen. */
    private static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        event.addProvider(new JsonFilesProvider("economy_pack",
                event.getGenerator().getPackOutput(), EconomyPackContent.build()));
    }
}
