package de.eindaniel.chunksClaim;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClaimStorage {
    private File claimsFile;
    private File playerDataFile;

    public ClaimStorage(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.claimsFile = new File(dataFolder, "claims.yml");
        this.playerDataFile = new File(dataFolder, "playerdata.yml");
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
            List<String> trustedStr = config.getStringList(key + ".trusted");

            ClaimData data = new ClaimData(UUID.fromString(ownerStr), adminClaim);
            for (String t : trustedStr) {
                data.addTrusted(UUID.fromString(t));
            }
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

            List<String> trustedList = new ArrayList<>();
            for (UUID uuid : data.getTrusted()) {
                trustedList.add(uuid.toString());
            }
            config.set(key + ".trusted", trustedList);
        }

        try {
            config.save(claimsFile);
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
