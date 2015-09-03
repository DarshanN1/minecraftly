package com.minecraftly.core.redis.message.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minecraftly.core.redis.message.ServerInstanceData;

/**
 * Created by Keir on 30/08/2015.
 */
public class GsonHelper {

    private GsonHelper() {}

    public static Gson getGsonWithAdapters() {
        return new GsonBuilder()
                .registerTypeAdapter(ServerInstanceData.class, new ServerDataSerializationHandler())
                .create();
    }

}
