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
        return new Location(world, 8.5, world.getSeaLevel() + 1, 8.1);
    }

    @Override
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        byte[][] result = new byte[world.getMaxHeight() / 16][];

        if (chunkX == 0 && chunkZ == 0) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 6; y++) {
                        if (y == 0 || y == 5 || x == 0 || x == 15 || z == 0 || z == 15) {
                            WorldGenUtilities.setBlock(result, x, world.getSeaLevel() + y, z, material);
                        }
                    }
                }
            }
        }

        return result;
    }
}
