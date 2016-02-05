package com.minecraftly.bukkit.modules.playerworlds.handlers;

import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.utilities.BukkitUtilities;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by Keir on 20/06/2015.
 */
public class WorldMessagesListener implements Listener {

    private ModulePlayerWorlds module;

    public WorldMessagesListener(ModulePlayerWorlds module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        World world = WorldDimension.getBaseWorld(e.getEntity().getWorld());

        if (module.isPlayerWorld(world)) {
            String deathMessage = e.getDeathMessage();
            e.setDeathMessage(null);

            BukkitUtilities.broadcast(WorldDimension.getPlayersAllDimensions(world), deathMessage);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

}
