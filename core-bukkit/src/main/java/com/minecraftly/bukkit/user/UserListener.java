package com.minecraftly.bukkit.user;

import com.minecraftly.bukkit.PlayerSwitchJobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by Keir on 01/06/2015.
 */
public class UserListener implements Listener, Consumer<Player> {

    public static UserListener initialize(Plugin plugin, UserManager userManager, PlayerSwitchJobManager playerSwitchJobManager) {
        UserListener userListener = new UserListener(userManager);
        Bukkit.getPluginManager().registerEvents(userListener, plugin);
        playerSwitchJobManager.addJob(userListener);
        return userListener;
    }

    private UserManager userManager;

    private UserListener(UserManager userManager) {
        this.userManager = userManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        if (!userManager.isUserLoaded(uuid)) {
            userManager.load(uuid);
        }
    }

    @Override
    public void accept(Player player) {
        User user = userManager.getUser(player, false);
        if (user != null) {
            userManager.save(user);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        User user = userManager.getUser(e.getPlayer(), false);
        if (user != null) {
            userManager.unload(user, false);
        }
    }
}
