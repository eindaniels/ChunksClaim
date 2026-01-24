package de.eindaniel.chunksClaim;

import de.eindaniel.chunksClaim.commands.*;
import de.eindaniel.chunksClaim.config.ClaimData;
import de.eindaniel.chunksClaim.config.ClaimStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.aether.spi.connector.Transfer;

import java.util.*;

public final class ChunksClaim extends JavaPlugin implements Listener {

    private Map<String, ClaimData> claims = new HashMap<>();
    private Map<UUID, Integer> playerClaimChunks = new HashMap<>();
    private Map<UUID, Set<UUID>> ownerTrust = new HashMap<>();
    private Map<UUID, String> lastChunk = new HashMap<>();
    private ClaimStorage storage;
    private int defaultClaimChunks = 5;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        defaultClaimChunks = getConfig().getInt("default-claim-chunks", 5);

        getServer().getPluginManager().registerEvents(this, this);
        storage = new ClaimStorage(getDataFolder());
        claims = storage.loadClaims();
        playerClaimChunks = storage.loadPlayerData();
        ownerTrust = storage.loadTrust();

        getLogger().info("ChunkClaims aktiviert! " + claims.size() + " Claims geladen.");
    }

    @Override
    public void onDisable() {
        storage.saveClaims(claims);
        storage.savePlayerData(playerClaimChunks);
        storage.saveTrust(ownerTrust);
        getLogger().info("ChunkClaims deaktiviert!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "claim":
                return handleClaim(player, false);
            case "adminclaim":
                return handleAdminClaim(player);
            case "removeclaim":
                return handleRemoveClaim(player);
            case "claiminfo":
                return handleClaimInfo(player);
            case "deleteallmyclaims":
                return handleDeleteAllMyClaims(player);
            case "giveclaimchunks":
                return handleGiveClaimChunks(player, args);
            case "transferclaim":
                return handleTransferClaim(player, args);
            case "trust":
                return handleTrust(player, args);
            case "untrust":
                return handleUntrust(player, args);
            case "trustlist":
                return handleTrustList(player);
        }

        return false;
    }

    private boolean handleClaim(Player player, boolean admin) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (claims.containsKey(chunkKey)) {
            ClaimData data = claims.get(chunkKey);
            if (data.getOwner().equals(player.getUniqueId())) {
                player.sendMessage("§eDieser Chunk gehört bereits dir!");
            } else {
                player.sendMessage("§cDieser Chunk ist bereits beansprucht!");
            }
            return true;
        }

        if (!admin) {
            int available = getAvailableChunks(player.getUniqueId());
            int used = getUsedChunks(player.getUniqueId());

            if (used >= available) {
                player.sendMessage("§cDu hast keine ClaimChunks mehr! (" + used + "/" + available + ")");
                return true;
            }
        }

        ClaimData data = new ClaimData(player.getUniqueId(), admin);
        claims.put(chunkKey, data);
        storage.saveClaims(claims);

        if (admin) {
            player.sendMessage("§aAdminClaim erfolgreich erstellt! (X: " + chunk.getX() + ", Z: " + chunk.getZ() + ")");
        } else {
            int used = getUsedChunks(player.getUniqueId());
            int available = getAvailableChunks(player.getUniqueId());
            player.sendMessage("§aChunk erfolgreich beansprucht! (" + used + "/" + available + " ClaimChunks verwendet)");
        }
        return true;
    }

    private boolean handleAdminClaim(Player player) {
        if (!player.hasPermission("chunkclaims.admin")) {
            player.sendMessage("§cKeine Berechtigung!");
            return true;
        }
        return handleClaim(player, true);
    }

    private boolean handleRemoveClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            player.sendMessage("§cDieser Chunk ist nicht beansprucht!");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        if (!data.getOwner().equals(player.getUniqueId()) && !player.hasPermission("chunkclaims.admin")) {
            player.sendMessage("§cDieser Chunk gehört dir nicht!");
            return true;
        }

        claims.remove(chunkKey);
        storage.saveClaims(claims);

        int used = getUsedChunks(player.getUniqueId());
        int available = getAvailableChunks(player.getUniqueId());
        player.sendMessage("§aClaim entfernt! (" + used + "/" + available + " ClaimChunks)");
        return true;
    }

    private boolean handleClaimInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            player.sendMessage("§cDieser Chunk ist nicht beansprucht!");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        String ownerName = getServer().getOfflinePlayer(data.getOwner()).getName();

        player.sendMessage("§6=== Claim Info ===");
        player.sendMessage("§eID: §f" + chunkKey);
        player.sendMessage("§eOwner: §f" + ownerName);
        player.sendMessage("§eAdminClaim: §f" + (data.isAdminClaim() ? "Ja" : "Nein"));
        player.sendMessage("§eMin: §f" + (chunk.getX() * 16) + ", " + (chunk.getZ() * 16));
        player.sendMessage("§eMax: §f" + (chunk.getX() * 16 + 15) + ", " + (chunk.getZ() * 16 + 15));
        return true;
    }

    private boolean handleDeleteAllMyClaims(Player player) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, ClaimData> entry : claims.entrySet()) {
            if (entry.getValue().getOwner().equals(player.getUniqueId())) {
                toRemove.add(entry.getKey());
            }
        }

        if (toRemove.isEmpty()) {
            player.sendMessage("§cDu hast keine Claims!");
            return true;
        }

        for (String key : toRemove) {
            claims.remove(key);
        }
        storage.saveClaims(claims);

        int available = getAvailableChunks(player.getUniqueId());
        player.sendMessage("§a" + toRemove.size() + " Claims gelöscht! (" + 0 + "/" + available + " ClaimChunks)");
        return true;
    }

    private boolean handleGiveClaimChunks(Player player, String[] args) {
        if (!player.hasPermission("chunkclaims.admin")) {
            player.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cVerwendung: /giveclaimchunks <Spieler> <Anzahl>");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            int current = playerClaimChunks.getOrDefault(target.getUniqueId(), defaultClaimChunks);
            playerClaimChunks.put(target.getUniqueId(), current + amount);
            storage.savePlayerData(playerClaimChunks);

            player.sendMessage("§a" + target.getName() + " hat jetzt " + (current + amount) + " ClaimChunks!");
            target.sendMessage("§aDu hast " + amount + " ClaimChunks erhalten!");
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Zahl!");
        }
        return true;
    }

    private boolean handleTransferClaim(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cVerwendung: /transferclaim <Spieler>");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            player.sendMessage("§cDieser Chunk ist nicht beansprucht!");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        if (!data.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cDieser Claim gehört dir nicht!");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }

        data.setOwner(target.getUniqueId());
        storage.saveClaims(claims);

        player.sendMessage("§aClaim an " + target.getName() + " übertragen!");
        target.sendMessage("§a" + player.getName() + " hat dir einen Claim übertragen!");
        return true;
    }

    private boolean handleTrust(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cVerwendung: /trust <Spieler>");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cDu kannst dir selbst nicht vertrauen!");
            return true;
        }

        Set<UUID> trusted = ownerTrust.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        trusted.add(target.getUniqueId());
        storage.saveTrust(ownerTrust);

        player.sendMessage("§a" + target.getName() + " wurde in allen deinen Claims vertraut!");
        target.sendMessage("§a" + player.getName() + " vertraut dir in allen Claims!");
        return true;
    }

    private boolean handleUntrust(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cVerwendung: /untrust <Spieler>");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }

        Set<UUID> trusted = ownerTrust.get(player.getUniqueId());
        if (trusted == null || !trusted.contains(target.getUniqueId())) {
            player.sendMessage("§cDieser Spieler ist nicht vertraut!");
            return true;
        }

        trusted.remove(target.getUniqueId());
        storage.saveTrust(ownerTrust);

        player.sendMessage("§c" + target.getName() + " wird nicht mehr vertraut!");
        return true;
    }

    private boolean handleTrustList(Player player) {
        Set<UUID> trusted = ownerTrust.get(player.getUniqueId());

        if (trusted == null || trusted.isEmpty()) {
            player.sendMessage("§eKeine vertrauten Spieler!");
            return true;
        }

        player.sendMessage("§6=== Vertraute Spieler ===");
        for (UUID uuid : trusted) {
            String name = getServer().getOfflinePlayer(uuid).getName();
            player.sendMessage("§e- §f" + name);
        }
        return true;
    }

    // Event Handlers
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu kannst hier keine Blöcke abbauen!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu kannst hier keine Blöcke platzieren!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        if (isProtectedBlock(type)) {
            if (!canModify(event.getPlayer(), event.getClickedBlock().getChunk())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cDu kannst hier nicht interagieren!");
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu kannst hier keine Flüssigkeiten platzieren!");
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu kannst hier keine Flüssigkeiten aufnehmen!");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            String chunkKey = getChunkKey(block.getChunk());
            return claims.containsKey(chunkKey);
        });
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getRightClicked().getLocation().getChunk();

        if (event.getRightClicked() instanceof Villager) {
            if (!canModify(player, chunk)) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst hier nicht mit Dorfbewohnern handeln!");
            }
        } else if (event.getRightClicked() instanceof Animals) {
            if (!canModify(player, chunk)) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst hier keine Tiere reiten!");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Chunk chunk = event.getEntity().getLocation().getChunk();

        if (event.getEntity() instanceof Animals) {
            if (!canModify(player, chunk)) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst hier keine friedlichen Kreaturen angreifen!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }

        Player player = event.getPlayer();
        String chunkKey = getChunkKey(toChunk);
        String lastChunkKey = lastChunk.get(player.getUniqueId());

        if (chunkKey.equals(lastChunkKey)) {
            return;
        }

        lastChunk.put(player.getUniqueId(), chunkKey);

        ClaimData data = claims.get(chunkKey);
        String ownerName = getServer().getOfflinePlayer(data.getOwner()).getName();

        if (data.isAdminClaim()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#ff2626>Admin-Claim"));
        } else if (data.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Dein Grundstück"));
        } else {
            Set<UUID> trusted = ownerTrust.get(data.getOwner());
            if (trusted != null && trusted.contains(player.getUniqueId())) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Grundstück von <#fbecab>" + ownerName + " <dark_gray>(<#47ff26><italic>Trusted</italic><dark_gray>)"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Grundstück von <#fbecab>" + ownerName));
            }
        }
    }

    // Helper Methods
    private boolean canModify(Player player, Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        if (!claims.containsKey(chunkKey)) {
            return true;
        }

        ClaimData data = claims.get(chunkKey);

        if (data.isAdminClaim() && player.hasPermission("chunkclaims.admin.interact")) {
            return true;
        }

        if (data.getOwner().equals(player.getUniqueId())) {
            return true;
        }

        Set<UUID> trusted = ownerTrust.get(data.getOwner());
        if (trusted != null && trusted.contains(player.getUniqueId())) {
            return true;
        }

        return player.hasPermission("chunkclaims.admin");
    }

    private boolean isProtectedBlock(Material type) {
        return type == Material.CHEST || type == Material.FURNACE ||
                type == Material.BARREL || type == Material.HOPPER ||
                type == Material.DISPENSER || type == Material.DROPPER ||
                type == Material.BREWING_STAND || type == Material.ENCHANTING_TABLE ||
                type == Material.ANVIL || type == Material.CRAFTING_TABLE ||
                type.name().contains("SHULKER_BOX") ||
                type.name().contains("DOOR") || type.name().contains("TRAPDOOR") ||
                type.name().contains("BUTTON") || type.name().contains("LEVER") ||
                type.name().contains("GATE");
    }

    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    private int getAvailableChunks(UUID player) {
        return playerClaimChunks.getOrDefault(player, defaultClaimChunks);
    }

    private int getUsedChunks(UUID player) {
        int count = 0;
        for (ClaimData data : claims.values()) {
            if (data.getOwner().equals(player)) {
                count++;
            }
        }
        return count;
    }

}