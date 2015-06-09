package com.minecraftly.modules.homeworlds.data;

import java.util.UUID;

/**
 * Created by Keir on 08/06/2015.
 */
public class SQLUtils {

    private SQLUtils() {}

    public static String convertUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

}
