package com.minecraftly.core.bukkit.modules.chest;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.chest.data.UserChestData;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.sk89q.intake.Command;
import com.sk89q.intake.parametric.annotation.Range;
import lc.vq.exhaust.command.annotation.Sender;
import org.bukkit.entity.Player;

/**
 * Created by Keir on 05/07/2015.
 */
public class ChestCommand {

    private ModuleChest moduleChest;
    private UserManager userManager;

    private LanguageValue languageExceedsMaximumChests = new LanguageValue("&cYou may only have a maximum of &6%s &cchests.");

    public ChestCommand(ModuleChest moduleChest, UserManager userManager) {
        this.moduleChest = moduleChest;
        this.userManager = userManager;
    }

    @Command(aliases = "chest", desc = "Opens a virtual chest.", usage = "<chest-number>", min = 1, max = 1)
    public void openChest(@Sender Player player, @Range(min = 1) int chestNumber) {
        int maximumChests = moduleChest.getMaximumChests().getValue();

        if (chestNumber > maximumChests) {
            languageExceedsMaximumChests.send(player, maximumChests);
        }

        User user = userManager.getUser(player);
        player.openInventory(user.getSingletonUserData(UserChestData.class).getInventory(chestNumber - 1, true));
    }

}
