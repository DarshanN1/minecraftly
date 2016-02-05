package com.minecraftly.bukkit.modules.playerworlds.command;

import com.minecraftly.bukkit.language.LanguageManager;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.utilities.Utilities;
import com.minecraftly.bukkit.language.LanguageValue;
import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.minecraftly.bukkit.user.UserManager;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import net.ellune.exhaust.command.annotation.Sender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Keir on 05/07/2015.
 */
public class WorldsCommands implements Listener {

    private static final String KEY_RESET = "Minecraftly.Reset.Time";
    private static final int RESET_SECONDS = 5;

    private final ModulePlayerWorlds module;
    private final UserManager userManager;

    private final LanguageValue languageAreYouSure = new LanguageValue("&cAre you sure you wish to do that? All your worlds will be reset.\n&cUse &6/reset &cagain to reset.");
    private final LanguageValue languageWorldIsBeingReset = new LanguageValue("&cSorry, that world is currently being reset.");
    private final LanguageValue languageError = new LanguageValue("&cThere was an error whilst resetting your world(s).");

    private final LanguageValue languageKickedSender = new LanguageValue("&aPlayer &6%s &asuccessfully kicked from world.");
    private final LanguageValue languageKickedTarget = new LanguageValue("&cYou were kicked from that players world.");
    private final LanguageValue languageNotFound = new LanguageValue("&cPlayer not found in your world.");

    private final LanguageValue languageMutedSender = new LanguageValue("&aThe player &6%s &ahas been muted in your world.");
    private final LanguageValue languageMutedTarget = new LanguageValue("&cYou have been muted in the world of &6%s&c.");
    private final LanguageValue languageMutedAttemptChat = new LanguageValue("&cSorry, the owner of this world has muted you.");

    public WorldsCommands(ModulePlayerWorlds module, UserManager userManager, LanguageManager languageManager) {
        this.module = module;
        this.userManager = userManager;

        languageManager.registerAll(new HashMap<String, LanguageValue>(){{
            String resetPrefix = module.getLanguageSection() + ".reset";
            put(resetPrefix + ".areYouSure", languageAreYouSure);
            put(resetPrefix + ".worldIsBeingReset", languageWorldIsBeingReset);
            put(resetPrefix + ".error", languageError);

            String worldPrefix = module.getLanguageSection() + ".world";
            put(worldPrefix + ".error.notFound", languageNotFound);
            put(worldPrefix + ".kicked.sender", languageKickedSender);
            put(worldPrefix + ".kicked.target", languageKickedTarget);
            put(worldPrefix + ".mute.sender", languageMutedSender);
            put(worldPrefix + ".mute.target", languageMutedTarget);
        }});
    }

    @Command(aliases = "kick", desc = "Kicks a player from your world.", min = 1, max = 1)
    @Require("minecraftly.world.kick")
    public void kickPlayer(@Sender Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();

        if (!sender.equals(target)
                && module.isWorldLoaded(senderUUID)
                && WorldDimension.getBaseWorld(target.getWorld()).equals(module.getPlayerWorld(senderUUID))) {
            languageKickedSender.send(sender, target.getDisplayName());
            languageKickedTarget.send(target);
            target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            languageNotFound.send(sender);
        }
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

            World world = module.getPlayerWorld(player);
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
                module.joinWorld(player);
            } else {
                languageError.send(player);
            }
        }
    }

    @Command(aliases = "mute", desc = "Mutes a player in your world.", min = 1, max = 1)
    @Require("minecraftly.world.mute")
    public void mutePlayer(@Sender Player sender, Player target) {
        UUID targetWorldOwner = module.getWorldOwner(WorldDimension.getBaseWorld(target.getWorld()));

        if (targetWorldOwner != null && sender.getUniqueId().equals(targetWorldOwner)) {
            WorldUserData worldUserData = userManager.getUser(sender).getSingletonUserData(WorldUserDataContainer.class).getOrLoad(target.getUniqueId());
            worldUserData.setMuted(true);

            languageMutedTarget.send(target, sender.getDisplayName());
            languageMutedSender.send(sender, target.getDisplayName());
        } else {
            languageNotFound.send(sender);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleMute(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID worldOwnerUUID = module.getWorldOwner(WorldDimension.getBaseWorld(player.getWorld()));

        if (worldOwnerUUID != null) {
            WorldUserData worldUserData = userManager.getUser(worldOwnerUUID).getSingletonUserData(WorldUserDataContainer.class).getOrLoad(player.getUniqueId());

            if (worldUserData.isMuted()) {
                languageMutedAttemptChat.send(player);
                e.setCancelled(true);
            }
        }
    }

}
