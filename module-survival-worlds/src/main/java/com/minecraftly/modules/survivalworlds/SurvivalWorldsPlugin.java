package com.minecraftly.modules.survivalworlds;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 23/04/2015.
 */
public class SurvivalWorldsPlugin extends Module implements Listener {

    public static final String WORLD_NAME_PREFIX = "z-player-world-";

    public static boolean isSurvivalWorld(World world) {
        return world.getName().startsWith(WORLD_NAME_PREFIX);
    }

    private MinecraftlyCore plugin;

    public final Map<UUID, World> playerWorlds = new HashMap<>();
    public final Map<World, ConfigManager> worldConfigs = new HashMap<>();

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        this.plugin = plugin;

        PluginManager pluginManager = Bukkit.getPluginManager();
        PlayerListener playerListener = new PlayerListener(this);

        plugin.getGateway().registerListener(playerListener);
        pluginManager.registerEvents(this, plugin);
        pluginManager.registerEvents(playerListener, plugin);
    }

    @Override
    protected void onDisable(MinecraftlyCore plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(ChatColor.RED + "Slave server is shutting down.");
        }

        for (World world : playerWorlds.values()) {
            Bukkit.unloadWorld(world, true);
        }

        this.plugin = null;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) { // do some cleaning up
        World world = e.getWorld();
        UUID ownerUUID = getWorldOwnerUUID(world);

        if (ownerUUID != null) {
            ConfigManager configManager = worldConfigs.get(world);
            if (configManager != null) {
                worldConfigs.remove(world);
            }

            playerWorlds.remove(ownerUUID);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) { // save world config when world is saved
        ConfigManager configManager = worldConfigs.get(e.getWorld());
        if (configManager != null) {
            configManager.saveConfig();
        }
    }

    @Nullable
    private UUID getWorldOwnerUUID(World world) {
        for (Map.Entry<UUID, World> entry : playerWorlds.entrySet()) {
            if (entry.getValue().equals(world)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public World getWorld(UUID uuid) {
        World world = playerWorlds.get(uuid);

        if (world == null) {
            String uuidString = uuid.toString();
            world = Bukkit.getWorld(uuid.toString());

            if (world == null) {
                WorldCreator worldCreator = new WorldCreator(uuidString);
                File worldDirectory = new File(Bukkit.getWorldContainer(), WORLD_NAME_PREFIX + uuidString);

                if (worldDirectory.exists() && worldDirectory.isDirectory()) {
                    world = worldCreator.createWorld(); // this actually loads an existing world
                } else { // generate world async
                    // todo start generation from BungeeCord?
                    world = worldCreator.createWorld();
                }
            }

            playerWorlds.put(uuid, world);
        }

        return world;
    }

}
