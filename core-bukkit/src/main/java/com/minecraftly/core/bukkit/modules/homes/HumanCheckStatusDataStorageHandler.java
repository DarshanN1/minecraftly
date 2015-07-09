package com.minecraftly.core.bukkit.modules.homes;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;

/**
 * Created by Keir on 27/06/2015.
 */
public class HumanCheckStatusDataStorageHandler extends DataStorageHandler<HumanCheckStatusData> implements AutoInitializedData<HumanCheckStatusData> {

    public HumanCheckStatusDataStorageHandler() {
        super(null);
    }

    @Override
    public HumanCheckStatusData autoInitialize(User user) {
        return new HumanCheckStatusData(user);
    }
}
