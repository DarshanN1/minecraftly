package com.minecraftly.core.bukkit.modules.playerworlds.command;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import lc.vq.exhaust.command.annotation.Sender;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;

/**
 * Created by Keir on 30/10/2015.
 */
public class SpawnCommands {

    private ModulePlayerWorlds module;

    private final LanguageValue langSetWorldSpawn = new LanguageValue("&aSet this worlds spawn.");
    private final LanguageValue langCannotSetSpawn = new LanguageValue("&cYou cannot set this worlds spawn.");

    public SpawnCommands(ModulePlayerWorlds module) {
        this.module = module;

        module.getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>(){{
            String prefix = module.getLanguageSection() + ".command.spawn.set";
            put(prefix + ".success", langSetWorldSpawn);
            put(prefix + ".error", langCannotSetSpawn);
        }});
    }

    @Command(aliases = "setspawn", desc = "Sets the spawn point for the world.", min = 0, max = 0)
    @Require("minecraftly.setspawn")
    public void setSpawn(@Sender Player player) {
        World world = player.getWorld();
        World baseWorld = WorldDimension.getBaseWorld(world);

        if (module.getWorldOwner(baseWorld).equals(player.getUniqueId())) {
            if (world == baseWorld) {
                Location location = player.getLocation();
                world.setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                langSetWorldSpawn.send(player);
            } else {
                langCannotSetSpawn.send(player);
            }
        } else {
            module.langNotOwner.send(player);
        }
    }

    @Command(aliases = "spawn", desc = "Goto spawn.")
    public void gotoSpawn(@Sender Player player) {
        World baseWorld = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isPlayerWorld(baseWorld)) {
            player.teleport(baseWorld.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        } else {
            module.langCannotUseCommandHere.send(player);
        }
    }

}
