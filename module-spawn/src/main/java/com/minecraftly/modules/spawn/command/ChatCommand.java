package com.minecraftly.modules.spawn.command;

import com.sk89q.intake.Command;
import org.bukkit.entity.Player;

/**
 * This command simply kicks the sender from the server, however HubMagic on the Proxy side should interpret this and connect the player to another server.
 */
public class ChatCommand {

    @Command(aliases = "chat", desc = "Switch to chat mode.", min = 0, max = 0)
    public void kick(Player player) {
        player.kickPlayer(null);
    }

}
