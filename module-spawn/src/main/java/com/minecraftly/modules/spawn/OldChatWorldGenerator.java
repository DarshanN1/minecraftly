package com.minecraftly.modules.spawn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Old custom world generator. Kept for possible future use.
 * Will generate a void world with a 16x16x5 room at chunk 0,0 at sea level.
 */
public class OldChatWorldGenerator extends ChunkGenerator {

    private final int BASE_LEVEL = 63;

    private Material material;

    /**
     * Instigates the class.
     *
     * @param material the material to create the room from.
     */
    public OldChatWorldGenerator(Material material) {
        this.material = material;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 264, BASE_LEVEL + 2, 264); // spawn a few higher to prevent falling through floor
    }

    @Override
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        byte[][] result = new byte[world.getMaxHeight() / 16][];

        if (chunkX == 16 && chunkZ == 16) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -6; y < 6; y++) {
                        if (y == -6 || y == 0 || y == 5 || x == 0 || x == 15 || z == 0 || z == 15) {
                            setBlock(result, x, BASE_LEVEL + y, z, material);
                        }
                    }
                }
            }
        }

        return result;
    }

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