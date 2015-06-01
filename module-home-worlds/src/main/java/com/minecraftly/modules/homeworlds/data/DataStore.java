package com.minecraftly.modules.homeworlds.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.modules.homeworlds.HomeWorldsModule;
import com.minecraftly.modules.homeworlds.WorldDimension;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by Keir on 03/05/2015.
 */
public class DataStore implements Listener {

    private final String WORLD_PLAYER_DIR_NAME = "mcly-player-data";

    private final HomeWorldsModule module;
    private final File globalPlayerDirectory;

    // World UUID, Player UUID, Data
    private final Table<UUID, UUID, PlayerWorldData> worldPlayerDataTable = HashBasedTable.create();
    private final Map<UUID, PlayerGlobalData> globalPlayerDataMap = new HashMap<>();

    public DataStore(final HomeWorldsModule module, File globalPlayerDirectory) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null.");
        }

        if (globalPlayerDirectory == null) {
            throw new IllegalArgumentException("Global player directory cannot be null.");
        }

        if (!globalPlayerDirectory.isDirectory()) {
            throw new IllegalArgumentException("File is not a valid global player data directory: " + globalPlayerDirectory.getPath());
        }

        this.module = module;
        this.globalPlayerDirectory = globalPlayerDirectory;
        module.registerListener(this);

        module.getBukkitPlugin().getPlayerSwitchJobManager().addJob(new Consumer<Player>() {
            @Override
            public void accept(Player player) {
                World world = WorldDimension.getBaseWorld(player.getWorld());

                if (module.isHomeWorld(world)) {
                    PlayerGlobalData playerGlobalData = getPlayerGlobalData(player);
                    playerGlobalData.copyFromPlayer(player);
                    playerGlobalData.saveToFile();
                    unloadPlayerGlobalData(player);

                    PlayerWorldData playerWorldData = getPlayerWorldData(world, player);
                    playerWorldData.copyFromPlayer(player);
                    playerWorldData.saveToFile();
                    unloadPlayerWorldData(world, player);

                    player.teleport(BukkitUtilities.getSafeLocation(Bukkit.getWorlds().get(0).getSpawnLocation()));
                }
            }
        });
    }

    public PlayerWorldData getPlayerWorldData(World world, OfflinePlayer offlinePlayer) {
        return getPlayerWorldData(world, offlinePlayer, true);
    }

    public PlayerWorldData getPlayerWorldData(World world, OfflinePlayer offlinePlayer, boolean loadIfNotLoaded) {
        UUID worldUUID = world.getUID();
        UUID playerUUID = offlinePlayer.getUniqueId();

        PlayerWorldData playerWorldData = worldPlayerDataTable.get(worldUUID, playerUUID);

        if (playerWorldData == null && loadIfNotLoaded) {
            playerWorldData = loadPlayerWorldData(world.getWorldFolder(), playerUUID);
            worldPlayerDataTable.put(worldUUID, playerUUID, playerWorldData);
        }

        return playerWorldData;
    }

    private PlayerWorldData loadPlayerWorldData(File worldDirectory, UUID playerUUID) {
        File worldPlayerDataDirectory = new File(worldDirectory, WORLD_PLAYER_DIR_NAME);
        worldPlayerDataDirectory.mkdir();

        if (!worldPlayerDataDirectory.isDirectory()) {
            throw new RuntimeException("Not a directory: " + worldPlayerDataDirectory.getPath());
        }

        File playerDataFile = new File(worldPlayerDataDirectory, playerUUID + ".yml");
        return new PlayerWorldData(playerUUID, playerDataFile);
    }

    public void unloadPlayerWorldData(World world, OfflinePlayer offlinePlayer) {
        worldPlayerDataTable.remove(world, offlinePlayer.getUniqueId());
    }

    public PlayerGlobalData getPlayerGlobalData(OfflinePlayer offlinePlayer) {
        return getPlayerGlobalData(offlinePlayer, true);
    }

    public PlayerGlobalData getPlayerGlobalData(OfflinePlayer offlinePlayer, boolean loadIfNotLoaded) {
        UUID playerUUID = offlinePlayer.getUniqueId();
        PlayerGlobalData playerGlobalData = globalPlayerDataMap.get(playerUUID);

        if (playerGlobalData == null && loadIfNotLoaded) {
            playerGlobalData = loadPlayerGlobalData(playerUUID);
            globalPlayerDataMap.put(playerUUID, playerGlobalData);
        }

        return playerGlobalData;
    }

    private PlayerGlobalData loadPlayerGlobalData(UUID uuid) {
        File playerGlobalDataFile = new File(globalPlayerDirectory, uuid + ".yml");
        return new PlayerGlobalData(uuid, playerGlobalDataFile);
    }

    public void unloadPlayerGlobalData(OfflinePlayer offlinePlayer) {
        globalPlayerDataMap.remove(offlinePlayer.getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        boolean fromWorldHome = module.isHomeWorld(fromWorld);
        boolean toWorldHome = module.isHomeWorld(toWorld);

        PlayerGlobalData playerGlobalData = getPlayerGlobalData(player);

        if (fromWorld != toWorld) {
            if (fromWorldHome) {
                PlayerWorldData playerWorldData = getPlayerWorldData(fromWorld, player);

                if (playerWorldData != null) {
                    playerWorldData.copyFromPlayer(player);
                    playerWorldData.saveToFile();
                    unloadPlayerWorldData(fromWorld, player);
                }
            }

            if (toWorldHome) {
                PlayerWorldData playerWorldData = getPlayerWorldData(toWorld, player);

                if (playerGlobalData != null && !fromWorldHome) {
                    playerGlobalData.copyToPlayer(player);
                }

                if (playerWorldData != null) {
                    playerWorldData.copyToPlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        worldPlayerDataTable.row(e.getWorld().getUID()).values().forEach(PlayerWorldData::saveToFile);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        worldPlayerDataTable.row(e.getWorld().getUID()).clear();
    }

}
