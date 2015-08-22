package com.minecraftly.core.bukkit.modules.playerworlds.task;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.data.JoinCountdownData;
import com.minecraftly.core.bukkit.user.User;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by Keir on 18/08/2015.
 */
public class JoinCountdownTask extends BukkitRunnable {

    private ModulePlayerWorlds modulePlayerWorlds;
    private LanguageValue langTeleportCountdown;
    private User user;
    private World world;

    private int countdown = 5;

    public JoinCountdownTask(ModulePlayerWorlds modulePlayerWorlds, LanguageValue langTeleportCountdown, User user, World world) {
        this.modulePlayerWorlds = modulePlayerWorlds;
        this.langTeleportCountdown = langTeleportCountdown;
        this.user = user;
        this.world = world;

        checkForExistingTasks();
        user.attachUserData(new JoinCountdownData(user, this));
    }

    @Override
    public void run() {
        Player player = user.getPlayer();

        if (player == null) { // self-destruct
            cancel();
        }

        langTeleportCountdown.send(player, countdown--);

        if (countdown <= 0) {
            modulePlayerWorlds.spawnInWorld(player, world);
            cancel();
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        checkForExistingTasks();
        super.cancel();
    }

    private void checkForExistingTasks() {
        JoinCountdownData joinCountdownData = user.getSingletonUserData(JoinCountdownData.class);

        if (joinCountdownData != null) {
            BukkitRunnable existingTask = joinCountdownData.getCountdownTask();

            if (existingTask != null && existingTask != this) { // second check prevents infinite loop
                existingTask.cancel();
            }

            user.detachUserData(joinCountdownData);
        }
    }
}
