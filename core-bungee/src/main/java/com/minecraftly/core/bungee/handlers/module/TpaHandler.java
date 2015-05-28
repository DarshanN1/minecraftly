package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
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
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Provides TPA functionality.
 */
public class TpaHandler implements Runnable, Listener {

    public static final String CHANNEL_NEW_TPA_REQUEST = "TpaRequest";
    public static final String CHANNEL_TPA_ACCEPT = "TpaAccept";

    private MclyCoreBungeePlugin plugin;
    private ProxyGateway<ProxiedPlayer, ServerInfo> gateway;
    private RedisBungeeAPI redisBungeeAPI;
    private int EXPIRE_SECONDS = 60;

    public TpaHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();
        this.redisBungeeAPI = plugin.getRedisBungeeAPI();

        redisBungeeAPI.registerPubSubChannels(CHANNEL_NEW_TPA_REQUEST, CHANNEL_TPA_ACCEPT);
    }

    // sender, target, time in millis started
    private Map<Map.Entry<UUID, UUID>, Long> tpaRequests = new HashMap<>();

    @Command(aliases = "tpa", desc = "Request to teleport to a player.", usage = "<player>", min = 1, max = 1)
    public void newTpaRequest(ProxiedPlayer sender, String inputName) {
        String targetName = BungeeUtilities.matchRedisPlayer(inputName);
        if (targetName == null) {
            sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                            .append(inputName).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.RED)
                            .create()
            );
        } else if (sender.getName().equals(targetName)) {
            sender.sendMessage(new ComponentBuilder("You may not teleport to yourself.").color(ChatColor.RED).create());
        } else {
            UUID targetUUID = redisBungeeAPI.getUuidFromName(targetName);
            redisBungeeAPI.sendChannelMessage(CHANNEL_NEW_TPA_REQUEST, sender.getUniqueId() + "," + targetUUID);

            sender.sendMessage(new ComponentBuilder("Request successfully sent to ").color(ChatColor.GREEN)
                            .append(targetName).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.GREEN)
                            .create()
            );
        }
    }

    @Command(aliases = "tpaccept", desc = "Accept a teleport request from a player.", usage = "<player>", min = 1, max = 1)
    public void acceptTpaRequest(ProxiedPlayer sender, String inputName) {
        String targetName = BungeeUtilities.matchRedisPlayer(inputName);

        if (targetName == null) {
            sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                    .append(inputName).color(ChatColor.GOLD)
                    .append(".").color(ChatColor.RED)
                    .create()
            );
        } else {
            final UUID senderUUID = sender.getUniqueId();
            UUID targetUUID = redisBungeeAPI.getUuidFromName(targetName);

            Map.Entry<UUID, UUID> searchQuery = new AbstractMap.SimpleImmutableEntry<>(targetUUID, senderUUID);
            Long timeCreated = tpaRequests.get(searchQuery);

            if (timeCreated == null || System.currentTimeMillis() > timeCreated + TimeUnit.SECONDS.toMillis(EXPIRE_SECONDS)) {
                sender.sendMessage(new ComponentBuilder("There are no teleport requests to accept (expired?).").color(ChatColor.RED).create());
            } else {
                sender.sendMessage(new ComponentBuilder("Teleporting ").color(ChatColor.GREEN)
                                .append(targetName).color(ChatColor.GOLD)
                                .append(" to you.").color(ChatColor.GREEN)
                                .create()
                );

                redisBungeeAPI.sendChannelMessage(CHANNEL_TPA_ACCEPT, senderUUID + "," + targetUUID);
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

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent e) {
        String channel = e.getChannel();
        String message = e.getMessage();

        if (channel.equals(CHANNEL_NEW_TPA_REQUEST)) {
            String[] split = message.split(",");
            UUID targetUUID = UUID.fromString(split[1]);
            ProxiedPlayer target = plugin.getProxy().getPlayer(targetUUID);

            if (target != null) {
                UUID senderUUID = UUID.fromString(split[0]);
                String senderName = redisBungeeAPI.getNameFromUuid(senderUUID);
                tpaRequests.put(new AbstractMap.SimpleImmutableEntry<>(senderUUID, target.getUniqueId()), System.currentTimeMillis());

                target.sendMessage(new ComponentBuilder(senderName).color(ChatColor.GOLD)
                                .append(" has sent you a teleport request.\nUse ").color(ChatColor.AQUA)
                                .append("/tpaccept " + senderName).color(ChatColor.GOLD)
                                .append(" to accept.").color(ChatColor.AQUA)
                                .create()
                );
            }
        } else if (channel.equals(CHANNEL_TPA_ACCEPT)) {
            String[] split = message.split(",");
            UUID targetUUID = UUID.fromString(split[1]);
            final ProxiedPlayer target = plugin.getProxy().getPlayer(targetUUID);

            if (target != null) {
                final UUID senderUUID = UUID.fromString(split[0]);
                String senderName = redisBungeeAPI.getNameFromUuid(senderUUID);
                ServerInfo senderServer = redisBungeeAPI.getServerFor(senderUUID);

                target.sendMessage(new ComponentBuilder("Teleporting you to ").color(ChatColor.GREEN)
                                .append(senderName).color(ChatColor.GOLD)
                                .append(".").color(ChatColor.GREEN)
                                .create()
                );

                if (target.getServer().equals(senderServer)) {
                    gateway.sendPacket(target, new PacketTeleport(senderUUID));
                } else {
                    target.connect(senderServer, new Callback<Boolean>() {
                        @Override
                        public void done(Boolean success, Throwable throwable) {
                            if (success) {
                                plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        gateway.sendPacket(target, new PacketTeleport(senderUUID));
                                    }
                                }, 2, TimeUnit.SECONDS);
                            }
                        }
                    });
                }
            }
        }
    }
}
