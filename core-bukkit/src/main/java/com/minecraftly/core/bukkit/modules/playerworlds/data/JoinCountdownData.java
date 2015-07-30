package com.minecraftly.core.bukkit.modules.playerworlds.data;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinCountdownData extends SingletonUserData {

    private BukkitRunnable countdownTask;

    public JoinCountdownData(User user, BukkitRunnable countdownTask) {
        super(user, null);
        this.countdownTask = countdownTask;
    }

    public BukkitRunnable getCountdownTask() {
        return countdownTask;
    }
}
