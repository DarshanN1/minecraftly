package com.minecraftly.modules.world;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;

/**
 * Created by Keir on 18/03/2015.
 */
public class WorldCommands {

    private WorldModule worldModule;

    public WorldCommands(WorldModule worldModule) {
        this.worldModule = worldModule;
    }

    // todo move to language file

    @Command(aliases = "load", desc = "Loads a world and configures it to be loaded at startup", usage = "<world>", min = 1, max = 1)
    @Require("minecraftly.world.load")
    public void loadWorld(CommandSender sender, String worldName) {
        String realWorldName; // real world name, including correct capitalisation
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            realWorldName = world.getName();
            sender.sendMessage(ChatColor.AQUA + "That world is already loaded, so we'll just configure it to be auto-loaded on each startup.");
            worldModule.addStartupLoadTask(realWorldName);
        } else {
            File worldFile = new File(worldName);

            if (worldFile.exists()) {
                realWorldName = worldFile.getName();
                sender.sendMessage(ChatColor.AQUA + "Loading world " + ChatColor.GOLD + worldName + ChatColor.AQUA + " (this may cause some short-term lag)");

                world = worldModule.loadWorld(realWorldName);

                if (world != null) {
                    worldModule.addStartupLoadTask(world.getName());
                    sender.sendMessage(ChatColor.AQUA + "Loaded world " + ChatColor.GOLD + worldName);
                } else {
                    sender.sendMessage(ChatColor.RED + "There was an error loading this world (try checking the console).");
                }
            } else {
                sender.sendMessage(ChatColor.AQUA + "Couldn't find world named " + ChatColor.GOLD + worldName + ChatColor.AQUA + ".");
            }
        }
    }

    @Command(aliases = "unload", desc = "Unloads a world and prevent the plugin from auto-loading the world at startup", usage = "<world>", min = 1, max = 1)
    @Require("minecraftly.world.unload")
    public void unloadWorld(CommandSender sender, World world) {
        String worldName = world.getName();
        sender.sendMessage(ChatColor.AQUA + "Teleporting players out of world, then save and unload world " + ChatColor.GOLD + worldName + ChatColor.AQUA + " (this may cause short-term lag)");

        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.sendMessage(ChatColor.AQUA + "You were moved to spawn as the world you were in is being unloaded.");
        }

        worldModule.removeStartupLoadTask(world);

        if (Bukkit.unloadWorld(world, true)) {
            sender.sendMessage(ChatColor.AQUA + "World " + ChatColor.GOLD + worldName + ChatColor.AQUA + " successfully saved and unloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to unload world, no further detail was given.");
        }
    }

    @Command(aliases = {"teleport", "tp"}, desc = "Teleports the sender to a loaded world.", usage = "<world>", min = 1, max = 1)
    @Require("minecraftly.world.teleport")
    public void teleportWorld(Player player, World world) {
        player.sendMessage(ChatColor.AQUA + "Teleporting to world: " + ChatColor.GOLD + world.getName());
        player.teleport(world.getSpawnLocation());
    }

}
