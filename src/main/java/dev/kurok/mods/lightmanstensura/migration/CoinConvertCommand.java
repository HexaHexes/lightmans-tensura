package dev.kurok.mods.lightmanstensura.migration;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /coinconvert} (permission level 2): sweeps everything currently loaded -
 * all online players' inventories and ender chests, every tracked loaded chunk's
 * container block entities and every loaded entity in every dimension.
 */
public final class CoinConvertCommand {

    private CoinConvertCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("coinconvert")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context.getSource())));
    }

    private static int run(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int changed = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            changed += MigrationService.migratePlayer(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            changed += MigrationEvents.sweepLevel(level);
        }

        int count = changed;
        source.sendSuccess(() -> Component.literal("Converted " + count + " retired coin/pouch stack(s)."), false);
        return Command.SINGLE_SUCCESS;
    }
}
