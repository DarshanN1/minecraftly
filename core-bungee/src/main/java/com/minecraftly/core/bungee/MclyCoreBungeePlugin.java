package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGateway;
import com.ikeirnez.pluginmessageframework.bungeecord.DefaultBungeeGateway;
import com.ikeirnez.pluginmessageframework.connection.ProxySide;
import com.minecraftly.core.MinecraftlyCommon;
import com.sk89q.intake.Command;
import lc.vq.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin implements MinecraftyBungeeCore {

    private CommandManager commandManager;
    private BungeeGateway gateway;

    @Override
    public void onEnable() {
        commandManager = new CommandManager(this);
        commandManager.builder().registerMethods(this);
        commandManager.build();

        gateway = new DefaultBungeeGateway(MinecraftlyCommon.GATEWAY_CHANNEL, ProxySide.SERVER, this);
    }

    @Override
    public BungeeGateway getGateway() {
        return gateway;
    }

    @Command(aliases = "mclybungeetestcommand", desc = "A test command.")
    public void testCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent("Intake is working in the MinecraftlyCore Bungee plugin :D"));
    }

}
