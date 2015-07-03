package com.minecraftly.core.bukkit.modules.spawn;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ModuleSpawn extends Module implements Listener {

    private World chatWorld = null;

    private LanguageValue languageNobodyCanHearYou = new LanguageValue("&cNobody can hear you.");

    public ModuleSpawn(MclyCoreBukkitPlugin plugin) {
        super("SpawnManager", plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().getLanguageManager().registerAll(new HashMap<String, LanguageValue>() {{
            put(getLanguageSection() + ".chat.nobodyCanHearYou", languageNobodyCanHearYou);
        }});

        registerListener(this);
    }

    /*@Override
    public ChunkGenerator getWorldGenerator(String worldName, String id) {
        Validate.notEmpty(id, "Generator settings missing.");

        String[] parts = id.split(",");
        String materialName = parts.length > 0 ? parts[0] : id;
        Material material = Material.matchMaterial(materialName);
        Validate.notNull(material, "Invalid material type: " + materialName);
        Validate.isTrue(material.isBlock(), material + " is not a block (likely an item).");
        Validate.isTrue(material.isSolid(), material + " is not a solid block (players will fall through this).");

        return new OldChatWorldGenerator(material);
    }*/

    public World getChatWorld() {
        if (chatWorld == null) {
            chatWorld = Bukkit.getWorlds().get(0);
        }

        return chatWorld;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.teleport(getChatWorld().getSpawnLocation());
        onEnterChatWorld(player, player.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        World to = e.getTo().getWorld();
        World from = e.getFrom().getWorld();

        World spawnWorld = getChatWorld();

        if (!to.equals(from)) {
            if (to.equals(spawnWorld)) {
                onEnterChatWorld(player, spawnWorld);
            } else if (from.equals(spawnWorld)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.removePotionEffect(PotionEffectType.JUMP);
                player.setWalkSpeed(0.2F);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Set<Player> recipients = e.getRecipients();
        Player player = e.getPlayer();
        World spawnWorld = Bukkit.getWorlds().get(0);

        if (player.getWorld() == spawnWorld) {
            recipients.clear();
            languageNobodyCanHearYou.send(player);
        } else {
            Iterator<Player> iterator = recipients.iterator();

            while (iterator.hasNext()) {
                Player recipient = iterator.next();
                if (recipient.getWorld() == spawnWorld) {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent e) {
        if (e.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) e.getEntity();
            World world = player.getWorld();

            if (world == getChatWorld()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();

        if (world == getChatWorld()) {
            onEnterChatWorld(player, world);
        }
    }

    private void onEnterChatWorld(final Player player, final World world) { // lol
        for (Player player1 : Bukkit.getOnlinePlayers()) {
            player.hidePlayer(player1);
            player1.hidePlayer(player);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 200, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 200, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250, true, false));
        player.setWalkSpeed(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setTotalExperience(0);
        player.setGameMode(GameMode.ADVENTURE);

        Bukkit.getScheduler().runTaskLater(getPlugin(), new Runnable() {
            @Override
            public void run() {
                if (player.getWorld() == world) { // check player hasn't changed world
                    player.getInventory().clear();
                    player.getEnderChest().clear();
                }
            }
        }, 1L);
    }

}
