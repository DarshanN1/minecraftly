package com.minecraftly.core.bukkit.modules.playerworlds;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.PlayerWorldsRepository;
import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.Module;
import com.minecraftly.core.bukkit.modules.playerworlds.command.*;
import com.minecraftly.core.bukkit.modules.playerworlds.data.global.GlobalStorageHandler;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldStorageHandler;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.DimensionListener;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.PlayerListener;
import com.minecraftly.core.bukkit.modules.playerworlds.handlers.WorldMessagesListener;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModulePlayerWorlds extends Module implements Listener {

    private static ModulePlayerWorlds instance;

    public static ModulePlayerWorlds getInstance() {
        return instance;
    }

    private ServerGateway<Player> gateway;
    private final PlayerWorldsRepository playerWorldsRepository;

    public final Map<UUID, World> playerWorlds = new HashMap<>();
    private final AtomicBoolean disabling = new AtomicBoolean(false);

    private final LanguageValue langLoadingOwner = new LanguageValue("&bOne moment whilst we load your world.");
    private final LanguageValue langLoadingGuest = new LanguageValue("&bOne moment whilst we load that world.");
    public final LanguageValue langLoadStillLoading = new LanguageValue("&bThis is taking longer than expected...");
    public final LanguageValue langLoadFailed = new LanguageValue("&cWe were unable to load that world, please contact a member of staff.");
    private final LanguageValue langLoaded = new LanguageValue("&bWorld has been loaded, please wait...");
    private final LanguageValue langTeleportCountdown = new LanguageValue("&bTeleporting in &6%s &bseconds.");
    public final LanguageValue langNotOwner = new LanguageValue("&cYou are not the owner of this world.");
    private final LanguageValue langCannotCreateOthersWorld = new LanguageValue("&cYou cannot create a world on behalf of another player.");
    public final LanguageValue langCannotUseCommandHere = new LanguageValue("&cYou cannot use that command here.");

    public ModulePlayerWorlds(MclyCoreBukkitPlugin plugin) {
        super("PlayerWorlds", plugin);
        this.playerWorldsRepository = new PlayerWorldsRepository(plugin.getJedisService().getJedisPool());
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.gateway = getPlugin().getGateway();

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

        languageManager.registerAll(new HashMap<String, LanguageValue>(){{
            put(getLanguageSection() + ".loading.owner", langLoadingOwner);
            put(getLanguageSection() + ".loading.guest", langLoadingGuest);
            put(getLanguageSection() + ".loading.stillLoading", langLoadStillLoading);
            put(getLanguageSection() + ".error.loadFailed", langLoadFailed);
            put(getLanguageSection() + ".loaded.message", langLoaded);
            put(getLanguageSection() + ".loaded.teleportCountdown", langTeleportCountdown);
            put(getLanguageSection() + ".error.notOwner", langNotOwner);
            put(getLanguageSection() + ".error.cannotUseCommandHere", langCannotUseCommandHere);
            put(getLanguageSection() + ".error.cannotCreateOthersWorld", langCannotCreateOthersWorld);
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
        registerListener(worldsCommands);

        dispatcherNode.registerMethods(new SpawnCommands(this));
        dispatcherNode.registerMethods(new HomeCommands(this));

        BackCommand backCommand = new BackCommand(this);
        dispatcherNode.registerMethods(backCommand);
        registerListener(backCommand);
    }

    @Override
    public void onDisable() {
        disabling.set(true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(null);
        }

        for (World world : playerWorlds.values()) {
            Bukkit.unloadWorld(world, true);
            unload(world);
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
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();
        String worldName = world.getName();

        try {
            UUID worldUUID = UUID.fromString(worldName);
            playerWorlds.put(worldUUID, world);
            setWorldHosted(worldUUID, true);
        } catch (IllegalArgumentException ignored) { // todo slow?
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) { // do some cleaning up
        unload(e.getWorld());
    }

    public void unload(World world) {
        UUID worldUUID = getWorldOwner(world);

        if (worldUUID != null) {
            playerWorlds.remove(worldUUID);
            setWorldHosted(worldUUID, false);
            getLogger().info("Unloaded world for player: " + worldUUID + ".");
        }
    }

    public void setWorldHosted(UUID worldUUID, boolean hosted) {
        String serverName = getPlugin().getComputeUniqueId();

        if (hosted) {
            playerWorldsRepository.setServer(worldUUID, serverName);
        } else {
            String existingServerName = playerWorldsRepository.getServer(worldUUID);

            if (serverName.equals(existingServerName)) {
                playerWorldsRepository.setServer(worldUUID, null);
            }
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
            throw new IllegalStateException("Player world is not loaded.");
        }

        return world;
    }

    public void joinWorld(Player player) {
        joinWorld(player, player.getUniqueId());
    }

    public void joinWorld(Player player, UUID worldUUID) {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(worldUUID);

        UUID playerUUID = player.getUniqueId();
        String worldName = worldUUID.toString();

        if (!isWorldLoaded(worldUUID)) {
            if (playerUUID.equals(worldUUID)) {
                langLoadingOwner.send(player);
            } else {
                langLoadingGuest.send(player);
            }
        }

        if (getWorld(worldName) == null) {
            langLoaded.send(player);
        }

        if (canLoadWorld(player, worldUUID)) {
            World world = loadWorld(worldUUID.toString(), World.Environment.NORMAL);
            spawnInWorld(player, world);
        } else {
            langCannotCreateOthersWorld.send(player);
            joinWorld(player); // take them to their own world
        }
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

        BukkitUtilities.asyncLoadAndTeleport(player, spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public World getWorld(String worldName) {
        return Bukkit.getWorld(worldName);
    }

    /**
     * If a player tries to create a world that they don't own, don't allow it.
     *
     * @param player the player attempting to load a world
     * @param worldUUID the owner of the world
     * @return whether the world can be loaded (or created)
     */
    public boolean canLoadWorld(Player player, UUID worldUUID) {
        if (!player.getUniqueId().equals(worldUUID)) {
            File existingWorldFile = new File(Bukkit.getWorldContainer(), worldUUID.toString());

            if (!existingWorldFile.exists()) {
                return false;
            }
        }

        return true;
    }

    public World loadWorld(String worldName, World.Environment environment) {
        World existingWorld = getWorld(worldName);

        if (existingWorld == null) {
            File worldDirectory = new File(Bukkit.getWorldContainer(), worldName);

            if (worldDirectory.exists() && !worldDirectory.isDirectory()) {
                throw new IllegalArgumentException(worldDirectory.getPath() + " exists, but is not a directory.");
            }

            World world = new WorldCreator(worldName).environment(environment).createWorld();
            initializeWorld(world);
            return world;
        } else {
            return existingWorld;
        }
    }

    public void initializeWorld(World world) {
        world.setGameRuleValue("mobGriefing", "false");
    }

}
