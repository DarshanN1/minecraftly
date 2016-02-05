package com.minecraftly.bukkit.modules.readonlyworlds;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Created by Keir on 05/07/2015.
 */
public class DoNothingWorldGenerator extends ChunkGenerator {

    @Override
    public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
        return new byte[world.getMaxHeight() / 16][];
    }

}
