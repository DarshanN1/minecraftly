package com.minecraftly.core.bukkit.commands;

import com.minecraftly.core.bukkit.user.User;
import com.sk89q.intake.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by Keir on 13/03/2015.
 */
public class MinecraftlyCommand {

    @Command(aliases = {"minecraftly", "mcly"}, desc = "Displays information about the Minecraftly plugin", min = 0, max = 0)
    public void about(Player player, User user) {
        player.sendMessage(ChatColor.AQUA + "This command is WIP.");
    }

}
