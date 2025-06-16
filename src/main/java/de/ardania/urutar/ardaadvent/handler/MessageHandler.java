package de.ardania.urutar.ardaadvent.handler;

import de.ardania.urutar.ardaadvent.domain.Messages;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Handler class for the messages of this plugin.
 */
public class MessageHandler {
    public static void showMessage(Player player, String message) {
        if (player != null)
            player.sendMessage(Messages.PLUGIN_PREFIX + message);
    }

    public static void showPlainMessage(Player player, String message) {
        if (player != null)
            player.sendMessage(message);
    }

}
