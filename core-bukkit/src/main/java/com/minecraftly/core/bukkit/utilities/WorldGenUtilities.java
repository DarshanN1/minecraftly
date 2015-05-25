package com.minecraftly.core.bukkit.utilities;

import org.bukkit.Material;

/**
 * Utility methods to aid in the creation of custom world generators.
 */
public class WorldGenUtilities {

    /**
     * Sets the block type at a specific coordinate.
     *
     * @param result the 2-dimensional array being used to generate the world
     * @param x the x coordinate
     * @param y the x coordinate
     * @param z the x coordinate
     * @param material the type to set the block to
     */
    public static void setBlock(byte[][] result, int x, int y, int z, Material material) {
        if (result[y >> 4] == null) {
            result[y >> 4] = new byte[4096];
        }

        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = (byte) material.getId();
    }

}
