package com.minecraftly.modules.homeworlds;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.packets.homes.PacketNoLongerHosting;
import com.minecraftly.modules.homeworlds.command.OwnerCommands;
import com.minecraftly.modules.homeworlds.data.global.GlobalStorageHandler;
import com.minecraftly.modules.homeworlds.data.world.WorldStorageHandler;
import com.minecraftly.modules.homeworlds.listener.DimensionListener;
import com.minecraftly.modules.homeworlds.listener.PlayerListener;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeWorldsModule extends Module implements Listener {

    private static HomeWorldsModule instance;

    public static HomeWorldsModule getInstance() {
        return instance;
    }

    private MinecraftlyCore plugin;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();

    @Override
    protected void onLoad(MinecraftlyCore plugin) {
        instance = this;
    }

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();

        PlayerListener playerListener = new PlayerListener(this);

        registerListener(this);
        registerListener(playerListener);
        registerListener(new DimensionListener(this));
        gateway.registerListener(playerListener);
        plugin.getPlayerSwitchJobManager().addJob(playerListener);

        UserManager userManager = plugin.getUserManager();
        GlobalStorageHandler globalStorageHandler = new GlobalStorageHandler(this);
        WorldStorageHandler worldStorageHandler = new WorldStorageHandler(this);
        userManager.addDataStorageHandler(globalStorageHandler);
        userManager.addDataStorageHandler(worldStorageHandler);
        registerListener(globalStorageHandler);
        registerListener(worldStorageHandler);
    }

    @Override
    protected void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new OwnerCommands(this));
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
        instance = null;
    }

    public MinecraftlyCore getPlugin() {
        return plugin;
    }

    public boolean isWorldLoaded(UUID worldUUID) {
        return playerWorlds.containsKey(worldUUID);
    }

    public boolean isHomeWorld(World world) {
        return playerWorlds.values().contains(world);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();
        String worldName = world.getName();

        try {
            UUID uuid = Utilities.convertFromNoDashes(worldName);
            playerWorlds.put(uuid, world);
        } catch (IllegalArgumentException ignored) { // todo slow?
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) { // do some cleaning up
        World world = e.getWorld();
        UUID ownerUUID = getHomeOwner(world);

        if (ownerUUID != null) {
            playerWorlds.remove(ownerUUID);
            gateway.sendPacket(new PacketNoLongerHosting(ownerUUID), false); // notify proxy if possible
            getLogger().info("Unloaded world for player: " + ownerUUID + ".");
        }
    }

    public UUID getHomeOwner(World world) {
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
            String uuidString = Utilities.convertToNoDashes(uuid);
            world = Bukkit.getWorld(uuidString);

            if (world == null) {
                WorldCreator worldCreator = new WorldCreator(uuidString);
                File worldDirectory = new File(Bukkit.getWorldContainer(), uuidString);

                if (worldDirectory.exists() && worldDirectory.isDirectory()) {
                    world = worldCreator.createWorld(); // this actually loads an existing world
                } else { // generate world async
                    // todo start generation from BungeeCord?
                    world = worldCreator.createWorld();
                }
            }
        }

        return world;
    }

}
