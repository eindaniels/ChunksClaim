package de.eindaniel.chunksClaim.i18n;

import de.eindaniel.chunksClaim.ChunksClaim;

public class MessageUtilsSingleton {
    private static MessageUtils instance;

    public static void initialize(ChunksClaim plugin) {
        if (instance == null) {
            instance = new MessageUtils(plugin);
        }
    }

    public static MessageUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MessageUtilsSingleton is not initialized. Call initialize() first.");
        }
        return instance;
    }
}