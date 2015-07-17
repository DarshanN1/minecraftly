package com.minecraftly.core.bukkit.modules.chest.data;

import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Created by Keir on 05/07/2015.
 */
public class ChestStorageHandler extends DataStorageHandler<UserChestData> implements AutoInitializedData<UserChestData> {

    public ChestStorageHandler(Supplier<QueryRunner> queryRunnerSupplier) {
        super(queryRunnerSupplier);
    }

    @Override
    public void initialize() throws SQLException {
        getQueryRunnerSupplier().get().update(
                String.format(
                        "CREATE TABLE IF NOT EXISTS `%suser_chests` (`uuid` BINARY(16) NOT NULL, `extra_data` LONGTEXT NOT NULL, PRIMARY KEY (`uuid`))",
                        DatabaseManager.TABLE_PREFIX
                )
        );
    }

    @Override
    public UserChestData autoInitialize(User user) {
        return new UserChestData(user, getQueryRunnerSupplier());
    }
}
