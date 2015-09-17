package com.minecraftly.core.bukkit.modules.playerworlds.task;

import com.minecraftly.core.utilities.ComputeEngineHelper;
import org.apache.commons.io.FileUtils;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 17/09/2015.
 */
public class RSyncUploadWorldTask implements Runnable {

    private File worldFolder;
    private UUID worldOwner;
    private boolean deleteLocally;
    private Logger logger;

    public RSyncUploadWorldTask(World world, UUID worldOwner, boolean deleteLocally, Logger logger) {
        this.worldFolder = world.getWorldFolder();
        this.worldOwner = worldOwner;
        this.deleteLocally = deleteLocally;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            File sessionLock = new File(worldFolder, "session.lock");

            if (deleteLocally && sessionLock.exists()) {
                if (!sessionLock.delete()) {
                    logger.warning("Error deleting session lock file for RSync upload.");
                }
            }

            boolean rsyncSuccess = ComputeEngineHelper.rsync(worldFolder.getCanonicalPath(), "gs://worlds/" + worldOwner);

            if (deleteLocally && rsyncSuccess) {
                try {
                    FileUtils.deleteDirectory(worldFolder);
                } catch (IOException e1) {
                    logger.log(Level.SEVERE, "Error whilst deleting world directory: " + worldFolder.getPath() + ".", e1);
                }
            } else {
                logger.log(Level.SEVERE, "RSync for world failed, will not delete directory (" + worldFolder.getPath() + ")");
            }
        } catch (IOException | InterruptedException e1) {
            logger.log(Level.SEVERE, "Error whilst rsync'ing world to GCS.", e1);
        }
    }
}
