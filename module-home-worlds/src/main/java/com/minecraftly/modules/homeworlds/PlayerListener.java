package com.minecraftly.modules.homeworlds;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.survivalworlds.PacketPlayerWorld;
import com.minecraftly.modules.homeworlds.data.DataStore;
import com.minecraftly.modules.homeworlds.data.PlayerWorldData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener {

    public static final String LANGUAGE_KEY_PREFIX = HomeWorldsModule.LANGUAGE_KEY_PREFIX;

    // todo convert these to language values for easier and faster access
    public static final String LANGUAGE_LOADING_OWNER = LANGUAGE_KEY_PREFIX + ".loading.owner";
    public static final String LANGUAGE_LOADING_GUEST = LANGUAGE_KEY_PREFIX + ".loading.guest";
    public static final String LANGUAGE_WELCOME_OWNER = LANGUAGE_KEY_PREFIX + ".welcome.owner";
    public static final String LANGUAGE_WELCOME_GUEST = LANGUAGE_KEY_PREFIX + ".welcome.guest";
    public static final String LANGUAGE_WELCOME_BOTH = LANGUAGE_KEY_PREFIX + ".welcome.both";

    public static final String LANGUAGE_ERROR_KEY_PREFIX = LANGUAGE_KEY_PREFIX + ".error";
    public static final String LANGUAGE_LOAD_FAILED = LANGUAGE_ERROR_KEY_PREFIX + ".loadFailed";

    private HomeWorldsModule module;
    private LanguageManager languageManager;
    private DataStore dataStore;
    private Cache<UUID, UUID> joinQueue = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public PlayerListener(final HomeWorldsModule module) {
        this.module = module;
        this.languageManager = module.getBukkitPlugin().getLanguageManager();
        this.dataStore = module.getDataStore();

        languageManager.registerAll(new HashMap<String, LanguageValue>() {{
            put(LANGUAGE_LOADING_OWNER, new LanguageValue(module, "&bOne moment whilst we load your home."));
            put(LANGUAGE_LOADING_GUEST, new LanguageValue(module, "&bOne moment whilst we load that home."));
            put(LANGUAGE_WELCOME_OWNER, new LanguageValue(module, "&aWelcome back to your home, &6%s&a."));
            put(LANGUAGE_WELCOME_GUEST, new LanguageValue(module, "&aWelcome to &6%s&a's home, they will have to grant you permission before you can modify blocks."));
            put(LANGUAGE_WELCOME_BOTH, new LanguageValue(module, "&aYou can go back to chat mode by typing &6%s/chat&a."));
            put(LANGUAGE_LOAD_FAILED, new LanguageValue(module, "&cWe were unable to load your home, please contact a member of staff."));
        }});
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();
        final UUID worldUUID = joinQueue.getIfPresent(playerUUID);

        if (worldUUID != null) {
            Bukkit.getScheduler().runTask(module.getBukkitPlugin(), new Runnable() {
                @Override
                public void run() {
                    joinWorld(player, module.getWorld(worldUUID));
                }
            });
        }
    }

    @PacketHandler
    public void onPacketJoinWorld(PacketPlayerWorld packet) {
        UUID playerUUID = packet.getPlayer();
        UUID worldUUID = packet.getWorld();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            joinWorld(player, worldUUID);
        } else if (!playerUUID.equals(worldUUID)) {
            joinQueue.put(playerUUID, worldUUID);
        }
    }

    public void joinWorld(Player player, UUID worldUUID) {
        if (!module.isWorldLoaded(worldUUID)) {
            if (player.getUniqueId().equals(worldUUID)) {
                player.sendMessage(languageManager.get(LANGUAGE_LOADING_OWNER));
            } else {
                player.sendMessage(languageManager.get(LANGUAGE_LOADING_GUEST));
            }
        }

        joinWorld(player, module.getWorld(worldUUID));
    }

    public void joinWorld(Player player, World world) {
        Preconditions.checkNotNull(player);
        UUID playerUUID = player.getUniqueId();

        if (world == null) {
            player.sendMessage(languageManager.get(LANGUAGE_LOAD_FAILED));
            return;
        }

        joinQueue.invalidate(playerUUID);
        Location spawnLocation;
        PlayerWorldData playerWorldData = dataStore.getPlayerWorldData(world, player);

        // todo util method for player data
        if (playerWorldData != null && playerWorldData.getLastLocation() != null) {
            spawnLocation = playerWorldData.getLastLocation();
        } else {
            spawnLocation = BukkitUtilities.getSafeLocation(world.getSpawnLocation());
        }

        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        checkWorldForUnloadDelayed(WorldDimension.getBaseWorld(e.getPlayer().getWorld()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        final Player player = e.getPlayer();
        World from = WorldDimension.getBaseWorld(e.getFrom().getWorld());
        World to = WorldDimension.getBaseWorld(e.getTo().getWorld());

        if (!from.equals(to)) {
            checkWorldForUnloadDelayed(from);

            if (module.isHomeWorld(to)) {
                final UUID owner = module.getWorldOwner(to);

                if (player.getUniqueId().equals(owner)) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(languageManager.get(LANGUAGE_WELCOME_OWNER, player.getDisplayName()));
                    player.sendMessage(languageManager.get(LANGUAGE_WELCOME_BOTH));
                } else {
                    player.setGameMode(GameMode.ADVENTURE);

                    Bukkit.getScheduler().runTaskAsynchronously(module.getBukkitPlugin(), new Runnable() { // async for getOfflinePlayer
                        @Override
                        public void run() {
                            player.sendMessage(languageManager.get(LANGUAGE_WELCOME_GUEST, Bukkit.getOfflinePlayer(owner).getName()));
                            player.sendMessage(languageManager.get(LANGUAGE_WELCOME_BOTH));
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        World world = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isHomeWorld(world)) {
            PlayerWorldData playerWorldData = dataStore.getPlayerWorldData(world, player);
            if (playerWorldData != null) {
                // todo can't help but think this could all be shortened
                Location bedLocation = playerWorldData.getBedLocation();
                if (bedLocation == null) {
                    bedLocation = player.getBedSpawnLocation();

                    if (bedLocation != null && !world.equals(WorldDimension.getBaseWorld(bedLocation.getWorld()))) { // if bed location is in another "server"
                        bedLocation = null;
                    }
                }

                if (bedLocation != null) {
                    e.setRespawnLocation(bedLocation);
                } else {
                    e.setRespawnLocation(BukkitUtilities.getSafeLocation(world.getSpawnLocation()));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        World world = WorldDimension.getBaseWorld(e.getPlayer().getWorld());

        if (module.isHomeWorld(world)) {
            Set<Player> recipients = e.getRecipients();
            recipients.clear();
            recipients.addAll(WorldDimension.getPlayersAllDimensions(world));
        }
    }

    public void checkWorldForUnloadDelayed(final World world) {
        Bukkit.getScheduler().runTaskLater(module.getBukkitPlugin(), new Runnable() {
            @Override
            public void run() {
                checkWorldForUnload(world);
            }
        }, 5L);
    }

    public void checkWorldForUnload(World world) {
        if (module.isHomeWorld(world) && WorldDimension.getPlayersAllDimensions(world).size() == 0) {
            Bukkit.unloadWorld(world, true);

            for (WorldDimension worldDimension : WorldDimension.values()) {
                World world1 = worldDimension.convertTo(world);
                if (world1 != null) Bukkit.unloadWorld(world1, true);
            }
        }
    }

}
