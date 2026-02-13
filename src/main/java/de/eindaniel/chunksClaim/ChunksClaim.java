package de.eindaniel.chunksClaim;

import de.eindaniel.chunksClaim.commands.*;
import de.eindaniel.chunksClaim.config.ClaimData;
import de.eindaniel.chunksClaim.config.ClaimStorage;
import de.eindaniel.chunksClaim.i18n.MessageUtils;
import de.eindaniel.chunksClaim.i18n.MessageUtilsSingleton;
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
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
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
        MessageUtilsSingleton.initialize(this);
        defaultClaimChunks = getConfig().getInt("default-claim-chunks", 50);

        getServer().getPluginManager().registerEvents(this, this);
        storage = new ClaimStorage(getDataFolder());
        claims = storage.loadClaims();
        playerClaimChunks = storage.loadPlayerData();
        ownerTrust = storage.loadTrust();
        registerCommands();

        startActionBarTask();

        getLogger().info("ChunkClaims aktiviert! " + claims.size() + " Claims geladen.");
    }

    @Override
    public void onDisable() {
        storage.saveClaims(claims);
        storage.savePlayerData(playerClaimChunks);
        storage.saveTrust(ownerTrust);
        getLogger().info("ChunkClaims deaktiviert!");
    }

    public void registerCommands() {
        registerCommand(new AdminClaimCommand(this));
        registerCommand(new ClaimCommand(this));
        registerCommand(new ClaimInfo(this));
        registerCommand(new DeleteAllMyClaims(this));
        registerCommand(new GiveClaimChunks(this));
        registerCommand(new RemoveClaim(this));
        registerCommand(new TransferClaim(this));
        registerCommand(new Trust(this));
        registerCommand(new TrustList(this));
        registerCommand(new Untrust(this));
    }

    public void registerCommand(Command command) {
        CommandMap commandMap = Bukkit.getCommandMap();
        commandMap.register("chunksclaim", command);
    }

    public boolean handleClaim(Player player, boolean admin) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (claims.containsKey(chunkKey)) {
            ClaimData data = claims.get(chunkKey);
            if (data.getOwner().equals(player.getUniqueId())) {
                messageUtils.sendLocalizedMessage(player, "alreadyOwned");
            } else {
                messageUtils.sendLocalizedMessage(player, "alreadyClaimed");
            }
            return true;
        }

        if (!admin) {
            int available = getAvailableChunks(player.getUniqueId());
            int used = getUsedChunks(player.getUniqueId());

            if (used >= available) {
                messageUtils.sendLocalizedMessage(player, "noClaimChunks", used, available);
                return true;
            }
        }

        ClaimData data = new ClaimData(player.getUniqueId(), admin);
        claims.put(chunkKey, data);
        storage.saveClaims(claims);

        if (admin) {
            messageUtils.sendLocalizedMessage(player, "adminClaimCreated", chunk.getX(), chunk.getZ());
        } else {
            int used = getUsedChunks(player.getUniqueId());
            int available = getAvailableChunks(player.getUniqueId());
            messageUtils.sendLocalizedMessage(player, "claimCreated", used, available);
        }
        return true;
    }

    public boolean handleAdminClaim(Player player) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        if (!player.hasPermission("chunkclaims.admin")) {
            messageUtils.sendLocalizedMessage(player, "noperm");
            return true;
        }
        return handleClaim(player, true);
    }

    public boolean handleRemoveClaim(Player player) {
        MessageUtils messageUtils =  MessageUtilsSingleton.getInstance();
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            messageUtils.sendLocalizedMessage(player, "chunkNotClaimed");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        if (!data.getOwner().equals(player.getUniqueId()) && !player.hasPermission("chunkclaims.admin")) {
            messageUtils.sendLocalizedMessage(player, "notYourChunk");
            return true;
        }

        claims.remove(chunkKey);
        storage.saveClaims(claims);

        int used = getUsedChunks(player.getUniqueId());
        int available = getAvailableChunks(player.getUniqueId());
        messageUtils.sendLocalizedMessage(player, "claimRemoved");
        return true;
    }

    public boolean handleClaimInfo(Player player) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            messageUtils.sendLocalizedMessage(player, "chunkNotClaimed");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        String ownerName = getServer().getOfflinePlayer(data.getOwner()).getName();

        messageUtils.sendLocalizedMessage(player, "claimInfoID", chunkKey);
        messageUtils.sendLocalizedMessage(player, "claimInfoOwner", ownerName);
        messageUtils.sendLocalizedMessage(player, "claimInfoAdminClaim", (data.isAdminClaim() ? "Ja" : "Nein"));
        messageUtils.sendLocalizedMessage(player, "claimInfoMin", (chunk.getX() * 16) + ", " + (chunk.getZ() * 16));
        messageUtils.sendLocalizedMessage(player, "claimInfoMax", (chunk.getX() * 16 + 15) + ", " + (chunk.getZ() * 16 + 15));
        return true;
    }

    public boolean handleDeleteAllMyClaims(Player player) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, ClaimData> entry : claims.entrySet()) {
            if (entry.getValue().getOwner().equals(player.getUniqueId())) {
                toRemove.add(entry.getKey());
            }
        }

        if (toRemove.isEmpty()) {
            messageUtils.sendLocalizedMessage(player, "noClaims");
            return true;
        }

        for (String key : toRemove) {
            claims.remove(key);
        }
        storage.saveClaims(claims);

        int available = getAvailableChunks(player.getUniqueId());
        messageUtils.sendLocalizedMessage(player, "allClaimsRemoved");
        return true;
    }

    public boolean handleGiveClaimChunks(Player player, String[] args) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        if (!player.hasPermission("chunkclaims.admin")) {
            messageUtils.sendLocalizedMessage(player, "noperm");
            return true;
        }

        if (args.length < 2) {
            messageUtils.sendLocalizedMessage(player, "giveClaimChunksUsage");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            messageUtils.sendLocalizedMessage(player, "playerNotFound");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            int current = playerClaimChunks.getOrDefault(target.getUniqueId(), defaultClaimChunks);
            playerClaimChunks.put(target.getUniqueId(), current + amount);
            storage.savePlayerData(playerClaimChunks);

            messageUtils.sendLocalizedMessage(player, "playerReceived", target.getName(), (current + amount));
            messageUtils.sendLocalizedMessage(target, "youReceived", amount);
        } catch (NumberFormatException e) {
            messageUtils.sendLocalizedMessage(player, "NaN");
        }
        return true;
    }

    public boolean handleTransferClaim(Player player, String[] args) {
        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        if (args.length < 1) {
            messageUtils.sendLocalizedMessage(player, "transferclaimUsage");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        if (!claims.containsKey(chunkKey)) {
            messageUtils.sendLocalizedMessage(player, "chunkNotClaimed");
            return true;
        }

        ClaimData data = claims.get(chunkKey);
        if (!data.getOwner().equals(player.getUniqueId())) {
            messageUtils.sendLocalizedMessage(player, "notYourClaim");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            messageUtils.sendLocalizedMessage(player, "playerNotFound");
            return true;
        }

        data.setOwner(target.getUniqueId());
        storage.saveClaims(claims);

        messageUtils.sendLocalizedMessage(player, "transferedTo", target.getName());
        messageUtils.sendLocalizedMessage(target, "transferedFrom", player.getName());
        return true;
    }

    public boolean handleTrust(Player player, String[] args) {
        MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
        if (args.length < 1) {
            messageUtils.sendLocalizedMessage(player, "trustUsage");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            messageUtils.sendLocalizedMessage(player, "playerNotFound");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            messageUtils.sendLocalizedMessage(player, "notYourself");
            return true;
        }

        Set<UUID> trusted = ownerTrust.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        trusted.add(target.getUniqueId());
        storage.saveTrust(ownerTrust);

        messageUtils.sendLocalizedMessage(player, "trustTo", target.getName());
        messageUtils.sendLocalizedMessage(target, "trustFrom", player.getName());
        return true;
    }

    public boolean handleUntrust(Player player, String[] args) {
        MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
        if (args.length < 1) {
            messageUtils.sendLocalizedMessage(player, "untrustUsage");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            messageUtils.sendLocalizedMessage(player, "playerNotFound");
            return true;
        }

        Set<UUID> trusted = ownerTrust.get(player.getUniqueId());
        if (trusted == null || !trusted.contains(target.getUniqueId())) {
            messageUtils.sendLocalizedMessage(player, "notTrusted");
            return true;
        }

        trusted.remove(target.getUniqueId());
        storage.saveTrust(ownerTrust);

        messageUtils.sendLocalizedMessage(player, "untrustSuccess", target.getName());
        return true;
    }

    public boolean handleTrustList(Player player) {
        MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
        Set<UUID> trusted = ownerTrust.get(player.getUniqueId());

        if (trusted == null || trusted.isEmpty()) {
            messageUtils.sendLocalizedMessage(player, "noTrusted");
            return true;
        }

        messageUtils.sendLocalizedMessage(player, "trustListHeader", player.getName());
        for (UUID uuid : trusted) {
            String name = getServer().getOfflinePlayer(uuid).getName();
            messageUtils.sendLocalizedMessage(player, "trustListPlayer", name);
        }
        return true;
    }

    // Event Handlers
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
            messageUtils.sendLocalizedMessage(event.getPlayer(), "blocks.breaking");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
            messageUtils.sendLocalizedMessage(event.getPlayer(), "blocks.placing");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        if (isProtectedBlock(type)) {
            if (!canModify(event.getPlayer(), event.getClickedBlock().getChunk())) {
                event.setCancelled(true);
                MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
                messageUtils.sendLocalizedMessage(event.getPlayer(), "interact");
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
            messageUtils.sendLocalizedMessage(event.getPlayer(), "liquids.place");
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
            messageUtils.sendLocalizedMessage(event.getPlayer(), "liquids.fill");
        }
    }

    @EventHandler
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getEntity().getLocation().getChunk();

        if (!canModify(player, chunk)) {
            event.setCancelled(true);
            MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
            messageUtils.sendLocalizedMessage(player, "fish");
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
                MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
                messageUtils.sendLocalizedMessage(player, "trade");
            }
        } else if (event.getRightClicked() instanceof Animals) {
            if (!canModify(player, chunk)) {
                event.setCancelled(true);
                MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
                messageUtils.sendLocalizedMessage(player, "mount");
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
                MessageUtils  messageUtils = MessageUtilsSingleton.getInstance();
                messageUtils.sendLocalizedMessage(player, "friendlyAnimals");
            }
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.SPREAD) return;

        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
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
    }

    private void startActionBarTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                updateActionBar(player);
            }
        }, 0L, 20L); // Alle 1 Sekunde (20 ticks)
    }

    private void updateActionBar(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);

        ClaimData data = claims.get(chunkKey);
        if (data == null) {
            return;
        }
        String ownerName = getServer().getOfflinePlayer(data.getOwner()).getName();

        MessageUtils messageUtils = MessageUtilsSingleton.getInstance();
        if (data.isAdminClaim()) {
            player.sendActionBar(messageUtils.getLocalizedMessage(player, "actionbar.adminClaim"));
        } else if (data.getOwner().equals(player.getUniqueId())) {
            player.sendActionBar(messageUtils.getLocalizedMessage(player, "actionbar.yourClaim"));
        } else {
            Set<UUID> trusted = ownerTrust.get(data.getOwner());
            if (trusted != null && trusted.contains(player.getUniqueId())) {
                player.sendActionBar(messageUtils.getLocalizedMessage(player, "actionbar.otherClaimTrusted", ownerName));
            } else {
                player.sendActionBar(messageUtils.getLocalizedMessage(player, "actionbar.otherClaim", ownerName));
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