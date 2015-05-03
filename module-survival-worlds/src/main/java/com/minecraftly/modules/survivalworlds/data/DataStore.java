package com.minecraftly.modules.survivalworlds.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 03/05/2015.
 */
public class DataStore implements Listener {

    private final String WORLD_PLAYER_DIR_NAME = "mcly-player-data";

    private final File globalPlayerDirectory;

    // World UUID, Player UUID, Data
    private final Table<UUID, UUID, PlayerWorldData> worldPlayerDataTable = HashBasedTable.create();
    private final Map<UUID, PlayerGlobalData> globalPlayerDataMap = new HashMap<>();

    public DataStore(Plugin plugin, File globalPlayerDirectory) {
        if (globalPlayerDirectory == null) {
            throw new IllegalArgumentException("Global player directory cannot be null.");
        }

        if (!globalPlayerDirectory.isDirectory()) {
            throw new IllegalArgumentException("File is not a valid global player data directory: " + globalPlayerDirectory.getPath());
        }

        this.globalPlayerDirectory = globalPlayerDirectory;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public PlayerWorldData getPlayerWorldData(World world, Player player) {
        return getPlayerWorldData(world, player, true);
    }

    public PlayerWorldData getPlayerWorldData(World world, Player player, boolean loadIfNotLoaded) {
        UUID worldUUID = world.getUID();
        UUID playerUUID = player.getUniqueId();

        PlayerWorldData playerWorldData = worldPlayerDataTable.get(worldUUID, playerUUID);

        if (playerWorldData == null && loadIfNotLoaded) {
            playerWorldData = loadPlayerWorldData(world.getWorldFolder(), playerUUID);
            worldPlayerDataTable.put(worldUUID, playerUUID, playerWorldData);
        }

        return playerWorldData;
    }

    private PlayerWorldData loadPlayerWorldData(File worldDirectory, UUID playerUUID) {
        File worldPlayerDataDirectory = new File(worldDirectory, WORLD_PLAYER_DIR_NAME);

        if (worldPlayerDataDirectory.exists() && !worldPlayerDataDirectory.isDirectory()) {
            throw new RuntimeException("Not a directory: " + worldPlayerDataDirectory.getPath());
        }

        File playerDataFile = new File(worldPlayerDataDirectory, playerUUID + ".yml");
        return new PlayerWorldData(playerUUID, playerDataFile);
    }

    public PlayerGlobalData getPlayerGlobalData(Player player) {
        return getPlayerGlobalData(player, true);
    }

    public PlayerGlobalData getPlayerGlobalData(Player player, boolean loadIfNotLoaded) {
        UUID playerUUID = player.getUniqueId();
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

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        for (PlayerWorldData playerWorldData : worldPlayerDataTable.row(e.getWorld().getUID()).values()) {
            playerWorldData.saveToFile();
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        // todo test
        System.out.println("Before " + worldPlayerDataTable);
        worldPlayerDataTable.row(e.getWorld().getUID()).clear();
        System.out.println("After " + worldPlayerDataTable);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PlayerGlobalData playerGlobalData = getPlayerGlobalData(e.getPlayer(), false);
        if (playerGlobalData != null) {
            playerGlobalData.saveToFile();
        }
    }

}
