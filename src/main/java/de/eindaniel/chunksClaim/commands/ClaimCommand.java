package de.eindaniel.chunksClaim.commands;

import de.eindaniel.chunksClaim.ChunksClaim;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimCommand extends Command {
    final ChunksClaim plugin;

    public ClaimCommand(ChunksClaim plugin) {
        super("claim");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        plugin.handleClaim(player, false);
        return false;
    }
}
