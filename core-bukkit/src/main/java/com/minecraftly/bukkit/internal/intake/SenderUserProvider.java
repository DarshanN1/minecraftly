package com.minecraftly.bukkit.internal.intake;

import com.google.common.collect.ImmutableList;
import com.minecraftly.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.bukkit.user.User;
import com.minecraftly.bukkit.user.UserManager;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import net.ellune.exhaust.bukkit.provider.core.CommandSenderProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Created by Keir on 08/07/2015.
 */
public class SenderUserProvider implements Provider<User> {

    public static final SenderUserProvider INSTANCE = new SenderUserProvider(CommandSenderProvider.INSTANCE, MclyCoreBukkitPlugin.getInstance().getUserManager()); // todo eww dis static

    private final CommandSenderProvider commandSenderProvider;
    private final UserManager userManager;

    public SenderUserProvider(CommandSenderProvider commandSenderProvider, UserManager userManager) {
        this.commandSenderProvider = commandSenderProvider;
        this.userManager = userManager;
    }

    @Override
    public boolean isProvided() {
        return true;
    }

    @Nullable
    @Override
    public User get(CommandArgs commandArgs, List<? extends Annotation> list) throws ArgumentException, ProvisionException {
        CommandSender sender = commandSenderProvider.get(commandArgs, list);

        if (sender == null) {
            throw new ProvisionException("Sender is not set on the namespace.");
        }

        if (!(sender instanceof Player)) {
            throw new ProvisionException("Sender is not a player.");
        }

        return userManager.getUser((Player) sender);
    }

    @Override
    public List<String> getSuggestions(String s) {
        return ImmutableList.of();
    }

}
