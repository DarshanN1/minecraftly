package com.minecraftly.bukkit.internal.intake;

import com.minecraftly.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.bukkit.user.User;
import com.minecraftly.bukkit.user.UserManager;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import net.ellune.exhaust.bukkit.provider.core.PlayerProvider;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Created by Keir on 08/07/2015.
 */
public class UserProvider implements Provider<User> {

    public static final UserProvider INSTANCE = new UserProvider(PlayerProvider.INSTANCE, MclyCoreBukkitPlugin.getInstance().getUserManager()); // todo hmm dis static

    private final PlayerProvider playerProvider;
    private final UserManager userManager;

    public UserProvider(PlayerProvider playerProvider, UserManager userManager) {
        this.playerProvider = playerProvider;
        this.userManager = userManager;
    }

    @Override
    public boolean isProvided() {
        return false;
    }

    @Nullable
    @Override
    public User get(CommandArgs commandArgs, List<? extends Annotation> list) throws ArgumentException, ProvisionException {
        return userManager.getUser(playerProvider.get(commandArgs, list));
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return playerProvider.getSuggestions(prefix);
    }

}
