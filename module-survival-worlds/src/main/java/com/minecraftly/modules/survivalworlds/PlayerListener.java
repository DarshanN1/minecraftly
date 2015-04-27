package com.minecraftly.modules.survivalworlds;

import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.util.UUID;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener {

    private SurvivalWorldsPlugin plugin;

    public PlayerListener(SurvivalWorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        World world = plugin.getWorld(player.getUniqueId());

        if (world != null) {
            Location spawnLocation;
            File dataFile = new File(world.getWorldFolder(), "mcly-data.yml");



            // todo util method for player data
            if (dataFile.exists() && dataFile.isFile()) {
                ConfigManager configManager = new ConfigManager(dataFile);
                FileConfiguration fileConfiguration = configManager.getConfig();
                spawnLocation = BukkitUtilities.getLocation(fileConfiguration.getConfigurationSection("players." + uuid + ".lastLocation"));
            } else {
                spawnLocation = BukkitUtilities.getSafeLocation(world.getSpawnLocation());
            }

            player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            player.kickPlayer(ChatColor.RED + "There was an error whilst loading your world.\nPlease contact support for help.");
        }
    }

}
