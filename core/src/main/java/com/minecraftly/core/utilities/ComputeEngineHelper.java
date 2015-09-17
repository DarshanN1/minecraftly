package com.minecraftly.core.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * These methods will only work in Google Compute Engine environments, running these elsewhere will result in failure.
 */
public class ComputeEngineHelper {

    private ComputeEngineHelper() {}

    /**
     * Gets this instance's unique id.
     * This value never changes and therefore should be cached where possible.
     *
     * @return the instance's unique id
     * @throws IOException
     */
    public static String queryUniqueId() throws IOException {
        return queryComputeAPI("http://metadata.google.internal/computeMetadata/v1/instance/id");
    }

    /**
     * Gets this instance's ip address.
     * This value never changes and therefore should be cached where possible.
     *
     * @return the instance's unique id
     * @throws IOException
     */
    public static String queryIpAddress() throws IOException {
        return queryComputeAPI("http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/ip");
    }

    /**
     * Queries the compute api on the url specified and returns the response.
     * A lot of the API responses are values that never change and therefore should be cached where possible.
     *
     * @param url the compute API url to query
     * @return the response
     * @throws IOException
     */
    public static String queryComputeAPI(String url) throws IOException {
        try {
            URLConnection computeIdUrlConnection = new URL(url).openConnection();
            computeIdUrlConnection.addRequestProperty("Metadata-Flavor", "Google");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(computeIdUrlConnection.getInputStream()))) {
                return reader.readLine(); // compute is single line response
            }
        } catch (IOException e) {
            throw new IOException("Error retrieving response from Google Compute API.", e);
        }
    }

    /**
     * Checks if a world exists in Google Cloud Storage.
     *
     * @param worldName the name of the world to check for existance
     * @return true if the world exists, false otherwise
     * @throws IOException thrown if there is an io exception whilst executing the stat command
     * @throws InterruptedException thrown if the thread is interrupted whilst waiting for the stat command to finish
     */
    public static boolean worldExists(String worldName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("gsutil", "stat", "gs://worlds/" + worldName + "/level.dat");
        Process process = processBuilder.start();
        int returnCode = process.waitFor();

        if (returnCode != 0 && returnCode != 1) {
            throw new RuntimeException("Exception during statistic; command = \"" + String.join(" ", processBuilder.command()) + "\"; code = " + returnCode);
        }

        return returnCode == 0;
    }

    /**
     * Performs an rsync function (only works on systems with rsync installed).
     * This method is blocking.
     *
     * @param source source path
     * @param destination destination path
     * @return boolean value containing whether or not the source file existed
     * @throws IOException thrown if there is an io exception whilst executing the rsync command
     * @throws InterruptedException thrown if the thread is interrupted whilst waiting for the rsync command to finish
     */
    public static boolean rsync(String source, String destination) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("gsutil", "-m", "rsync", "-c", "-d", "-r", source, destination);
        Process process = processBuilder.start();
        int returnCode = process.waitFor();

        if (returnCode == 3) {
            return false;
        } else if (returnCode != 0) {
            throw new RuntimeException("Exception during RSync; command = \"" + String.join(" ", processBuilder.command()) + "\"; code = " + returnCode);
        } else {
            return true;
        }
    }

}
