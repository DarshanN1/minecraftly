package com.minecraftly.core.bukkit.redis;

import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Keir on 27/10/2015.
 */
public class CachedUUIDEntry {

    private final String name;
    private final UUID uuid;
    private final Calendar expiry;

    public CachedUUIDEntry(String name, UUID uuid, Calendar expiry) {
        this.name = name;
        this.uuid = uuid;
        this.expiry = expiry;
    }

    public String getName() {
        return name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Calendar getExpiry() {
        return expiry;
    }

    public boolean expired() {
        return Calendar.getInstance().after(expiry);
    }
}
