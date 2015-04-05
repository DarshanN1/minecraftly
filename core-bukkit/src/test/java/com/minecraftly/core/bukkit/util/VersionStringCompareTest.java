package com.minecraftly.core.bukkit.util;

import org.junit.Test;

import static com.minecraftly.core.bukkit.utilities.BukkitUtilities.compareVersions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test to confirm Utilities#compareVersions method functions correctly in a variety of conditions.
 * Who cares if I went over the top?
 */
public class VersionStringCompareTest {

    @Test
    public void testSimple() {
        assertEquals(0, compareVersions("1.0.0", "1.0.0")); // check args same
        assertEquals(1, compareVersions("1.0.0", "0.0.1")); // check arg 1 bigger
        assertEquals(-1, compareVersions("0.0.1", "1.0.0")); // check arg 2 bigger
    }

    @Test
    public void testHarder() {
        assertEquals(-1, compareVersions("0.5.1", "8.1.6"));
        assertEquals(1, compareVersions("8.1.2", "1.2.8.3"));
    }

    @Test
    public void testWithLetters() {
        assertEquals(-1, compareVersions("1.0.0a", "1.0.0b"));
        assertEquals(-1, compareVersions("8.5.6", "8.5.6b"));
        assertEquals(1, compareVersions("1.0.0", "0.0.1b"));
    }

    @Test
    public void testExtreme() { // I went a lil crazy here
        assertEquals(-1, compareVersions("8.0.5.6.64.1.2g", "3646.68.868.47.875a"));
        assertEquals(1, compareVersions("3646.68.868.47.875a", "8.0.5.6.64.1.2g"));
    }

    @Test
    public void testRegexAndExceptionThrowing() {
        try {
            compareVersions("0.3.5aa", "1.0.0"); // this should throw an exception, meaning the below fail() isn't executed
            fail("IllegalArgumentException was not thrown by invalid version string.");
        } catch (IllegalArgumentException ignored) {
        }

        // todo more?
    }

}
