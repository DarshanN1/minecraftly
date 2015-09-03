package com.minecraftly.core.redis.message.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.minecraftly.core.redis.message.ServerInstanceData;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;

/**
 * Created by Keir on 28/08/2015.
 */
public class ServerDataSerializationHandler implements JsonSerializer<ServerInstanceData>, JsonDeserializer<ServerInstanceData> {

    @Override
    public JsonElement serialize(ServerInstanceData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", src.getId());

        InetSocketAddress inetSocketAddress = src.getSocketAddress();
        JsonObject socketAddressObject = new JsonObject();
        socketAddressObject.addProperty("host", inetSocketAddress.getHostString());
        socketAddressObject.addProperty("port", inetSocketAddress.getPort());

        jsonObject.add("socketAddress", socketAddressObject);
        return jsonObject;
    }

    @Override
    public ServerInstanceData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String id = jsonObject.get("id").getAsString();

        JsonObject socketAddressObject = jsonObject.get("socketAddress").getAsJsonObject();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(socketAddressObject.get("host").getAsString(), socketAddressObject.get("port").getAsInt());

        return new ServerInstanceData(id, inetSocketAddress);
    }
}
