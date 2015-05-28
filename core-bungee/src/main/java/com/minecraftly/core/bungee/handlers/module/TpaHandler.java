package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.bungee.utilities.BungeeUtilities;
import com.minecraftly.core.packets.PacketTeleport;
import com.sk89q.intake.Command;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Provides TPA functionality.
 */
public class TpaHandler implements Runnable {

    private MclyCoreBungeePlugin plugin;
    private ProxyGateway<ProxiedPlayer, ServerInfo> gateway;
    private int EXPIRE_SECONDS = 60;

    public TpaHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();
    }

    // sender, target, time in millis started
    private Map<Map.Entry<UUID, UUID>, Long> tpaRequests = new HashMap<>();

    @Command(aliases = "tpa", desc = "Request to teleport to a player.", usage = "<player>", min = 1, max = 1)
    public void newTpaRequest(ProxiedPlayer sender, String targetName) {
        ProxiedPlayer target = BungeeUtilities.easyMatchPlayer(targetName);
        if (target == null) {
            sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                            .append(targetName).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.RED)
                            .create()
            );
        } else if (sender == target) {
            sender.sendMessage(new ComponentBuilder("You may not teleport to yourself.").color(ChatColor.RED).create());
        } else {
            tpaRequests.put(new AbstractMap.SimpleImmutableEntry<>(sender.getUniqueId(), target.getUniqueId()), System.currentTimeMillis());

            target.sendMessage(new ComponentBuilder(sender.getDisplayName()).color(ChatColor.GOLD)
                            .append(" has sent you a teleport request.\nUse ").color(ChatColor.AQUA)
                            .append("/tpaccept " + sender.getName()).color(ChatColor.GOLD)
                            .append(" to accept.").color(ChatColor.AQUA)
                            .create()
            );

            sender.sendMessage(new ComponentBuilder("Request successfully sent to ").color(ChatColor.GREEN)
                            .append(target.getDisplayName()).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.GREEN)
                            .create()
            );
        }
    }

    @Command(aliases = "tpaccept", desc = "Accept a teleport request from a player.", usage = "<player>", min = 1, max = 1)
    public void acceptTpaRequest(ProxiedPlayer sender, String targetName) {
        final ProxiedPlayer target = BungeeUtilities.easyMatchPlayer(targetName);
        if (target == null) {
            sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                    .append(targetName).color(ChatColor.GOLD)
                    .append(".").color(ChatColor.RED)
                    .create()
            );
        } else {
            final UUID senderUUID = sender.getUniqueId();
            Map.Entry<UUID, UUID> searchQuery = new AbstractMap.SimpleImmutableEntry<>(target.getUniqueId(), senderUUID);
            Long timeCreated = tpaRequests.get(searchQuery);

            if (timeCreated == null || System.currentTimeMillis() > timeCreated + TimeUnit.SECONDS.toMillis(EXPIRE_SECONDS)) {
                sender.sendMessage(new ComponentBuilder("There are no teleport requests to accept (expired?).").color(ChatColor.RED).create());
            } else {
                sender.sendMessage(new ComponentBuilder("Teleporting ").color(ChatColor.GREEN)
                                .append(target.getDisplayName()).color(ChatColor.GOLD)
                                .append(" to you.").color(ChatColor.GREEN)
                                .create()
                );

                target.sendMessage(new ComponentBuilder("Teleporting you to ").color(ChatColor.GREEN)
                                .append(sender.getDisplayName()).color(ChatColor.GOLD)
                                .append(".").color(ChatColor.GREEN)
                                .create()
                );

                ServerInfo senderServer = sender.getServer().getInfo();
                if (!target.getServer().getInfo().equals(senderServer)) {
                    target.connect(senderServer, new Callback<Boolean>() {
                        @Override
                        public void done(Boolean success, Throwable throwable) {
                            if (success) {
                                gateway.sendPacket(target, new PacketTeleport(senderUUID));
                            }
                        }
                    });
                }
            }

            tpaRequests.remove(searchQuery);
        }
    }

    @Override
    public void run() {
        for (Map.Entry<Map.Entry<UUID, UUID>, Long> entry : new HashSet<>(tpaRequests.entrySet())) { // copy as we modify
            ProxyServer proxyServer = plugin.getProxy();
            Map.Entry<UUID, UUID> pair = entry.getKey();
            UUID senderUUID = pair.getKey();
            UUID targetUUID = pair.getValue();
            Long millisCreated = entry.getValue();

            if (proxyServer.getPlayer(senderUUID) == null
                    || proxyServer.getPlayer(targetUUID) == null
                    || System.currentTimeMillis() > millisCreated + TimeUnit.SECONDS.toMillis(EXPIRE_SECONDS)) {
                tpaRequests.remove(entry.getKey());
            }
        }
    }
}
