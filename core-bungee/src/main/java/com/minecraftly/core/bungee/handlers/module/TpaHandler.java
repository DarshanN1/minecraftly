package com.minecraftly.core.bungee.handlers.module;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.JsonObject;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
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

    public TpaHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
        this.gateway = plugin.getGateway();
        this.redisBungeeAPI = plugin.getRedisBungeeAPI();

        redisBungeeAPI.registerPubSubChannels(CHANNEL_NEW_TPA_REQUEST, CHANNEL_TPA_ACCEPT);
    }

    // sender, target, time in millis started
    private Map<Map.Entry<UUID, UUID>, Long> tpaRequests = new HashMap<>();

    @Command(aliases = "tpa", desc = "Request to teleport to a player.", usage = "<player>", min = 1, max = 1)
    public void newTpaRequest(@Sender ProxiedPlayer sender, String inputName) {
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
                sender.sendMessage(new ComponentBuilder("You may not teleport to yourself.").color(ChatColor.RED).create());
            } else {
                UUID targetUUID = redisBungeeAPI.getUuidFromName(targetName);
                JsonObject jsonData = toJsonObject(sender.getUniqueId(), sender.getName(), targetUUID, targetName);
                redisBungeeAPI.sendChannelMessage(CHANNEL_NEW_TPA_REQUEST, plugin.getGson().toJson(jsonData));

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
        String initiatorName = BungeeUtilities.matchRedisPlayer(initiatorNameInput);

        if (initiatorName == null) {
            teleportTarget.sendMessage(new ComponentBuilder("Couldn't find a player by the name of ").color(ChatColor.RED)
                            .append(initiatorNameInput).color(ChatColor.GOLD)
                            .append(".").color(ChatColor.RED)
                            .create()
            );
        } else {
            final UUID teleportTargetUUID = teleportTarget.getUniqueId();
            UUID initiatorUUID = redisBungeeAPI.getUuidFromName(initiatorName);

            Map.Entry<UUID, UUID> searchQuery = new AbstractMap.SimpleImmutableEntry<>(initiatorUUID, teleportTargetUUID);
            Long timeCreated = tpaRequests.get(searchQuery);

            if (timeCreated == null || System.currentTimeMillis() > timeCreated + TimeUnit.SECONDS.toMillis(EXPIRE_SECONDS)) {
                teleportTarget.sendMessage(new ComponentBuilder("There are no teleport requests to accept (expired?).").color(ChatColor.RED).create());
            } else {
                teleportTarget.sendMessage(new ComponentBuilder("Teleporting ").color(ChatColor.GREEN)
                                .append(initiatorName).color(ChatColor.GOLD)
                                .append(" to you.").color(ChatColor.GREEN)
                                .create()
                );

                JsonObject jsonData = toJsonObject(initiatorUUID, initiatorName, teleportTargetUUID, teleportTarget.getName());
                redisBungeeAPI.sendChannelMessage(CHANNEL_TPA_ACCEPT, plugin.getGson().toJson(jsonData));
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
        if (!channel.equals(CHANNEL_NEW_TPA_REQUEST) && !channel.equals(CHANNEL_TPA_ACCEPT)) return;

        JsonObject jsonObject = plugin.getGson().fromJson(e.getMessage(), JsonObject.class);

        JsonObject initiatorData = jsonObject.getAsJsonObject("initiator");
        String initiatorName = initiatorData.get("name").getAsString();
        UUID initiatorUUID = UUID.fromString(initiatorData.get("uuid").getAsString());

        JsonObject teleportTargetData = jsonObject.getAsJsonObject("teleportTarget");
        String teleportTargetName = teleportTargetData.get("name").getAsString();
        final UUID teleportTargetUUID = UUID.fromString(teleportTargetData.get("uuid").getAsString());

        if (channel.equals(CHANNEL_NEW_TPA_REQUEST)) {
            ProxiedPlayer target = plugin.getProxy().getPlayer(teleportTargetUUID);

            if (target != null) {
                tpaRequests.put(new AbstractMap.SimpleImmutableEntry<>(initiatorUUID, target.getUniqueId()), System.currentTimeMillis());

                target.sendMessage(new ComponentBuilder(initiatorName).color(ChatColor.GOLD)
                                .append(" has sent you a teleport request.\nUse ").color(ChatColor.AQUA)
                                .append("/tpaccept " + initiatorName).color(ChatColor.GOLD)
                                .append(" to accept.").color(ChatColor.AQUA)
                                .create()
                );
            }
        } else if (channel.equals(CHANNEL_TPA_ACCEPT)) {
            final ProxiedPlayer initiator = plugin.getProxy().getPlayer(initiatorUUID);

            if (initiator != null) {
                ServerInfo teleportTargetServer = redisBungeeAPI.getServerFor(teleportTargetUUID);

                initiator.sendMessage(new ComponentBuilder("Teleporting you to ").color(ChatColor.GREEN)
                                .append(teleportTargetName).color(ChatColor.GOLD)
                                .append(".").color(ChatColor.GREEN)
                                .create()
                );

                if (initiator.getServer().getInfo().equals(teleportTargetServer)) {
                    gateway.sendPacket(initiator, new PacketTeleport(teleportTargetUUID));
                } else {
                    initiator.connect(teleportTargetServer);

                    // todo remove cast
                    plugin.getJobManager().getJobQueue(ConnectJobQueue.class).addJob(initiatorUUID, (proxiedPlayer, serverConnection) -> {
                        gateway.sendPacketServer(serverConnection, new PacketTeleport(teleportTargetUUID));
                    });
                }
            }
        }
    }

    public JsonObject toJsonObject(UUID initiatorUUID, String initiatorName, UUID targetUUID, String targetName) { // todo move to util class for other command
        checkNotNull(initiatorUUID);
        checkNotNull(initiatorName);
        checkNotNull(targetUUID);
        checkNotNull(targetName);

        JsonObject requestData = new JsonObject();

        JsonObject initiatorDetails = new JsonObject();
        initiatorDetails.addProperty("uuid", initiatorUUID.toString());
        initiatorDetails.addProperty("name", initiatorName);
        requestData.add("initiator", initiatorDetails);

        JsonObject teleportTarget = new JsonObject();
        teleportTarget.addProperty("uuid", targetUUID.toString());
        teleportTarget.addProperty("name", targetName);
        requestData.add("teleportTarget", teleportTarget);

        return requestData;
    }
}
