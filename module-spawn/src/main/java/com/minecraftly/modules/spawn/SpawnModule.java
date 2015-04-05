package com.minecraftly.modules.spawn;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.packets.spawn.PacketSpawn;
import com.sk89q.intake.Command;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.entity.Player;

import java.io.IOException;

/**
 * Created by Keir on 05/04/2015.
 */
public class SpawnModule extends Module {

    @Override
    protected void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(this);
    }

    @Command(aliases = "spawn", desc = "Teleports the player to the main spawn location", max = 0)
    public void spawnCommand(MinecraftlyCore minecraftlyCore, Player player) {
        try {
            minecraftlyCore.getGateway().sendPacket(player, new PacketSpawn());
        } catch (IOException e) { // todo
            e.printStackTrace();
        }
    }
}
