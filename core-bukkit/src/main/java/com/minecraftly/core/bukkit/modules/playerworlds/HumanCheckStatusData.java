package com.minecraftly.core.bukkit.modules.playerworlds;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;

/**
 * Stores whether or not a player has passed the bot check for this session.
 */
public class HumanCheckStatusData extends SingletonUserData {

    // when we receive the bot check packet, this will be set to false
    private boolean status = true;

    public HumanCheckStatusData(User user) {
        super(user, null);
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

}
