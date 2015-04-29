package com.minecraftly.modules.survivalworlds;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import com.minecraftly.core.packets.survivalworlds.PacketPlayerWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener {

    private SurvivalWorldsPlugin plugin;

    private LoadingCache<UUID, World> worldCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<UUID, World>() {
                @Override
                public void onRemoval(@Nonnull RemovalNotification<UUID, World> notification) {
                    if (notification.getCause() != RemovalCause.EXPLICIT) {
                        World world = notification.getValue();

                        if (world != null) {
                            checkWorldForUnload(world);
                        }
                    }
                }
            }).build(new CacheLoader<UUID, World>() {
                @Override
                public World load(@Nonnull UUID key) throws Exception {
                    return plugin.getWorld(key);
                }
            });

    public PlayerListener(SurvivalWorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();

        try {
            joinWorld(player, worldCache.get(playerUUID));
        } catch (ExecutionException e1) {
            player.kickPlayer(ChatColor.RED + "Error whilst loading world: " + e1.getMessage() + "\nSee console for further details.");
            plugin.getLogger().log(Level.SEVERE, "Error whilst loading world for player " + playerUUID, e1);
        }
    }

    @PacketHandler
    public void onPacketJoinWorld(PacketPlayerWorld packet) {
        UUID playerUUID = packet.getPlayer();
        UUID worldUUID = packet.getWorld();

        Player player = Bukkit.getPlayer(playerUUID);
        World world = plugin.getWorld(worldUUID);

        if (player != null) {
            joinWorld(player, world);
        } else {
            worldCache.put(playerUUID, world);
        }
    }

    public void joinWorld(Player player, World world) {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(world);

        UUID playerUUID = player.getUniqueId();
        worldCache.invalidate(playerUUID);

        Location spawnLocation;
        File dataFile = new File(world.getWorldFolder(), "mcly-data.yml");

        // todo util method for player data
        if (dataFile.exists() && dataFile.isFile()) {
            ConfigManager configManager = new ConfigManager(dataFile);
            FileConfiguration fileConfiguration = configManager.getConfig();
            spawnLocation = BukkitUtilities.getLocation(fileConfiguration.getConfigurationSection("players." + playerUUID + ".lastLocation"));
        } else {
            spawnLocation = BukkitUtilities.getSafeLocation(world.getSpawnLocation());
        }

        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        checkWorldForUnload(e.getPlayer().getWorld());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        World from = e.getFrom().getWorld();
        World to = e.getTo().getWorld();

        if (!from.equals(to)) {
            checkWorldForUnload(from);
        }
    }

    public void checkWorldForUnload(World world) {
        if (SurvivalWorldsPlugin.isSurvivalWorld(world) && world.getPlayers().size() == 0) {
            Bukkit.unloadWorld(world, true);
        }
    }

}
