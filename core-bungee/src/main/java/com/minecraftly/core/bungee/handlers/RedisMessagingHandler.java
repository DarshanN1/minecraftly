package com.minecraftly.core.bungee.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

/**
 * Contains general redis functions which should really be built-in to RedisBungee.
 */
public class RedisMessagingHandler implements Listener {

    public static final String MESSAGE_PLAYER_CHANNEL = "mcly_message_player";

    private static final Gson gson = new Gson();

    @Deprecated
    public static void sendMessage(UUID uuid, String message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("player_uuid", uuid.toString());
        jsonObject.addProperty("message_raw", message);

        RedisBungee.getApi().sendChannelMessage(MESSAGE_PLAYER_CHANNEL, gson.toJson(jsonObject));
    }

    public static void sendMessage(UUID uuid, BaseComponent[] baseComponents) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("player_uuid", uuid.toString());
        jsonObject.addProperty("message_json", ComponentSerializer.toString(baseComponents));

        RedisBungee.getApi().sendChannelMessage(MESSAGE_PLAYER_CHANNEL, gson.toJson(jsonObject));
    }

    @EventHandler
    public void onMsg(PubSubMessageEvent e) {
        if (e.getChannel().equals(MESSAGE_PLAYER_CHANNEL)) {
            JsonObject jsonObject = gson.fromJson(e.getMessage(), JsonObject.class);
            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(jsonObject.get("player_uuid").getAsString()));

            if (proxiedPlayer != null) {
                 if (jsonObject.has("message_json")) {
                     proxiedPlayer.sendMessage(ComponentSerializer.parse(jsonObject.get("message_json").getAsString()));
                 } else if (jsonObject.has("message_raw")) {
                     proxiedPlayer.sendMessage(TextComponent.fromLegacyText(jsonObject.get("message_raw").getAsString()));
                 } else {
                     throw new IllegalArgumentException("PubSub player message contains no message to send.");
                 }
            }
        }
    }

}
