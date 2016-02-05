package com.minecraftly.bukkit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.minecraftly.bukkit.utilities.BukkitUtilities;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to confirm Utilities#compareVersions method functions correctly in a variety of conditions.
 * Who cares if I went over the top?
 * todo move to core
 */
public class VersionStringCompareTest {

    @Test
    public void testSimple() {
        Assert.assertEquals(0, BukkitUtilities.compareVersions("1.0.0", "1.0.0")); // check args same
        Assert.assertEquals(1, BukkitUtilities.compareVersions("1.0.0", "0.0.1")); // check arg 1 bigger
        Assert.assertEquals(-1, BukkitUtilities.compareVersions("0.0.1", "1.0.0")); // check arg 2 bigger
    }

    @Test
    public void testHarder() {
        Assert.assertEquals(-1, BukkitUtilities.compareVersions("0.5.1", "8.1.6"));
        Assert.assertEquals(1, BukkitUtilities.compareVersions("8.1.2", "1.2.8.3"));
    }

    @Test
    public void testWithLetters() {
        Assert.assertEquals(-1, BukkitUtilities.compareVersions("1.0.0a", "1.0.0b"));
        Assert.assertEquals(-1, BukkitUtilities.compareVersions("8.5.6", "8.5.6b"));
        Assert.assertEquals(1, BukkitUtilities.compareVersions("1.0.0", "0.0.1b"));
    }

    @Test
    public void testExtreme() { // I went a lil crazy here
        Assert.assertEquals(-1, BukkitUtilities.compareVersions("8.0.5.6.64.1.2g", "3646.68.868.47.875a"));
        Assert.assertEquals(1, BukkitUtilities.compareVersions("3646.68.868.47.875a", "8.0.5.6.64.1.2g"));
    }

    @Test
    public void testRegexAndExceptionThrowing() {
        try {
            BukkitUtilities.compareVersions("0.3.5aa", "1.0.0"); // this should throw an exception, meaning the below fail() isn't executed
            fail("IllegalArgumentException was not thrown by invalid version string.");
        } catch (IllegalArgumentException ignored) {
        }

        // todo more?
    }

}
