package com.minecraftly.modules.spawn;

import com.minecraftly.core.bukkit.utilities.WorldGenUtilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Custom world generator.
 * Will generate a void world with a 16x16x5 room at chunk 0,0 at sea level.
 */
public class VoidGenerator extends ChunkGenerator {

    private final int BASE_LEVEL = 63;

    private Material material;

    /**
     * Instigates the class.
     *
     * @param material the material to create the room from.
     */
    public VoidGenerator(Material material) {
        this.material = material;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 264, BASE_LEVEL + 3, 264); // spawn a few higher to prevent falling through floor
    }

    @Override
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        byte[][] result = new byte[world.getMaxHeight() / 16][];

        if (chunkX == 16 && chunkZ == 16) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -6; y < 6; y++) {
                        if (y == -6 || y == 0 || y == 5 || x == 0 || x == 15 || z == 0 || z == 15) {
                            WorldGenUtilities.setBlock(result, x, BASE_LEVEL + y, z, material);
                        }
                    }
                }
            }
        }

        return result;
    }
}
