package com.minecraftly.modules.survivalworlds;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import com.minecraftly.core.packets.survivalworlds.PacketNoLongerHosting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
public class SurvivalWorldsModule extends Module implements Listener {

    public static final String WORLD_NAME_PREFIX = "z-player-world-";

    public static String getWorldName(UUID uuid) {
        return WORLD_NAME_PREFIX + uuid;
    }

    private MinecraftlyCore bukkitPlugin;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();
    public final Map<World, ConfigManager> worldConfigs = new HashMap<>();

    public MinecraftlyCore getBukkitPlugin() {
        return bukkitPlugin;
    }

    @Override
    protected void onEnable(MinecraftlyCore bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;
        this.gateway = bukkitPlugin.getGateway();

        PluginManager pluginManager = Bukkit.getPluginManager();
        PlayerListener playerListener = new PlayerListener(this);

        gateway.registerListener(playerListener);
        pluginManager.registerEvents(this, bukkitPlugin);
        pluginManager.registerEvents(playerListener, bukkitPlugin);
    }

    @Override
    protected void onDisable(MinecraftlyCore plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(ChatColor.RED + "Slave server is shutting down.");
        }

        for (World world : playerWorlds.values()) {
            Bukkit.unloadWorld(world, true);
        }

        this.bukkitPlugin = null;
    }

    public boolean isSurvivalWorld(World world) {
        return playerWorlds.values().contains(world);
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
            gateway.sendPacket(new PacketNoLongerHosting(ownerUUID), false); // notify proxy if possible
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
            String worldName = getWorldName(uuid);
            world = Bukkit.getWorld(worldName);

            if (world == null) {
                WorldCreator worldCreator = new WorldCreator(worldName);
                File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);

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

    public Location getRespawnLocation(Player player) {
        return null; // todo
    }

}
