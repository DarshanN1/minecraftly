package com.minecraftly.bukkit.modules.playerworlds.command;

import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.language.LanguageValue;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import net.ellune.exhaust.command.annotation.Sender;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 01/11/2015.
 */
public class BackCommand implements Listener {

    private final ModulePlayerWorlds module;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    private final LanguageValue langNoBackLocation = new LanguageValue("&cYou have nowhere to go back to.");

    public BackCommand(ModulePlayerWorlds module) {
        this.module = module;

        module.getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>(){{
            String prefix = module.getLanguageSection() + ".command.back";
            put(prefix + ".noBackLocations", langNoBackLocation);
        }});
    }

    @Command(aliases = "back", desc = "Go to your last location.", max = 0)
    @Require("essentials.back")
    public void teleportLastLocation(@Sender Player player) {
        Location lastLocation = lastLocations.get(player.getUniqueId());

        if (lastLocation != null) {
            World lastWorld = lastLocation.getWorld();
            World currentWorld = player.getWorld();

            if (WorldDimension.getBaseWorld(currentWorld) == WorldDimension.getBaseWorld(lastWorld)) {
                player.teleport(lastLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
            } else {
                langNoBackLocation.send(player);
            }
        } else {
            langNoBackLocation.send(player);
        }
    }

    private void recordLastLocation(Player player) {
        World baseWorld = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isPlayerWorld(baseWorld)) {
            lastLocations.put(player.getUniqueId(), player.getLocation());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        recordLastLocation(e.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        recordLastLocation(e.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        lastLocations.remove(e.getPlayer().getUniqueId());
    }
}
