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
import com.minecraftly.core.bukkit.modules.playerworlds.task.JoinCountdownTask;
import com.minecraftly.core.bukkit.modules.playerworlds.task.RSyncUploadWorldTask;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.playerworlds.PacketNoLongerHostingWorld;
import com.minecraftly.core.utilities.ComputeEngineHelper;
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
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

public class ModulePlayerWorlds extends Module implements Listener {

    private static ModulePlayerWorlds instance;

    public static ModulePlayerWorlds getInstance() {
        return instance;
    }

    private HumanCheck humanCheck;
    private ServerGateway<Player> gateway;

    public final Map<UUID, World> playerWorlds = new HashMap<>();
    private final AtomicBoolean disabling = new AtomicBoolean(false);

    private final LanguageValue langLoadingOwner = new LanguageValue("&bOne moment whilst we load your world.");
    private final LanguageValue langLoadingGuest = new LanguageValue("&bOne moment whilst we load that world.");
    public final LanguageValue langLoadStillLoading = new LanguageValue("&bThis is taking longer than expected...");
    public final LanguageValue langLoadFailed = new LanguageValue("&cWe were unable to load that world, please contact a member of staff.");
    private final LanguageValue langLoaded = new LanguageValue("&bWorld has been loaded, please wait...");
    private final LanguageValue langTeleportCountdown = new LanguageValue("&bTeleporting in &6%s &bseconds.");

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
            put(getLanguageSection() + ".loading.stillLoading", langLoadStillLoading);
            put(getLanguageSection() + ".error.loadFailed", langLoadFailed);
            put(getLanguageSection() + ".loaded.message", langLoaded);
            put(getLanguageSection() + ".loaded.teleportCountdown", langTeleportCountdown);
        }});
    }

    private void runAsyncUnlessDisabling(Runnable runnable) {
        if (disabling.get()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), runnable);
        }
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
        disabling.set(true);

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

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        World world = e.getWorld();
        UUID ownerUUID = getWorldOwner(world);

        if (ownerUUID == null) {
            try {
                ownerUUID = UUID.fromString(world.getName()); // manually parse since cached value may have been removed
            } catch (IllegalArgumentException ignored) {
                return;
            }
        }

        runAsyncUnlessDisabling(new RSyncUploadWorldTask(world, ownerUUID, !playerWorlds.containsKey(ownerUUID), getLogger()));
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
            throw new IllegalStateException("Player world is not loaded.");
        }

        return world;
    }

    public void delayedJoinWorld(Player player) {
        delayedJoinWorld(player, player.getUniqueId());
    }

    public void delayedJoinWorld(Player player, UUID worldUUID) {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(worldUUID);

        UUID playerUUID = player.getUniqueId();

        if (!isWorldLoaded(worldUUID)) {
            if (playerUUID.equals(worldUUID)) {
                langLoadingOwner.send(player);
            } else {
                langLoadingGuest.send(player);
            }
        }

        langLoaded.send(player);
        JoinCountdownTask joinCountdownTask = new JoinCountdownTask(this, langTeleportCountdown, getPlugin().getUserManager().getUser(player));
        loadWorld(worldUUID.toString(), World.Environment.NORMAL, joinCountdownTask);
        joinCountdownTask.runTaskTimer(getPlugin(), 20L, 20L);
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

        world.getChunkAtAsync(spawnLocation, chunk -> {
            player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        });
    }

    public World getWorld(String worldName) {
        return Bukkit.getWorld(worldName);
    }

    public void loadWorld(String worldName, World.Environment environment, Consumer<World> consumer) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        World existingWorld = getWorld(worldName);

        if (existingWorld == null) {
            scheduler.runTaskAsynchronously(getPlugin(), () -> {
                File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);

                try {
                    if (ComputeEngineHelper.worldExists(worldName)) {
                        if (!worldDirectory.exists() && !worldDirectory.mkdir()) {
                            getLogger().severe("Error creating directory: " + worldDirectory.getPath() + ".");
                        }

                        ComputeEngineHelper.rsync("gs://worlds/" + worldName, worldDirectory.getCanonicalPath());

                        File sessionLockFile = new File(worldDirectory, "session.lock");
                        if (sessionLockFile.exists() && !sessionLockFile.delete()) {
                            getLogger().warning("Unable to delete session.lock file, world will fail to load.");
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    getLogger().log(Level.SEVERE, "Error retrieving existing world from cloud storage.", e);

                    if (consumer != null) {
                        scheduler.runTask(getPlugin(), () -> consumer.accept(null));
                    }

                    return;
                }

                if (worldDirectory.exists() && !worldDirectory.isDirectory()) {
                    throw new IllegalArgumentException(worldDirectory.getPath() + " exists, but is not a directory.");
                }

                scheduler.runTask(getPlugin(), () -> {
                    World world = new WorldCreator(worldName).environment(environment).createWorld();
                    initializeWorld(world);

                    if (consumer != null) {
                        consumer.accept(world);
                    }
                });
            });
        } else if (consumer != null) {
            scheduler.runTask(getPlugin(), () -> consumer.accept(existingWorld));
        }
    }

    public void initializeWorld(World world) {
        world.setGameRuleValue("mobGriefing", "false");
    }

}
