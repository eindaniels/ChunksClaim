package de.eindaniel.chunksClaim.i18n;

import de.cytooxien.realms.api.PlayerInformationProvider;
import de.eindaniel.chunksClaim.ChunksClaim;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.sisu.launch.Main;

import java.text.MessageFormat;

public class MessageUtils {
    private final Internationalization internationalization;

    public MessageUtils(ChunksClaim plugin) {
        this.internationalization = new Internationalization(plugin);
    }

    public void sendLocalizedMessage(Player player, String messageKey, Object... args) {
        PlayerInformationProvider playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);

        String language = playerInformationProvider.language(player.getUniqueId()).name();
        String messageTemplate = internationalization.getLanguageConfig(language).getString(messageKey);
        String prefix = internationalization.getLanguageConfig(language).getString("prefix");

        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            Component messageComponent = MiniMessage.miniMessage().deserialize(formattedMessage);
            Component pref = MiniMessage.miniMessage().deserialize(prefix);
            player.sendMessage(pref.append(messageComponent));
        } else {
            Component messageComponent = MiniMessage.miniMessage().deserialize("<red>Could not find key: " + messageKey + ". Please report this to the server administrator.");
            player.sendMessage(messageComponent);
        }
    }

    public void sendLocalizedMessageCustomPrefix(Component prefix, Player player, String messageKey, Object... args) {
        PlayerInformationProvider playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);

        String language = playerInformationProvider.language(player.getUniqueId()).name();
        String messageTemplate = internationalization.getLanguageConfig(language).getString(messageKey);

        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            Component messageComponent = MiniMessage.miniMessage().deserialize(formattedMessage);
            player.sendMessage(prefix.append(messageComponent));
        } else {
            Component messageComponent = MiniMessage.miniMessage().deserialize("<red>Could not find key: " + messageKey + ". Please report this to the server administrator.");
            player.sendMessage(messageComponent);
        }
    }

    public void sendLocalizedMessageWithoutPrefix(Player player, String messageKey, Object... args) {
        PlayerInformationProvider playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);

        String language = playerInformationProvider.language(player.getUniqueId()).name();
        String messageTemplate = internationalization.getLanguageConfig(language).getString(messageKey);

        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            Component messageComponent = MiniMessage.miniMessage().deserialize(formattedMessage);
            player.sendMessage(messageComponent);
        } else {
            Component messageComponent = MiniMessage.miniMessage().deserialize("<#ff1717>Could not find key: " + messageKey + ". Please report this to the server administrator.");
            player.sendMessage(messageComponent);
        }
    }

    public Component getLocalizedMessage(Player player, String messageKey, Object... args) {
        PlayerInformationProvider playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);

        String language = playerInformationProvider.language(player.getUniqueId()).name();
        String messageTemplate = internationalization.getLanguageConfig(language).getString(messageKey);
        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            Component messageComponent = MiniMessage.miniMessage().deserialize(formattedMessage);
            return messageComponent;
        }
        return null;
    }

    public String getLocalizedMessageString(Player player, String messageKey, Object... args) {
        PlayerInformationProvider playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);

        String language = playerInformationProvider.language(player.getUniqueId()).name();
        String messageTemplate = internationalization.getLanguageConfig(language).getString(messageKey);
        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            return formattedMessage;
        }
        return null;
    }


    public Component getLocalizedMessage(String localefile, String messageKey, Object... args) {
        String messageTemplate = internationalization.getLanguageConfig(localefile).getString(messageKey);

        if (messageTemplate != null) {
            String formattedMessage = MessageFormat.format(messageTemplate, args);
            Component messageComponent = MiniMessage.miniMessage().deserialize(formattedMessage);
            return messageComponent;
        } else {
            Component messageComponent = MiniMessage.miniMessage().deserialize("<red>Could not find key: " + messageKey + ". Please report this to the server administrator.");
            return messageComponent;
        }
    }

    public void sendLocalizedMessageToAllPlayers(String messageKey, Object... args) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendLocalizedMessage(player, messageKey, args);
        }
    }

    public void sendLocalizedMessageToAllPlayersWithoutPrefix(String messageKey, Object... args) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendLocalizedMessageWithoutPrefix(player, messageKey, args);
        }
    }

    public String getLocalizedString(String localefile, String messageKey, Object... args) {
        String messageTemplate = internationalization.getLanguageConfig(localefile).getString(messageKey);

        if (messageTemplate != null) {
            return MessageFormat.format(messageTemplate, args);
        } else {
            return "<#ff1717>Could not find key: " + messageKey;
        }
    }
}