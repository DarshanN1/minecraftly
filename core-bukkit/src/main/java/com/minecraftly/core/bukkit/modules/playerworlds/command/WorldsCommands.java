package com.minecraftly.core.bukkit.modules.playerworlds.command;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Keir on 05/07/2015.
 */
public class WorldsCommands {

    private static final String KEY_RESET = "Minecraftly.Reset.Time";
    private static final int RESET_SECONDS = 5;

    private ModulePlayerWorlds module;

    private LanguageValue languageAreYouSure = new LanguageValue("&cAre you sure you wish to do that? All your worlds will be reset.\n&cUse &6/reset &cagain to reset.");
    private LanguageValue languageWorldIsBeingReset = new LanguageValue("&cSorry, that world is currently being reset.");
    private LanguageValue languageError = new LanguageValue("&cThere was an error whilst resetting your worlds.");

    public WorldsCommands(ModulePlayerWorlds module, LanguageManager languageManager) {
        this.module = module;
        languageManager.registerAll(new HashMap<String, LanguageValue>(){{
            String prefix = module.getLanguageSection() + ".reset";
            put(prefix + ".areYouSure", languageAreYouSure);
            put(prefix + ".worldIsBeingReset", languageWorldIsBeingReset);
            put(prefix + ".error", languageError);
        }});
    }

    @Command(aliases = "reset", desc = "Resets all of your worlds.", min = 0, max = 0)
    public void resetWorlds(@Sender Player player) {
        long lastResetCommand = 0;

        if (player.hasMetadata(KEY_RESET)) {
            lastResetCommand = player.getMetadata(KEY_RESET).get(0).asLong();
        }

        if (lastResetCommand == 0 || lastResetCommand + TimeUnit.SECONDS.toMillis(RESET_SECONDS) < System.currentTimeMillis()) {
            languageAreYouSure.send(player);
            player.setMetadata(KEY_RESET, new FixedMetadataValue(module.getPlugin(), System.currentTimeMillis()));
        } else { // reset
            player.removeMetadata(KEY_RESET, module.getPlugin());

            World world = module.getWorld(player);
            Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            for (Player p : WorldDimension.getPlayersAllDimensions(world)) {
                languageWorldIsBeingReset.send(p);
                p.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }

            List<File> worldDirectories = new ArrayList<File>(){{
                add(world.getWorldFolder());
            }};

            for (WorldDimension worldDimension : WorldDimension.values()) {
                String worldName = worldDimension.convertTo(world.getName());
                File directory = new File(Bukkit.getWorldContainer(), worldName);

                if (directory.exists() && directory.isDirectory()) {
                    worldDirectories.add(directory);
                }

                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    Bukkit.unloadWorld(w, false);
                }
            }

            Bukkit.unloadWorld(world, false);

            boolean error = false;
            for (File worldDirectory : worldDirectories) {
                if (!Utilities.deleteDirectory(worldDirectory)) {
                    module.getLogger().severe("Error deleting world directory: " + worldDirectory.getPath() + ".");
                    error = true;
                }
            }

            if (!error) {
                module.getPlugin().getUserManager().getUser(player).getSingletonUserData(WorldUserDataContainer.class).getOrLoad(player.getUniqueId()).reset();
                module.joinWorld(player, player);
            } else {
                languageError.send(player);
            }
        }
    }

}
