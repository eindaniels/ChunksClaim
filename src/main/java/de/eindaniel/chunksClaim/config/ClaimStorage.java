package de.eindaniel.chunksClaim.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClaimStorage {
    private File claimsFile;
    private File playerDataFile;
    private File trustFile;

    public ClaimStorage(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.claimsFile = new File(dataFolder, "claims.yml");
        this.playerDataFile = new File(dataFolder, "playerdata.yml");
        this.trustFile = new File(dataFolder, "trust.yml");
    }

    public Map<String, ClaimData> loadClaims() {
        Map<String, ClaimData> claims = new HashMap<>();
        if (!claimsFile.exists()) {
            return claims;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(claimsFile);
        for (String key : config.getKeys(false)) {
            String ownerStr = config.getString(key + ".owner");
            boolean adminClaim = config.getBoolean(key + ".adminClaim", false);

            ClaimData data = new ClaimData(UUID.fromString(ownerStr), adminClaim);
            claims.put(key, data);
        }
        return claims;
    }

    public void saveClaims(Map<String, ClaimData> claims) {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ClaimData> entry : claims.entrySet()) {
            String key = entry.getKey();
            ClaimData data = entry.getValue();

            config.set(key + ".owner", data.getOwner().toString());
            config.set(key + ".adminClaim", data.isAdminClaim());
        }

        try {
            config.save(claimsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Set<UUID>> loadTrust() {
        Map<UUID, Set<UUID>> ownerTrust = new HashMap<>();
        if (!trustFile.exists()) {
            return ownerTrust;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(trustFile);
        for (String key : config.getKeys(false)) {
            UUID owner = UUID.fromString(key);
            List<String> trustedList = config.getStringList(key);

            Set<UUID> trusted = new HashSet<>();
            for (String t : trustedList) {
                trusted.add(UUID.fromString(t));
            }
            ownerTrust.put(owner, trusted);
        }
        return ownerTrust;
    }

    public void saveTrust(Map<UUID, Set<UUID>> ownerTrust) {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : ownerTrust.entrySet()) {
            List<String> trustedList = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                trustedList.add(uuid.toString());
            }
            config.set(entry.getKey().toString(), trustedList);
        }

        try {
            config.save(trustFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Integer> loadPlayerData() {
        Map<UUID, Integer> playerChunks = new HashMap<>();
        if (!playerDataFile.exists()) {
            return playerChunks;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        for (String key : config.getKeys(false)) {
            playerChunks.put(UUID.fromString(key), config.getInt(key));
        }
        return playerChunks;
    }

    public void savePlayerData(Map<UUID, Integer> playerChunks) {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : playerChunks.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
