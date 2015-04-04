package com.minecraftly.testmodule;

import com.sk89q.intake.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Created by Keir on 15/03/2015.
 */
public class TestModuleCommand {

    @Command(aliases = {"tmcommand", "testmodulecommand", "testmodule"}, desc = "This is a command added by a Minecraftly module", max = 0)
    public void testModuleCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Awesome, it works :)");
    }

}
