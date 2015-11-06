package com.minecraftly.core.bukkit.modules.playerworlds.command;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserData;
import com.minecraftly.core.bukkit.modules.playerworlds.data.world.WorldUserDataContainer;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
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
    private final LanguageValue langSuccessSender = new LanguageValue("&aSuccessfully set trust mode of &6%s &ato &6%s&a.");
    private final LanguageValue langSuccessTarget = new LanguageValue("&aYou are now &6%s &ain &6%s&a's world.");

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

    @Command(aliases = {"trust"}, desc = "Trusts a player.", usage = "<target>", min = 1, max = 1)
    public void setTrusted(@Sender Player sender, Player target) {
        updatePlayerTrustStatus(sender, target, true);
    }

    @Command(aliases = {"untrust"}, desc = "Un-trusts a player.", usage = "<target>", min = 1, max = 1)
    public void setUntrusted(@Sender Player sender, Player target) {
        updatePlayerTrustStatus(sender, target, false);
    }

    public void updatePlayerTrustStatus(Player sender, Player target, boolean trusted) {
        World world = WorldDimension.getBaseWorld(sender.getWorld());

        if (module.isPlayerWorld(world)) {
            UUID senderUUID = sender.getUniqueId();
            UUID targetUUID = target.getUniqueId();
            UUID worldOwnerUUID = module.getWorldOwner(world);

            if (senderUUID.equals(worldOwnerUUID)) {
                if (sender != target) {
                    if (WorldDimension.getBaseWorld(target.getWorld()) == world) {
                        WorldUserData worldUserData = module.getPlugin().getUserManager()
                                .getUser(worldOwnerUUID)
                                .getSingletonUserData(WorldUserDataContainer.class)
                                .getOrLoad(targetUUID);

                        worldUserData.setTrusted(trusted); // this method also updates gamemode

                        String trustString = trusted ? "trusted" : "un-trusted";
                        langSuccessSender.send(sender, target.getDisplayName(), trustString);
                        langSuccessTarget.send(target, trustString, sender.getDisplayName());
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
