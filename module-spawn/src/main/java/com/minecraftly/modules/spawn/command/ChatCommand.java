package com.minecraftly.modules.spawn.command;

import com.minecraftly.modules.spawn.SpawnModule;
import com.sk89q.intake.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * This command simply kicks the sender from the server, however HubMagic on the Proxy side should interpret this and connect the player to another server.
 */
public class ChatCommand {

    private SpawnModule module;

    public ChatCommand(SpawnModule module) {
        this.module = module;
    }

    @Command(aliases = "chat", desc = "Switch to chat mode.", min = 0, max = 0)
    public void switchToChatMode(Player player) {
        player.teleport(module.getChatWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

}
