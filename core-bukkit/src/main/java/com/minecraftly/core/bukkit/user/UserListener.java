package com.minecraftly.core.bukkit.user;

import com.minecraftly.core.bukkit.PlayerSwitchJobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;

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
    public void onPlayerJoin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            userManager.load(e.getUniqueId());
        }
    }

    @Override
    public void accept(Player player) {
        userManager.unload(userManager.getUser(player));
    }
}
