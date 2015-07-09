package com.minecraftly.core.bukkit.modules.playerworlds;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import org.apache.commons.lang.WordUtils;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Keir on 03/06/2015.
 */
public class OwnerCommands {

    private final ModulePlayerWorlds module;

    private final LanguageValue langNotOwner = new LanguageValue("&cYou cannot do that here, this is not your world.");
    private final LanguageValue langNotInWorld = new LanguageValue("&cThat player is not in your world.");
    private final LanguageValue langAttemptSelf = new LanguageValue("&cYou may not set your own game mode.");
    private final LanguageValue langAlreadyInGameMode = new LanguageValue("&cThat player is already in &6%s &cmode.");
    private final LanguageValue langSuccessSender = new LanguageValue("&aSuccessfully set &6%s &ato &6%s &amode.");
    private final LanguageValue langSuccessTarget = new LanguageValue("&aYou have been set to &6%s &amode by &6%s&a.");

    public OwnerCommands(ModulePlayerWorlds module) {
        this.module = module;

        module.getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>() {{
            String langPrefix = module.getLanguageSection() + ".command.owner";

            put(langPrefix + ".notOwner", langNotOwner);
            put(langPrefix + ".notInWorld", langNotInWorld);
            put(langPrefix + ".attemptSelf", langAttemptSelf);
            put(langPrefix + ".alreadyInGameMode", langAlreadyInGameMode);
            put(langPrefix + ".success.sender", langSuccessSender);
            put(langPrefix + ".success.target", langSuccessTarget);
        }});
    }

    @Command(aliases = {"survival"}, desc = "Puts a player in survival mode.", usage = "<target>", min = 1, max = 1)
    public void setGuestSurvival(@Sender Player player, Player target) {
        setGuestGameMode(player, target, GameMode.SURVIVAL);
    }

    @Command(aliases = {"adventure"}, desc = "Puts a player in adventure mode.", usage = "<target>", min = 1, max = 1)
    public void setGuestAdventure(@Sender Player player, Player target) {
        setGuestGameMode(player, target, GameMode.ADVENTURE);
    }

    public void setGuestGameMode(Player sender, Player target, GameMode gameMode) {
        World world = WorldDimension.getBaseWorld(sender.getWorld());

        if (module.isPlayerWorld(world)) {
            UUID worldOwner = module.getWorldOwner(world);

            if (sender.getUniqueId().equals(worldOwner)) {
                if (sender != target) {
                    if (WorldDimension.getBaseWorld(target.getWorld()) == world) {
                        String gameModeCamelCase = WordUtils.capitalizeFully(gameMode.name().toLowerCase()).replace(" ", "");

                        if (target.getGameMode() != gameMode) {
                            target.setGameMode(gameMode);

                            langSuccessSender.send(sender, target.getDisplayName(), gameModeCamelCase);
                            langSuccessTarget.send(target, gameModeCamelCase, sender.getDisplayName());
                        } else {
                            langAlreadyInGameMode.send(sender, gameModeCamelCase);
                        }
                    } else {
                        langNotInWorld.send(sender);
                    }
                } else {
                    langAttemptSelf.send(sender);
                }
            } else {
                langNotOwner.send(sender);
            }
        } else {
            langNotOwner.send(sender);
        }
    }

}
