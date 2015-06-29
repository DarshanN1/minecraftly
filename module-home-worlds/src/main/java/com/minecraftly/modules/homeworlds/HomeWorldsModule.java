package com.minecraftly.modules.homeworlds;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.homes.PacketNoLongerHosting;
import com.minecraftly.modules.homeworlds.bot.BotCheck;
import com.minecraftly.modules.homeworlds.bot.BotCheckStatusDataStorageHandler;
import com.minecraftly.modules.homeworlds.command.OwnerCommands;
import com.minecraftly.modules.homeworlds.data.global.GlobalStorageHandler;
import com.minecraftly.modules.homeworlds.data.world.WorldStorageHandler;
import com.minecraftly.modules.homeworlds.data.world.WorldUserData;
import com.minecraftly.modules.homeworlds.data.world.WorldUserDataContainer;
import com.minecraftly.modules.homeworlds.handlers.DimensionListener;
import com.minecraftly.modules.homeworlds.handlers.PlayerListener;
import com.minecraftly.modules.homeworlds.handlers.WorldMessagesListener;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
    private BotCheck botCheck;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();

    private final LanguageValue langLoadingOwner = new LanguageValue("&bOne moment whilst we load your home.");
    private final LanguageValue langLoadingGuest = new LanguageValue("&bOne moment whilst we load that home.");
    private final LanguageValue langLoadFailed = new LanguageValue("&cWe were unable to load your home, please contact a member of staff.");

    @Override
    protected void onLoad(MinecraftlyCore plugin) {
        instance = this;
    }

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();
        this.botCheck = new BotCheck(this);
        registerListener(this.botCheck);

        PlayerListener playerListener = new PlayerListener(this);

        registerListener(this);
        registerListener(playerListener);
        registerListener(new DimensionListener(this));
        registerListener(new WorldMessagesListener(this));
        gateway.registerListener(playerListener);
        plugin.getPlayerSwitchJobManager().addJob(playerListener);

        UserManager userManager = plugin.getUserManager();
        GlobalStorageHandler globalStorageHandler = new GlobalStorageHandler(this);
        WorldStorageHandler worldStorageHandler = new WorldStorageHandler(this);
        userManager.addDataStorageHandler(globalStorageHandler);
        userManager.addDataStorageHandler(worldStorageHandler);
        registerListener(globalStorageHandler);
        registerListener(worldStorageHandler);

        userManager.addDataStorageHandler(new BotCheckStatusDataStorageHandler());

        plugin.getLanguageManager().registerAll(new HashMap<String, LanguageValue>(){{
            put(getLanguageSection() + ".loading.owner", langLoadingOwner);
            put(getLanguageSection() + ".loading.guest", langLoadingGuest);
            put(getLanguageSection() + ".error.loadFailed", langLoadFailed);
        }});
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
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                botCheck.checkBot(e.getPlayer());
            }
        }, 5L);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();
        String worldName = world.getName();

        try {
            playerWorlds.put(UUID.fromString(worldName), world);
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
            String uuidString = uuid.toString();
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

    public void joinWorld(Player player, UUID worldUUID) {
        if (!isWorldLoaded(worldUUID)) {
            if (player.getUniqueId().equals(worldUUID)) {
                langLoadingOwner.send(player);
            } else {
                langLoadingGuest.send(player);
            }
        }

        World world = getWorld(worldUUID);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                joinWorld(player, world);
            }
        }, 20L * 2);
    }

    public void joinWorld(Player player, World world) {
        Preconditions.checkNotNull(player);
        UUID playerUUID = player.getUniqueId();

        if (world == null) {
            langLoadFailed.send(player);
            return;
        }

        WorldUserDataContainer worldUserDataContainer = plugin.getUserManager().getUser(player).getSingletonUserData(WorldUserDataContainer.class);
        WorldUserData worldUserData = worldUserDataContainer.getOrLoad(getHomeOwner(world));

        Location lastLocation = worldUserData.getLastLocation();
        Location bedLocation = worldUserData.getBedLocation();
        Location spawnLocation;

        // todo util method for player data
        if (lastLocation != null) {
            spawnLocation = lastLocation;
        } else if (bedLocation != null) {
            spawnLocation = bedLocation;
        } else {
            spawnLocation = BukkitUtilities.getSafeLocation(world.getSpawnLocation());
        }

        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

}
