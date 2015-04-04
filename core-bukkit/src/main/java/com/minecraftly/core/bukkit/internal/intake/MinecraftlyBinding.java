package com.minecraftly.core.bukkit.internal.intake;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.user.User;
import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Created by Keir on 13/03/2015.
 */
public class MinecraftlyBinding extends BindingHelper {

    private MclyCoreBukkitPlugin plugin;

    public MinecraftlyBinding(MclyCoreBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @BindingMatch(type = User.class, behavior = BindingBehavior.PROVIDES)
    public User provideUser(ArgumentStack context) throws ParameterException {
        Player player = context.getContext().getLocals().get(Player.class);

        if (player == null) {
            throw new ParameterException("User couldn't be retrieved as there was no Player defined.");
        }

        return plugin.getUserManager().getUser(player);
    }

    @BindingMatch(type = World.class, behavior = BindingBehavior.CONSUMES)
    public World provideWorld(ArgumentStack stack) throws ParameterException {
        World world = Bukkit.getWorld(stack.next());

        if (world == null) {
            throw new ParameterException("World not found.");
        }

        return world;
    }

    @BindingMatch(type = MinecraftlyCore.class, behavior = BindingBehavior.PROVIDES)
    public MinecraftlyCore provideMinecraftlyCore(ArgumentStack stack) throws ParameterException {
        return plugin;
    }

    @BindingMatch(type = LanguageManager.class, behavior = BindingBehavior.PROVIDES)
    public LanguageManager provideLanguageManager(ArgumentStack stack) throws ParameterException {
        return plugin.getLanguageManager();
    }

    @BindingMatch(type = DatabaseManager.class, behavior = BindingBehavior.PROVIDES)
    public DatabaseManager provideDatabaseManager(ArgumentStack stack) throws ParameterException {
        return plugin.getDatabaseManager();
    }

}
