package com.minecraftly.core.bukkit.modules.spawn;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ModuleSpawn extends Module implements Listener {

    private World chatWorld = null;

    public ModuleSpawn(MclyCoreBukkitPlugin plugin) {
        super("SpawnManager", plugin);
    }

    @Override
    public void onEnable() {
        registerListener(this);
    }

    /*
    Old generator code

    @Override
    public ChunkGenerator getWorldGenerator(String worldName, String id) {
        Validate.notEmpty(id, "Generator settings missing.");

        String[] parts = id.split(",");
        String materialName = parts.length > 0 ? parts[0] : id;
        Material material = Material.matchMaterial(materialName);
        Validate.notNull(material, "Invalid material type: " + materialName);
        Validate.isTrue(material.isBlock(), material + " is not a block (likely an item).");
        Validate.isTrue(material.isSolid(), material + " is not a solid block (players will fall through this).");

        return new OldSpawnWorldGenerator(material);
    }
    */

    public World getChatWorld() {
        if (chatWorld == null) {
            chatWorld = Bukkit.getWorlds().get(0);
        }

        return chatWorld;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.teleport(getChatWorld().getSpawnLocation());
    }
}
