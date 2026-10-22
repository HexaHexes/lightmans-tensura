package org.bitwisemadness.lightmanstensura.mixin.tata;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Gates the Tensura: All The Adventure mixins on that mod being installed, so the
 * optional config never fails when TATA is absent.
 *
 * <p>Early-safety matters here: mixin configs are loaded during ModLauncher
 * initialization, potentially before {@link ModList} has been built. The original
 * implementation called {@code ModList.get().isLoaded(...)} and swallowed every
 * throwable into {@code false} - so when the gate ran too early, {@code ModList.get()}
 * returned {@code null}, the NPE was silently eaten, and both mixins were permanently
 * disabled in every environment (no log line, no injection error - just TATA paying
 * the retired tensura coins again). The gate now falls back to the discovery-time
 * {@link LoadingModList}, which exists before {@code ModList} (mixin configs are
 * registered through it), and it logs every decision.
 */
public class TataMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOG = LogManager.getLogger("lightmans_tensura_tata");

    private static boolean modPresent() {
        // Primary: fully-built mod list (correct once mod loading began).
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded("tensura_all_the_adventure");
            }
        } catch (Throwable t) {
            // fall through to the early-safe source
        }
        // Fallback: discovery-time mod list, populated before ModList exists.
        try {
            LoadingModList loading = LoadingModList.get();
            if (loading != null) {
                return loading.getMods().stream()
                        .anyMatch(info -> "tensura_all_the_adventure".equals(info.getModId()));
            }
        } catch (Throwable t) {
            // fall through
        }
        return false;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean apply = modPresent();
        LOG.info("TATA mixin gate: {} for {} (tensura_all_the_adventure {})",
                apply ? "APPLY" : "SKIP", mixinClassName,
                apply ? "present" : "absent or not yet discoverable");
        return apply;
    }

    @Override
    public void onLoad(String mixinPackage) {
        LOG.info("TATA mixin config loaded (package {}); TATA present={}", mixinPackage, modPresent());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public List<String> getMixins() {
        return null;
    }
}
