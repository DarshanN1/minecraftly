package com.minecraftly.modules.homeworlds;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.homes.PacketPlayerGotoHome;
import com.minecraftly.modules.homeworlds.data.world.WorldUserData;
import com.minecraftly.modules.homeworlds.data.world.WorldUserDataContainer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener, Consumer<Player> {

    public static final String LANGUAGE_KEY_PREFIX = HomeWorldsModule.getInstance().getLanguageSection();

    // todo convert these to language values for easier and faster access
    public static final String LANGUAGE_LOADING_OWNER = LANGUAGE_KEY_PREFIX + ".loading.owner";
    public static final String LANGUAGE_LOADING_GUEST = LANGUAGE_KEY_PREFIX + ".loading.guest";
    public static final String LANGUAGE_WELCOME_OWNER = LANGUAGE_KEY_PREFIX + ".welcome.owner";
    public static final String LANGUAGE_WELCOME_GUEST = LANGUAGE_KEY_PREFIX + ".welcome.guest";
    public static final String LANGUAGE_WELCOME_BOTH = LANGUAGE_KEY_PREFIX + ".welcome.both";
    public static final String LANGUAGE_OWNER_LEFT = LANGUAGE_KEY_PREFIX + ".ownerLeft";

    public static final String LANGUAGE_ERROR_KEY_PREFIX = LANGUAGE_KEY_PREFIX + ".error";
    public static final String LANGUAGE_LOAD_FAILED = LANGUAGE_ERROR_KEY_PREFIX + ".loadFailed";

    private HomeWorldsModule module;
    private LanguageManager languageManager;
    private UserManager userManager;

    public PlayerListener(final HomeWorldsModule module) {
        this.module = module;
        this.languageManager = module.getPlugin().getLanguageManager();
        this.userManager = module.getPlugin().getUserManager();

        languageManager.registerAll(new HashMap<String, LanguageValue>() {{
            put(LANGUAGE_LOADING_OWNER, new LanguageValue(module, "&bOne moment whilst we load your home."));
            put(LANGUAGE_LOADING_GUEST, new LanguageValue(module, "&bOne moment whilst we load that home."));
            put(LANGUAGE_WELCOME_OWNER, new LanguageValue(module, "&aWelcome back to your home, &6%s&a."));
            put(LANGUAGE_WELCOME_GUEST, new LanguageValue(module, "&aWelcome to &6%s&a's home, they will have to grant you permission before you can modify blocks."));
            put(LANGUAGE_WELCOME_BOTH, new LanguageValue(module, "&aYou can go back to chat mode by typing &6/chat&a."));
            put(LANGUAGE_LOAD_FAILED, new LanguageValue(module, "&cWe were unable to load your home, please contact a member of staff."));
            put(LANGUAGE_OWNER_LEFT, new LanguageValue(module, "&cThe owner of that world left."));
        }});
    }

    @PacketHandler
    public void onPacketJoinWorld(PacketPlayerGotoHome packet) {
        UUID playerUUID = packet.getPlayer();
        UUID worldUUID = packet.getWorld();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            joinWorld(player, worldUUID);
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

        WorldUserDataContainer worldUserDataContainer = userManager.getUser(player).getSingletonUserData(WorldUserDataContainer.class);
        WorldUserData worldUserData = worldUserDataContainer.getOrLoad(module.getHomeOwner(world));

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
        worldUserData.apply(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isHomeWorld(world)) {
            if (module.getHomeOwner(world).equals(player.getUniqueId())) {
                ownerLeftWorld(player, world);
            }

            checkWorldForUnloadDelayed(world);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        final Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        World from = WorldDimension.getBaseWorld(e.getFrom().getWorld());
        World to = WorldDimension.getBaseWorld(e.getTo().getWorld());

        if (!from.equals(to)) {
            checkWorldForUnloadDelayed(from);

            if (module.isHomeWorld(from) && module.getHomeOwner(from).equals(uuid)) {
                ownerLeftWorld(player, from);
            }

            if (module.isHomeWorld(to)) {
                final UUID owner = module.getHomeOwner(to);

                if (uuid.equals(owner)) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(languageManager.get(LANGUAGE_WELCOME_OWNER, player.getDisplayName()));
                    player.sendMessage(languageManager.get(LANGUAGE_WELCOME_BOTH));
                } else {
                    player.setGameMode(GameMode.ADVENTURE);

                    Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), new Runnable() { // async for getOfflinePlayer
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
            WorldUserDataContainer worldUserDataContainer = userManager.getUser(player).getSingletonUserData(WorldUserDataContainer.class);
            WorldUserData worldUserData = worldUserDataContainer.get(module.getHomeOwner(world));

            if (worldUserData != null) {
                // todo can't help but think this could all be shortened
                Location bedLocation = worldUserData.getBedLocation();
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
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), new Runnable() {
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

    public void ownerLeftWorld(Player owner, World world) {
        for (Player p : world.getPlayers()) {
            if (p != owner) {
                p.kickPlayer(languageManager.get(LANGUAGE_OWNER_LEFT)); // player will go to another server (fallback)
            }
        }
    }

    // fired when player is about to switch server
    @Override
    public void accept(Player player) {
        User user = userManager.getUser(player, false);
        if (user != null) {
            userManager.save(user);
        }
    }
}
