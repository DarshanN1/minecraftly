package com.minecraftly.core.bukkit.modules.playerworlds;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.Module;
import com.minecraftly.core.bukkit.modules.playerworlds.command.WorldsCommands;
import com.minecraftly.core.bukkit.modules.playerworlds.data.global.GlobalStorageHandler;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldStorageHandler;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.DimensionListener;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.PlayerListener;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.WorldMessagesListener;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.playerworlds.PacketNoLongerHostingWorld;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.Bukkit;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModulePlayerWorlds extends Module implements Listener {

    private static ModulePlayerWorlds instance;

    public static ModulePlayerWorlds getInstance() {
        return instance;
    }

    private HumanCheck humanCheck;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();

    private final LanguageValue langLoadingOwner = new LanguageValue("&bOne moment whilst we load your world.");
    private final LanguageValue langLoadingGuest = new LanguageValue("&bOne moment whilst we load that world.");
    private final LanguageValue langLoadFailed = new LanguageValue("&cWe were unable to load that world, please contact a member of staff.");
    private final LanguageValue langLoaded = new LanguageValue("&bWorld has been loaded, please wait...");
    private final LanguageValue langTeleportCountdown = new LanguageValue("&bTeleporting in &6%s &bseconds.");

    private final LanguageValue langWorldGenerating = new LanguageValue("&5A world is currently being generated, you may experience some lag.");
    private final LanguageValue langWorldGenerated = new LanguageValue("&5World generation finished, server should be back to normal.");

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
        this.humanCheck = new HumanCheck(this);
        registerListener(this.humanCheck);
        Bukkit.getScheduler().runTaskTimer(getPlugin(), this.humanCheck, 20L, 20L);

        LanguageManager languageManager = getPlugin().getLanguageManager();
        PlayerListener playerListener = new PlayerListener(this);

        registerListener(this);
        registerListener(playerListener);
        registerListener(new DimensionListener(this, languageManager, getPlugin().getPermission()));
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

        userManager.addDataStorageHandler(new HumanCheckStatusDataStorageHandler());

        languageManager.registerAll(new HashMap<String, LanguageValue>(){{
            put(getLanguageSection() + ".loading.owner", langLoadingOwner);
            put(getLanguageSection() + ".loading.guest", langLoadingGuest);
            put(getLanguageSection() + ".error.loadFailed", langLoadFailed);
            put(getLanguageSection() + ".loaded.message", langLoaded);
            put(getLanguageSection() + ".loaded.teleportCountdown", langTeleportCountdown);

            put(getLanguageSection() + ".world.generating", langWorldGenerating);
            put(getLanguageSection() + ".world.generated", langWorldGenerated);
        }});
    }

    @Override
    public void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new OwnerCommands(this));

        WorldsCommands worldsCommands = new WorldsCommands(this, getPlugin().getUserManager(), getPlugin().getLanguageManager());
        dispatcherNode.registerMethods(worldsCommands);
        Bukkit.getPluginManager().registerEvents(worldsCommands, getPlugin());
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(null);
        }

        for (World world : playerWorlds.values()) {
            Bukkit.unloadWorld(world, true);
        }

        instance = null;
    }

    public boolean isWorldLoaded(UUID worldUUID) {
        return playerWorlds.containsKey(worldUUID);
    }

    public boolean isPlayerWorld(World world) {
        return playerWorlds.values().contains(world);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(getPlugin(), new Runnable() {
            @Override
            public void run() {
                humanCheck.showHumanCheck(e.getPlayer());
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
        UUID ownerUUID = getWorldOwner(world);

        if (ownerUUID != null) {
            playerWorlds.remove(ownerUUID);
            gateway.sendPacket(new PacketNoLongerHostingWorld(ownerUUID), false); // notify proxy if possible
            getLogger().info("Unloaded world for player: " + ownerUUID + ".");
        }
    }

    public UUID getWorldOwner(World world) {
        for (Map.Entry<UUID, World> entry : playerWorlds.entrySet()) {
            if (entry.getValue().equals(world)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public World getPlayerWorld(OfflinePlayer offlinePlayer) {
        return getPlayerWorld(offlinePlayer.getUniqueId());
    }

    public World getPlayerWorld(UUID uuid) {
        World world = playerWorlds.get(uuid);

        if (world == null) {
            world = getOrLoadWorld(uuid.toString(), World.Environment.NORMAL);
        }

        return world;
    }

    public void delayedJoinWorld(Player player, OfflinePlayer offlinePlayer) {
        delayedJoinWorld(player, offlinePlayer.getUniqueId());
    }

    public void delayedJoinWorld(Player player, UUID worldUUID) {
        if (!isWorldLoaded(worldUUID)) {
            if (player.getUniqueId().equals(worldUUID)) {
                langLoadingOwner.send(player);
            } else {
                langLoadingGuest.send(player);
            }
        }

        delayedJoinWorld(player, getPlayerWorld(worldUUID));
    }

    public void delayedJoinWorld(Player player, World world) {
        Preconditions.checkNotNull(player);
        UUID playerUUID = player.getUniqueId();

        if (world == null) {
            langLoadFailed.send(player);
            return;
        }

        langLoaded.send(player);

        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                langTeleportCountdown.send(player, countdown--);

                if (countdown <= 0) {
                    spawnInWorld(player, world);
                    cancel();
                }
            }
        }.runTaskTimer(getPlugin(), 20L, 20L);
    }

    public void spawnInWorld(Player player, World world) {
        WorldUserDataContainer worldUserDataContainer = getPlugin().getUserManager().getUser(player).getSingletonUserData(WorldUserDataContainer.class);
        WorldUserData worldUserData = worldUserDataContainer.getOrLoad(getWorldOwner(world));

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

    public World getOrLoadWorld(String worldName, World.Environment environment) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);
            boolean generating = !worldDirectory.exists();

            if (generating) {
                langWorldGenerating.broadcast();
            } else if (!worldDirectory.isDirectory()) {
                throw new IllegalArgumentException(worldDirectory.getPath() + " exists, but is not a directory.");
            }

            world = new WorldCreator(worldName).environment(environment).createWorld();
            initializeWorld(world);

            if (generating) {
                langWorldGenerated.broadcast();
            }
        }

        return world;
    }

    public void initializeWorld(World world) {
        world.setGameRuleValue("mobGriefing", "false");
    }

}
