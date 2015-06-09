package com.minecraftly.modules.homeworlds.command;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.modules.homeworlds.HomeWorldsModule;
import com.minecraftly.modules.homeworlds.WorldDimension;
import com.sk89q.intake.Command;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Keir on 03/06/2015.
 */
public class OwnerCommands {

    private final HomeWorldsModule module;

    private final LanguageValue langNotOwner;
    private final LanguageValue langNotInHome;
    private final LanguageValue langAttemptSelf;
    private final LanguageValue langAlreadyInGameMode;
    private final LanguageValue langSuccessSender;
    private final LanguageValue langSuccessTarget;

    public OwnerCommands(HomeWorldsModule module) {
        this.module = module;
        this.langNotOwner = new LanguageValue(module, "&cYou cannot do that here, this is not your home.");
        this.langNotInHome = new LanguageValue(module, "&cThat player is not in your home.");
        this.langAttemptSelf = new LanguageValue(module, "&cYou may not set your own game mode.");
        this.langAlreadyInGameMode = new LanguageValue(module, "&cThat player is already in &6%s &cmode.");
        this.langSuccessSender = new LanguageValue(module, "&aSuccessfully set &6%s &ato &6%s &amode.");
        this.langSuccessTarget = new LanguageValue(module, "&aYou have been set to &6%s &amode by &6%s&a.");

        String langPrefix = module.getLanguageSection() + ".command.owner";

        module.getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>() {{
            put(langPrefix + ".notOwner", langNotOwner);
            put(langPrefix + ".notInHome", langNotInHome);
            put(langPrefix + ".attemptSelf", langAttemptSelf);
            put(langPrefix + ".alreadyInGameMode", langAlreadyInGameMode);
            put(langPrefix + ".success.sender", langSuccessSender);
            put(langPrefix + ".success.target", langSuccessTarget);
        }});
    }

    @Command(aliases = {"survival"}, desc = "Puts a player in survival mode.", usage = "<target>", min = 1, max = 1)
    public void setGuestSurvival(Player player, String targetName) {
        setGuestGameMode(player, targetName, GameMode.SURVIVAL);
    }

    @Command(aliases = {"adventure"}, desc = "Puts a player in adventure mode.", usage = "<target>", min = 1, max = 1)
    public void setGuestAdventure(Player player, String targetName) {
        setGuestGameMode(player, targetName, GameMode.ADVENTURE);
    }

    public void setGuestGameMode(Player sender, String targetName, GameMode gameMode) {
        World world = WorldDimension.getBaseWorld(sender.getWorld());

        if (module.isHomeWorld(world)) {
            UUID worldOwner = module.getHomeOwner(world);

            if (sender.getUniqueId().equals(worldOwner)) {
                Player target = Bukkit.getPlayer(targetName);

                if (target != null) {
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
                            langNotInHome.send(sender);
                        }
                    } else {
                        langAttemptSelf.send(sender);
                    }
                } else {
                    langNotInHome.send(sender);
                }
            } else {
                langNotOwner.send(sender);
            }
        } else {
            langNotOwner.send(sender);
        }
    }

}
