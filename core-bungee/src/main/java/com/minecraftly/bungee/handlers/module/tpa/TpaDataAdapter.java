package com.minecraftly.bungee.handlers.module.tpa;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Handles serializing and des-erializing of the {@link TpaData} class.
 */
public class TpaDataAdapter implements JsonSerializer<TpaData>, JsonDeserializer<TpaData> {

    @Override
    public JsonElement serialize(TpaData tpaData, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("initiator", tpaData.getInitiatorActor().toString());
        jsonObject.addProperty("target", tpaData.getTargetActor().toString());
        jsonObject.addProperty("direction", tpaData.getDirection().name());
        jsonObject.addProperty("created", tpaData.getTimeCreated());
        return jsonObject;
    }

    @Override
    public TpaData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        UUID initiator = UUID.fromString(jsonObject.get("initiator").getAsString());
        UUID target = UUID.fromString(jsonObject.get("target").getAsString());
        TpaData.Direction direction = TpaData.Direction.valueOf(jsonObject.get("direction").getAsString());
        long created = jsonObject.get("created").getAsLong();

        return new TpaData(initiator, target, direction, created);
    }
}
