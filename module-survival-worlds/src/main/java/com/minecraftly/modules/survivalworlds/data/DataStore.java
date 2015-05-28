package com.minecraftly.modules.survivalworlds.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.minecraftly.core.Callback;
import com.minecraftly.modules.survivalworlds.SurvivalWorldsModule;
import com.minecraftly.modules.survivalworlds.WorldDimension;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 03/05/2015.
 */
public class DataStore implements Listener {

    private final String WORLD_PLAYER_DIR_NAME = "mcly-player-data";

    private final SurvivalWorldsModule module;
    private final File globalPlayerDirectory;

    // World UUID, Player UUID, Data
    private final Table<UUID, UUID, PlayerWorldData> worldPlayerDataTable = HashBasedTable.create();
    private final Map<UUID, PlayerGlobalData> globalPlayerDataMap = new HashMap<>();

    public DataStore(SurvivalWorldsModule module, File globalPlayerDirectory) {
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

        module.getBukkitPlugin().getPreSwitchJobManager().addJob(new Callback<Player>() {
            @Override
            public void call(Player player) {
                PlayerGlobalData playerGlobalData = getPlayerGlobalData(player);
                if (playerGlobalData != null) {
                    playerGlobalData.saveToFile();
                }

                World world = WorldDimension.getBaseWorld(player.getWorld());
                PlayerWorldData playerWorldData = getPlayerWorldData(world, player);
                if (playerWorldData != null) {
                    playerWorldData.copyFromPlayer(player);
                    playerWorldData.saveToFile();
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

        if (fromWorld != toWorld) {
            if (module.isSurvivalWorld(fromWorld)) {
                PlayerWorldData playerWorldData = getPlayerWorldData(fromWorld, player);

                if (playerWorldData != null) {
                    playerWorldData.copyFromPlayer(player);
                    playerWorldData.saveToFile();
                    unloadPlayerWorldData(fromWorld, player);
                }
            }

            if (module.isSurvivalWorld(toWorld)) {
                PlayerWorldData playerWorldData = getPlayerWorldData(toWorld, player);

                if (playerWorldData != null) {
                    playerWorldData.copyToPlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        for (PlayerWorldData playerWorldData : worldPlayerDataTable.row(e.getWorld().getUID()).values()) {
            playerWorldData.saveToFile();
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        worldPlayerDataTable.row(e.getWorld().getUID()).clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = WorldDimension.getBaseWorld(player.getWorld());
        PlayerGlobalData playerGlobalData = getPlayerGlobalData(e.getPlayer());

        if (playerGlobalData != null) {
            playerGlobalData.copyFromPlayer(player);
            unloadPlayerGlobalData(player);
        }

        if (module.isSurvivalWorld(world)) {
            PlayerWorldData playerWorldData = getPlayerWorldData(world, player);
            if (playerWorldData != null) {
                unloadPlayerWorldData(world, player);
            }
        }
    }

}
