package com.minecraftly.core.bukkit.modules.playerworlds.task;

import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.data.JoinCountdownData;
import com.minecraftly.core.bukkit.user.User;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * Created by Keir on 18/08/2015.
 */
public class JoinCountdownTask extends BukkitRunnable implements Consumer<World> {

    private ModulePlayerWorlds modulePlayerWorlds;
    private LanguageValue langTeleportCountdown;
    private User user;
    private World world = null;

    private int countdown = 5;

    public JoinCountdownTask(ModulePlayerWorlds modulePlayerWorlds, LanguageValue langTeleportCountdown, User user) {
        this.modulePlayerWorlds = modulePlayerWorlds;
        this.langTeleportCountdown = langTeleportCountdown;
        this.user = user;

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
            if (world != null) {
                modulePlayerWorlds.spawnInWorld(player, world);
            } else {
                modulePlayerWorlds.langLoadStillLoading.send(player);
            }

            cancel();
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        checkForExistingTasks();
        super.cancel();
    }

    @Override
    public void accept(World world) {
        this.world = world;

        Player player = user.getPlayer();

        if (player != null) {
            if (world == null) {
                modulePlayerWorlds.langLoadFailed.send(player);
                cancel();
            }

            if (countdown <= 0) {
                modulePlayerWorlds.spawnInWorld(player, world);
            }
        }
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
