package com.minecraftly.core.bukkit.modules.homes;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.Module;
import com.minecraftly.core.bukkit.modules.homes.command.WorldsCommands;
import com.minecraftly.core.bukkit.modules.homes.data.global.GlobalStorageHandler;
import com.minecraftly.core.bukkit.modules.homes.data.world.WorldStorageHandler;
import com.minecraftly.core.bukkit.modules.homes.data.world.WorldUserData;
import com.minecraftly.core.bukkit.modules.homes.data.world.WorldUserDataContainer;
import com.minecraftly.core.bukkit.modules.homes.handlers.DimensionListener;
import com.minecraftly.core.bukkit.modules.homes.handlers.PlayerListener;
import com.minecraftly.core.bukkit.modules.homes.handlers.WorldMessagesListener;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.homes.PacketNoLongerHosting;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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

public class ModulePlayerWorlds extends Module implements Listener {

    private static ModulePlayerWorlds instance;

    public static ModulePlayerWorlds getInstance() {
        return instance;
    }

    private BotCheck botCheck;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();

    private final LanguageValue langLoadingOwner = new LanguageValue("&bOne moment whilst we load your home.");
    private final LanguageValue langLoadingGuest = new LanguageValue("&bOne moment whilst we load that home.");
    private final LanguageValue langLoadFailed = new LanguageValue("&cWe were unable to load your home, please contact a member of staff.");

    public ModulePlayerWorlds(MclyCoreBukkitPlugin plugin) {
        super("PlayerWorlds", plugin);
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.gateway = getPlugin().getGateway();
        this.botCheck = new BotCheck(this);
        registerListener(this.botCheck);
        Bukkit.getScheduler().runTaskTimer(getPlugin(), this.botCheck, 20L, 20L);

        PlayerListener playerListener = new PlayerListener(this);

        registerListener(this);
        registerListener(playerListener);
        registerListener(new DimensionListener(this));
        registerListener(new WorldMessagesListener(this));
        gateway.registerListener(playerListener);
        getPlugin().getPlayerSwitchJobManager().addJob(playerListener);

        UserManager userManager = getPlugin().getUserManager();
        GlobalStorageHandler globalStorageHandler = new GlobalStorageHandler(this);
        WorldStorageHandler worldStorageHandler = new WorldStorageHandler(this);
        userManager.addDataStorageHandler(globalStorageHandler);
        userManager.addDataStorageHandler(worldStorageHandler);
        registerListener(globalStorageHandler);
        registerListener(worldStorageHandler);

        userManager.addDataStorageHandler(new BotCheckStatusDataStorageHandler());

        getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>(){{
            put(getLanguageSection() + ".loading.owner", langLoadingOwner);
            put(getLanguageSection() + ".loading.guest", langLoadingGuest);
            put(getLanguageSection() + ".error.loadFailed", langLoadFailed);
        }});
    }

    @Override
    public void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new OwnerCommands(this));
        dispatcherNode.registerMethods(new WorldsCommands(this, getPlugin().getLanguageManager()));
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(ChatColor.RED + "Slave server is shutting down.");
        }

        for (World world : playerWorlds.values()) {
            Bukkit.unloadWorld(world, true);
        }

        instance = null;
    }

    public boolean isWorldLoaded(UUID worldUUID) {
        return playerWorlds.containsKey(worldUUID);
    }

    public boolean isHomeWorld(World world) {
        return playerWorlds.values().contains(world);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(getPlugin(), new Runnable() {
            @Override
            public void run() {
                botCheck.showHumanCheck(e.getPlayer());
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

    public World getWorld(OfflinePlayer offlinePlayer) {
        return getWorld(offlinePlayer.getUniqueId());
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

                world.setGameRuleValue("mobGriefing", "false");
            }
        }

        return world;
    }

    public void joinWorld(Player player, OfflinePlayer offlinePlayer) {
        joinWorld(player, offlinePlayer.getUniqueId());
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
        Bukkit.getScheduler().runTaskLater(getPlugin(), new Runnable() {
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

        WorldUserDataContainer worldUserDataContainer = getPlugin().getUserManager().getUser(player).getSingletonUserData(WorldUserDataContainer.class);
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
            spawnLocation = BukkitUtilities.getSafeSpawnLocation(world.getSpawnLocation());
        }

        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

}
