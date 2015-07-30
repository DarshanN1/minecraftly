package com.minecraftly.core.bukkit.modules.playerworlds.data;

import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
import org.apache.commons.dbutils.QueryRunner;

import java.util.function.Supplier;

/**
 * Created by Keir on 30/07/2015.
 */
public class JoinCountdownDataHandler extends DataStorageHandler<JoinCountdownData> {

    public JoinCountdownDataHandler(Supplier<QueryRunner> queryRunnerSupplier) {
        super(queryRunnerSupplier);
    }
}
