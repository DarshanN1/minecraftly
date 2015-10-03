package com.minecraftly.core.bungee.handlers.module.tpa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.bungee.handlers.RedisMessagingHandler;
import com.minecraftly.core.bungee.handlers.job.queue.ConnectJobQueue;
import com.minecraftly.core.bungee.utilities.BungeeUtilities;
import com.minecraftly.core.packets.PacketTeleport;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
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

    public static final String CHANNEL_NEW_TPA_REQUEST = "mcly-TpaRequest";
    public static final String CHANNEL_TPA_ACCEPT = "mcly-TpaAccept";

    private MclyCoreBungeePlugin plugin;
    private ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private RedisBungeeAPI redisBungeeAPI;
    private int EXPIRE_SECONDS = 60;

    // sender, target, data
    private Map<Map.Entry<UUID, UUID>, TpaData> tpaRequests = new HashMap<>();

    public TpaHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();
        this.redisBungeeAPI = plugin.getRedisBungeeAPI();

        redisBungeeAPI.registerPubSubChannels(CHANNEL_NEW_TPA_REQUEST, CHANNEL_TPA_ACCEPT);
    }

    @Command(aliases = "tpa", desc = "Request to teleport to a player.", usage = "<player>", min = 1, max = 1)
    public void newTpaToRequest(@Sender ProxiedPlayer sender, String inputName) {
        newTpaRequest(sender, inputName, TpaData.Direction.TO_RECEIVER);
    }

    @Command(aliases = "tpahere", desc = "Request to a player to teleport to you.", usage = "<player>", min = 1, max = 1)
    public void newTpaHereRequest(@Sender ProxiedPlayer sender, String inputName) {
        newTpaRequest(sender, inputName, TpaData.Direction.TO_SENDER);
    }

    public void newTpaRequest(ProxiedPlayer sender, String inputName, TpaData.Direction direction) {
        if (!plugin.getHumanCheckManager().isHumanVerified(sender)) {
            sender.sendMessage(MclyCoreBungeePlugin.MESSAGE_NOT_HUMAN);
        } else {
            String targetName = BungeeUtilities.matchRedisPlayer(inputName);

            if (targetName == null) {
                sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                                .append(inputName).color(ChatColor.GOLD)
                                .append(".").color(ChatColor.RED)
                                .create()
                );
            } else if (sender.getName().equals(targetName)) {
                sender.sendMessage(new ComponentBuilder("You may not use yourself in this command.").color(ChatColor.RED).create());
            } else {
                UUID targetUUID = redisBungeeAPI.getUuidFromName(targetName);
                TpaData tpaData = new TpaData(sender.getUniqueId(), targetUUID, direction);
                redisBungeeAPI.sendChannelMessage(CHANNEL_NEW_TPA_REQUEST, plugin.getGson().toJson(tpaData));

                sender.sendMessage(new ComponentBuilder("Request successfully sent to ").color(ChatColor.GREEN)
                                .append(targetName).color(ChatColor.GOLD)
                                .append(".").color(ChatColor.GREEN)
                                .create()
                );
            }
        }
    }

    @Command(aliases = "tpaccept", desc = "Accept a teleport request from a player.", usage = "<player>", min = 1, max = 1)
    public void acceptTpaRequest(@Sender ProxiedPlayer teleportTarget, String initiatorNameInput) {
        String initiatorName = getInitiatorName(teleportTarget, initiatorNameInput);

        if (initiatorName != null) {
            final UUID teleportTargetUUID = teleportTarget.getUniqueId();
            UUID initiatorUUID = redisBungeeAPI.getUuidFromName(initiatorName);
            Map.Entry<UUID, UUID> searchQuery = createSearchQuery(initiatorUUID, teleportTargetUUID);
            TpaData tpaData = tpaRequests.get(searchQuery);

            if (tpaData == null || hasExpired(tpaData.getTimeCreated())) {
                teleportTarget.sendMessage(new ComponentBuilder("That player hasn't sent you a teleport request (expired?).").color(ChatColor.RED).create()); // todo dupe
            } else {
                redisBungeeAPI.sendChannelMessage(CHANNEL_TPA_ACCEPT, plugin.getGson().toJson(tpaData));
            }

            tpaRequests.remove(searchQuery);
        }
    }

    @Command(aliases = "tpdeny", desc = "Deny a teleport request from a player.", usage = "<player>", min = 1, max = 1)
    public void denyTpaRequest(@Sender ProxiedPlayer teleportTarget, String initiatorNameInput) {
        String initiatorName = getInitiatorName(teleportTarget, initiatorNameInput);

        if (initiatorName != null) {
            UUID teleportTargetUUID = teleportTarget.getUniqueId();
            UUID initiatorUUID = redisBungeeAPI.getUuidFromName(initiatorName);
            Map.Entry<UUID, UUID> searchQuery = createSearchQuery(initiatorUUID, teleportTargetUUID);

            if (tpaRequests.remove(searchQuery) != null) {
                teleportTarget.sendMessage(new ComponentBuilder("Successfully cancelled teleport request.").color(ChatColor.RED).create());

                RedisMessagingHandler.sendMessage(initiatorUUID,
                        new ComponentBuilder(teleportTarget.getDisplayName())
                                .color(ChatColor.GOLD)
                                .append(" has cancelled your teleport request.")
                                .color(ChatColor.RED).create()
                );
            } else {
                teleportTarget.sendMessage(new ComponentBuilder("That player hasn't sent you a teleport request (expired?).").color(ChatColor.RED).create()); // todo dupe
            }
        }
    }

    /** HANDLERS **/

    @Override
    public void run() {
        for (Map.Entry<Map.Entry<UUID, UUID>, TpaData> entry : new HashSet<>(tpaRequests.entrySet())) { // copy as we modify
            ProxyServer proxyServer = plugin.getProxy();
            Map.Entry<UUID, UUID> pair = entry.getKey();
            UUID senderUUID = pair.getKey();
            UUID targetUUID = pair.getValue();
            Long millisCreated = entry.getValue().getTimeCreated();

            if (proxyServer.getPlayer(senderUUID) == null || proxyServer.getPlayer(targetUUID) == null || hasExpired(millisCreated)) {
                tpaRequests.remove(entry.getKey());
            }
        }
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent e) {
        String channel = e.getChannel();
        if (!channel.equals(CHANNEL_NEW_TPA_REQUEST) && !channel.equals(CHANNEL_TPA_ACCEPT)) return;

        TpaData tpaData = plugin.getGson().fromJson(e.getMessage(), TpaData.class);
        TpaData.Direction direction = tpaData.getDirection();
        UUID initiatorUUID = tpaData.getInitiatorActor();
        UUID targetUUID = tpaData.getTargetActor();

        if (channel.equals(CHANNEL_NEW_TPA_REQUEST)) {
            ProxiedPlayer target = plugin.getProxy().getPlayer(targetUUID);

            if (target != null) {
                String initiatorName = redisBungeeAPI.getNameFromUuid(initiatorUUID);
                tpaRequests.put(new AbstractMap.SimpleImmutableEntry<>(initiatorUUID, targetUUID), tpaData);

                target.sendMessage(new ComponentBuilder(initiatorName).color(ChatColor.GOLD)
                                .append(" " + direction.getInviteMessage()).color(ChatColor.AQUA)
                                .append("\nUse ").color(ChatColor.AQUA)
                                .append("/tpaccept " + initiatorName).color(ChatColor.GOLD)
                                .append(" to accept.").color(ChatColor.AQUA)
                                .append("\n/tpdeny " + initiatorName).color(ChatColor.GOLD)
                                .append(" to deny.").color(ChatColor.AQUA)
                                .create()
                );
            }
        } else if (channel.equals(CHANNEL_TPA_ACCEPT)) {
            final ProxiedPlayer movingPlayer = plugin.getProxy().getPlayer(tpaData.getMovingActor());

            if (movingPlayer != null) {
                teleport(movingPlayer, tpaData.getDestinationActor());
            }
        }
    }

    /** HELPER METHODS **/

    private String getInitiatorName(ProxiedPlayer sender, String input) {
        String initiatorName = BungeeUtilities.matchRedisPlayer(input);

        if (initiatorName == null) {
            sender.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                            .append(input).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.RED)
                            .create()
            );
        }

        return initiatorName;
    }

    private Map.Entry<UUID, UUID> createSearchQuery(UUID initiatorUUID, UUID teleportTargetUUID) {
        return new AbstractMap.SimpleImmutableEntry<>(initiatorUUID, teleportTargetUUID);
    }

    private boolean hasExpired(long timeCreated) {
        return System.currentTimeMillis() > timeCreated + TimeUnit.SECONDS.toMillis(EXPIRE_SECONDS);
    }

    private void teleport(ProxiedPlayer player, UUID targetUUID) {
        String targetName = redisBungeeAPI.getNameFromUuid(targetUUID);
        ServerInfo teleportTargetServer = redisBungeeAPI.getServerFor(targetUUID);

        player.sendMessage(new ComponentBuilder("Teleporting you to ").color(ChatColor.GREEN)
                        .append(targetName).color(ChatColor.GOLD)
                        .append(".").color(ChatColor.GREEN)
                        .create()
        );

        if (player.getServer().getInfo().equals(teleportTargetServer)) {
            gateway.sendPacket(player, new PacketTeleport(targetUUID));
        } else {
            player.connect(teleportTargetServer);

            // todo remove cast
            plugin.getJobManager().getJobQueue(ConnectJobQueue.class).addJob(player.getUniqueId(), (proxiedPlayer, serverConnection) -> {
                gateway.sendPacketServer(serverConnection, new PacketTeleport(targetUUID));
            });
        }
    }

}
