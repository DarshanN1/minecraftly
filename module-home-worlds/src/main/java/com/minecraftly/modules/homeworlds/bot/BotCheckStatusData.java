package com.minecraftly.modules.homeworlds.bot;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;

/**
 * Stores whether or not a player has passed the bot check for this session.
 */
public class BotCheckStatusData extends SingletonUserData {

    private boolean status = false;

    public BotCheckStatusData(User user) {
        super(user, null);
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

}
