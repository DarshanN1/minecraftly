package com.minecraftly.core.bukkit.modules.playerworlds.handlers;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.minecraftly.core.bukkit.redis.CachedUUIDEntry;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.playerworlds.PacketPlayerGotoWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by Keir on 24/04/2015.
 */
public class PlayerListener implements Listener, Consumer<Player> {

    private final LanguageValue langWelcomeOwner = new LanguageValue("&aWelcome back to your world, &6%s&a.");
    private final LanguageValue langWelcomeGuest = new LanguageValue("&aWelcome to &6%s&a's world, they will have to grant you permission before you can modify blocks.");
    private final LanguageValue langPlayerJoinedWorld = new LanguageValue("&6%s &bhas joined.");
    private final LanguageValue langPlayerLeftWorld = new LanguageValue("&6%s &bhas left.");

    private ModulePlayerWorlds module;
    private UserManager userManager;

    public PlayerListener(final ModulePlayerWorlds module) {
        this.module = module;
        this.userManager = module.getPlugin().getUserManager();

        module.getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>() {{
            String prefix = PlayerListener.this.module.getLanguageSection();

            put(prefix + ".welcome.owner", langWelcomeOwner);
            put(prefix + ".welcome.guest", langWelcomeGuest);
            put(prefix + ".joinedWorld", langPlayerJoinedWorld);
            put(prefix + ".leftWorld", langPlayerLeftWorld);
        }});
    }

    @PacketHandler
    public void onPacketGotoWorld(PacketPlayerGotoWorld packet) {
        UUID playerUUID = packet.getPlayer();
        UUID worldUUID = packet.getWorld();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            module.joinWorld(player, worldUUID);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = WorldDimension.getBaseWorld(player.getWorld());
        leftWorld(player, world);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        final Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        World from = WorldDimension.getBaseWorld(e.getFrom().getWorld());
        World to = WorldDimension.getBaseWorld(e.getTo().getWorld());

        if (!from.equals(to)) {
            leftWorld(player, from);

            if (module.isPlayerWorld(to)) {
                final UUID owner = module.getWorldOwner(to);

                if (uuid.equals(owner)) {
                    langWelcomeOwner.send(player, player.getDisplayName());
                } else {
                    Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), new Runnable() { // async for getPlayerName which uses getOfflinePlayer and Redis
                        @Override
                        public void run() {
                            String ownerName = getPlayerName(owner);
                            langWelcomeGuest.send(player, ownerName);
                        }
                    });
                }

                BukkitUtilities.broadcast(WorldDimension.getPlayersAllDimensions(to), player, langPlayerJoinedWorld.getValue(player.getName()));
            }
        }
    }

    /**
     * Fetches a players name from their UUID using multiple sources.
     * This method should be run async.
     *
     * @param playerUUID the uuid of the player to get the name of
     * @return the players name (or the last one we're aware of)
     */
    public String getPlayerName(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        String name = offlinePlayer instanceof Player ? ((Player) offlinePlayer).getDisplayName() : offlinePlayer.getName();

        if (name == null) {
            try (Jedis jedis = module.getPlugin().getJedisService().getJedisPool().getResource()) {
                String storedJson = jedis.hget("uuid-cache", playerUUID.toString());

                if (storedJson != null) { // todo refactor this
                    CachedUUIDEntry cachedUUIDEntry = module.getPlugin().getGson().fromJson(storedJson, CachedUUIDEntry.class);
                    name = cachedUUIDEntry.getName();
                    // todo expensive lookup if expired
                }
            }
        }

        return name;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (module.isPlayerWorld(WorldDimension.getBaseWorld(e.getEntity().getWorld()))) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        World world = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isPlayerWorld(world)) {
            WorldUserDataContainer worldUserDataContainer = userManager.getUser(player).getSingletonUserData(WorldUserDataContainer.class);
            WorldUserData worldUserData = worldUserDataContainer.get(module.getWorldOwner(world));

            if (worldUserData != null) {
                // first, try home location
                Location respawnLocation = worldUserData.getHomeLocation();
                if (respawnLocation == null) {
                    // if that fails, try using the players LOCAL bed location
                    respawnLocation = player.getBedSpawnLocation();

                    // check the bed location is good and that it isn't in another world
                    if ((respawnLocation != null && !world.equals(WorldDimension.getBaseWorld(respawnLocation.getWorld()))) || respawnLocation == null) {
                        // if that fails, try the players stored bed location
                        respawnLocation = worldUserData.getBedLocation();

                        if (respawnLocation == null) {
                            // if all else fails, fallback to spawn location
                            respawnLocation = BukkitUtilities.getSafeSpawnLocation(world.getSpawnLocation());
                        }
                    }
                }

                e.setRespawnLocation(respawnLocation);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        World world = WorldDimension.getBaseWorld(e.getPlayer().getWorld());

        if (module.isPlayerWorld(world)) {
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
        if (module.isPlayerWorld(world) && WorldDimension.getPlayersAllDimensions(world).size() == 0) {
            Bukkit.unloadWorld(world, true); // unloads other dimensions too
        }
    }

    public void leftWorld(Player player, World baseWorld) {
        if (module.isPlayerWorld(baseWorld)) {
            BukkitUtilities.broadcast(WorldDimension.getPlayersAllDimensions(baseWorld), player, langPlayerLeftWorld.getValue(player.getName()));
        }

        checkWorldForUnloadDelayed(baseWorld);
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
