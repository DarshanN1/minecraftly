package com.minecraftly.core.bukkit.modules.playerworlds;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.packets.PacketBotCheck;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Random;

/**
 * Created by Keir on 24/06/2015.
 */
public class HumanCheck implements Listener, Runnable {

    public static final String KEY_HUMAN_CHECK_INVENTORY = "Minecraftly.HumanCheckInventory";

    public static final String INVENTORY_NAME = "Are you a bot?";

    public static final ItemStack ACCEPT_ITEM_STACK = new ItemStack(Material.WOOL, 1, (short) 5) {{
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "I confirm I am not a bot.");
        setItemMeta(itemMeta);
    }};

    private ModulePlayerWorlds module;
    private Random random = new Random();

    public HumanCheck(ModulePlayerWorlds module) {
        this.module = module;
    }

    public void showHumanCheck(Player player) {
        Inventory inventory;

        if (!player.hasMetadata(KEY_HUMAN_CHECK_INVENTORY)) {
            int inventorySize = 9 * 6;
            inventory = Bukkit.createInventory(player, inventorySize, INVENTORY_NAME);
            inventory.setItem(random.nextInt(inventorySize), ACCEPT_ITEM_STACK);
            player.setMetadata(KEY_HUMAN_CHECK_INVENTORY, new FixedMetadataValue(module.getPlugin(), inventory));
        } else {
            inventory = (Inventory) player.getMetadata(KEY_HUMAN_CHECK_INVENTORY).get(0).value();
        }

        player.openInventory(inventory);
    }

    public void deleteInventoryCache(Player player) {
        player.removeMetadata(KEY_HUMAN_CHECK_INVENTORY, module.getPlugin());
    }

    @PacketHandler
    public void onPacketBotCheck(Player player, PacketBotCheck packet) {
        if (packet.getStage() != 0) {
            throw new UnsupportedOperationException("Expecting stage 0.");
        }

        // make player do bot check
        module.getPlugin().getUserManager().getUser(player).getSingletonUserData(HumanCheckStatusData.class).setStatus(false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity whoClicked = e.getWhoClicked();

        if (whoClicked instanceof Player) {
            Player player = (Player) whoClicked;
            Inventory inventory = e.getInventory();
            ItemStack currentItem = e.getCurrentItem();

            if (inventory.getName().equals(INVENTORY_NAME)) {
                if (currentItem != null && currentItem.getType() == ACCEPT_ITEM_STACK.getType()) { // todo make this check better
                    module.getPlugin().getUserManager().getUser(player).getSingletonUserData(HumanCheckStatusData.class).setStatus(true);
                    module.getPlugin().getGateway().sendPacket(player, new PacketBotCheck(true));
                    deleteInventoryCache(player);
                    player.closeInventory();
                }

                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        deleteInventoryCache(e.getPlayer());
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            if (!module.getPlugin().getUserManager().getUser(player).getSingletonUserData(HumanCheckStatusData.class).getStatus()) {
                showHumanCheck(player);
            }
        }
    }
}
