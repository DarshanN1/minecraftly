package com.minecraftly.core.bukkit.modules.fun;

import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

/**
 * Created by Keir on 14/07/2015.
 */
public class CommandHat {

    private final LanguageValue langNoItemInHand = new LanguageValue("&cYou must be holding an item in your hand.");
    private final LanguageValue langSetHat = new LanguageValue("&aSet your hat to &6%s&a.");

    public CommandHat(ModuleFun module, LanguageManager languageManager) {
        languageManager.registerAll(new HashMap<String, LanguageValue>() {{
            String prefix = module.getLanguageSection() + ".command.hat";
            put(prefix + ".noItemInHand", langNoItemInHand);
            put(prefix + ".setHat", langSetHat);
        }});
    }

    @Command(aliases = "hat", desc = "Sets a block as your hat.", min = 0, max = 0)
    public void setHat(@Sender Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack inHand = inventory.getItemInHand();

        if (inHand != null && inHand.getAmount() > 0) {
            // todo only allow solid blocks?
            ItemStack helmet = inHand.clone();
            helmet.setAmount(1);
            inventory.setHelmet(helmet);

            inHand.setAmount(inHand.getAmount() - 1);
            inventory.setItemInHand(inHand.getAmount() > 0 ? inHand : null);

            langSetHat.send(player, BukkitUtilities.getFriendlyName(helmet.getType()));
        } else {
            langNoItemInHand.send(player);
        }
    }

}
