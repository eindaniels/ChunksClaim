package de.eindaniel.chunksClaim.commands;

import de.eindaniel.chunksClaim.ChunksClaim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveClaim extends Command {
    final ChunksClaim plugin;
    public RemoveClaim(ChunksClaim plugin) {
        super("removeclaim");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        plugin.handleRemoveClaim(player);
        return false;
    }
}
