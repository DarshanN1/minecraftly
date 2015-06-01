package com.minecraftly.core.bukkit.user.modularisation;

import com.minecraftly.core.bukkit.user.User;

/**
 * A {@link UserData} instance of which there will only be 1 instance of for each {@link User}.
 */
public abstract class SingletonUserData extends UserData {

    public SingletonUserData(User user) {
        super(user);
    }

}
