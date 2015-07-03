package com.minecraftly.core.bukkit.modules.homes;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;

/**
 * Created by Keir on 27/06/2015.
 */
public class BotCheckStatusDataStorageHandler extends DataStorageHandler<BotCheckStatusData> implements AutoInitializedData<BotCheckStatusData> {

    public BotCheckStatusDataStorageHandler() {
        super(null);
    }

    @Override
    public BotCheckStatusData autoInitialize(User user) {
        return new BotCheckStatusData(user);
    }
}
