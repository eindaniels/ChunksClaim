package de.eindaniel.chunksClaim.commands;

import de.eindaniel.chunksClaim.ChunksClaim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Untrust extends Command {
    final ChunksClaim plugin;
    public Untrust(ChunksClaim plugin) {
        super("untrust");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        plugin.handleUntrust(player, args);
        return false;
    }
}
