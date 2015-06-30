package com.minecraftly.modules.homeworlds.bot;

import com.minecraftly.core.packets.PacketBotCheck;
import com.minecraftly.modules.homeworlds.HomeWorldsModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

/**
 * Created by Keir on 24/06/2015.
 */
public class BotCheck implements Listener {

    public static final String INVENTORY_NAME = "Are you a bot?";

    public static final ItemStack ACCEPT_ITEM_STACK = new ItemStack(Material.WOOL, 1, (short) 5) {{
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "I confirm I am not a bot.");
        setItemMeta(itemMeta);
    }};

    public static final Material[] RANDOM_MATERIALS = { Material.STONE, Material.DIRT, Material.GRASS };

    private HomeWorldsModule module;
    private Random random = new Random();

    public BotCheck(HomeWorldsModule module) {
        this.module = module;
    }

    public void checkBot(Player player) {
        int inventorySize = 9 * 6;
        Inventory inventory = Bukkit.createInventory(player, inventorySize, INVENTORY_NAME);
        inventory.setItem(random.nextInt(inventorySize), ACCEPT_ITEM_STACK);

        for (int i = 0; i < random.nextInt(5) + 5; i++) {
            int slot;

            do {
                slot = random.nextInt(inventorySize);
            } while (inventory.getItem(slot) != null);

            inventory.setItem(slot, new ItemStack(RANDOM_MATERIALS[random.nextInt(RANDOM_MATERIALS.length)], 1));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity whoClicked = e.getWhoClicked();

        if (whoClicked instanceof Player) {
            Player player = (Player) whoClicked;
            Inventory inventory = e.getInventory();
            ItemStack currentItem = e.getCurrentItem();

            if (inventory.getName().equals(INVENTORY_NAME)) {
                if (currentItem != null && currentItem.getType() == ACCEPT_ITEM_STACK.getType()) { // todo make this check better
                    player.closeInventory();
                    module.getPlugin().getUserManager().getUser(player).getSingletonUserData(BotCheckStatusData.class).setStatus(true);
                    module.getPlugin().getGateway().sendPacket(player, new PacketBotCheck(true));
                }

                e.setCancelled(true);
            }
        }
    }

}
