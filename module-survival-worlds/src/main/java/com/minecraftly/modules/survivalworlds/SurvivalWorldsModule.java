package com.minecraftly.modules.survivalworlds;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.config.DataValue;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.config.ConfigWrapper;
import com.minecraftly.core.packets.survivalworlds.PacketNoLongerHosting;
import com.minecraftly.modules.survivalworlds.data.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SurvivalWorldsModule extends Module implements Listener {

    private static SurvivalWorldsModule instance;

    public static final String LANGUAGE_KEY_PREFIX = "module.survivalWorlds";
    public static final String WORLD_NAME_PREFIX = ""; // disable prefix

    public static SurvivalWorldsModule getInstance() {
        return instance;
    }

    public static String getWorldName(UUID uuid) {
        return WORLD_NAME_PREFIX + uuid;
    }

    private MinecraftlyCore bukkitPlugin;
    private ServerGateway<Player> gateway;
    private DataStore dataStore;

    public final Map<UUID, World> playerWorlds = new HashMap<>();
    public final Map<World, ConfigWrapper> worldConfigs = new HashMap<>();

    public DataValue<String> CFG_GLOBAL_DIR = new DataValue<>(this, "../../../../global/mcly_homes/", String.class);

    public MinecraftlyCore getBukkitPlugin() {
        return bukkitPlugin;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    protected void onLoad(MinecraftlyCore plugin) {
        instance = this;
    }

    @Override
    protected void onEnable(MinecraftlyCore bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;
        this.gateway = bukkitPlugin.getGateway();

        bukkitPlugin.getConfigManager().register("homes.global-data-dir", CFG_GLOBAL_DIR);

        File survivalWorldsDirectory = new File(bukkitPlugin.getGeneralDataDirectory(), CFG_GLOBAL_DIR.getValue());
        File globalPlayersDirectory = new File(survivalWorldsDirectory, "global-data");
        globalPlayersDirectory.mkdirs();

        this.dataStore = new DataStore(this, globalPlayersDirectory);

        PlayerListener playerListener = new PlayerListener(this);

        gateway.registerListener(playerListener);
        registerListener(this);
        registerListener(playerListener);
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
        instance = null;
    }

    public UUID getWorldOwner(World world) {
        world = WorldDimension.getBaseWorld(world);
        String name = world.getName();
        UUID uuid = null;

        if (name.startsWith(WORLD_NAME_PREFIX)) {
            name = name.substring(WORLD_NAME_PREFIX.length(), name.length());

            try {
                uuid = UUID.fromString(name);
            } catch (IllegalArgumentException ignored) {}
        }

        return uuid;
    }

    public boolean isWorldLoaded(UUID worldUUID) {
        return playerWorlds.containsKey(worldUUID);
    }

    public boolean isSurvivalWorld(World world) {
        return playerWorlds.values().contains(world);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();
        String worldName = world.getName();

        if (worldName.startsWith(WORLD_NAME_PREFIX)) {
            String uuidString = worldName.substring(WORLD_NAME_PREFIX.length(), worldName.length());

            try {
                UUID uuid = UUID.fromString(uuidString);
                playerWorlds.put(uuid, world);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) { // do some cleaning up
        World world = e.getWorld();
        UUID ownerUUID = getWorldOwnerUUID(world);

        if (ownerUUID != null) {
            ConfigWrapper configWrapper = worldConfigs.get(world);
            if (configWrapper != null) {
                worldConfigs.remove(world);
            }

            playerWorlds.remove(ownerUUID);
            gateway.sendPacket(new PacketNoLongerHosting(ownerUUID), false); // notify proxy if possible
            getLogger().info("Unloaded world for player: " + ownerUUID);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) { // save world config when world is saved
        ConfigWrapper configWrapper = worldConfigs.get(e.getWorld());
        if (configWrapper != null) {
            configWrapper.saveConfig();
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
        }

        return world;
    }

}
