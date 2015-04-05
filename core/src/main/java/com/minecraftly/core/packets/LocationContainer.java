package com.minecraftly.core.packets;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for storing a location on a BungeeCord server.
 */
public class LocationContainer implements Serializable {

    private static final long serialVersionUID = -4595168505846619275L;

    private String world;
    private double x, y, z;
    private float yaw, pitch;

    public LocationContainer(Map<String, Object> data) {
        this((String) data.get("world"),
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                ((Double) data.get("yaw")).floatValue(),
                ((Double) data.get("pitch")).floatValue());
    }

    public LocationContainer(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = Preconditions.checkNotNull(world);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("world", world);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("yaw", yaw);
        data.put("pitch", pitch);
        return data;
    }
}
