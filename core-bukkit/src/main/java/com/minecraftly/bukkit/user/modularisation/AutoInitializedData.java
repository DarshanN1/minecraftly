package com.minecraftly.bukkit.user.modularisation;

import com.minecraftly.bukkit.user.User;

/**
 * Created by Keir on 09/06/2015.
 */
public interface AutoInitializedData<T extends UserData> {

    T autoInitialize(User user);

}
