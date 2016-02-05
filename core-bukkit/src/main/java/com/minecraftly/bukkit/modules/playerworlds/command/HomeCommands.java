package com.minecraftly.bukkit.modules.playerworlds.command;

import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.language.LanguageValue;
import com.minecraftly.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.minecraftly.bukkit.user.User;
import com.sk89q.intake.Command;
import net.ellune.exhaust.command.annotation.Sender;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/**
 * Handles all world home related commands.
 * @author iKeirNez
 */
public class HomeCommands {

    private ModulePlayerWorlds module;

    private final LanguageValue langHomeSet = new LanguageValue("&aSet home location.");

    public HomeCommands(ModulePlayerWorlds module) {
        this.module = module;

        String section = module.getLanguageSection() + ".command.home";
        module.getPlugin().getLanguageManager().register(section + ".homeSet", langHomeSet);
    }

    @Command(aliases = "home", desc = "Teleport to your home.", max = 0)
    public void gotoHome(@Sender Player player, @Sender User user) {
        World baseWorld = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isPlayerWorld(baseWorld)) {
            UUID worldUUID = module.getWorldOwner(baseWorld);
            WorldUserData worldUserData = user.getSingletonUserData(WorldUserDataContainer.class).get(worldUUID);
            Location homeLocation = worldUserData.getHomeLocation();

            if (homeLocation != null) {
                player.teleport(homeLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
            } else {
                player.performCommand("/spawn");
            }
        } else {
            module.langCannotUseCommandHere.send(player);
        }
    }

    @Command(aliases = "sethome", desc = "Set your home location.", max = 0)
    public void setHome(@Sender Player player, @Sender User user) {
        World baseWorld = WorldDimension.getBaseWorld(player.getWorld());

        if (module.isPlayerWorld(baseWorld)) {
            UUID worldUUID = module.getWorldOwner(baseWorld);
            WorldUserData worldUserData = user.getSingletonUserData(WorldUserDataContainer.class).get(worldUUID);
            worldUserData.setHomeLocation(player.getLocation());
            langHomeSet.send(player);
        } else {
            module.langCannotUseCommandHere.send(player);
        }
    }

}
