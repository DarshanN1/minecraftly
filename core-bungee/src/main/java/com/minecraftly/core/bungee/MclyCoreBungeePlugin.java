package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.bungeecord.DefaultBungeeGateway;
import com.ikeirnez.pluginmessageframework.connection.ProxySide;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.TestPacket;
import com.sk89q.intake.Command;
import lc.vq.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin {

    private CommandManager commandManager;
    private ProxyGateway<ServerInfo> gateway;

    @Override
    public void onEnable() {
        commandManager = new CommandManager(this);

        commandManager.builder()
                .registerMethods(this);

        commandManager.build();

        gateway = new DefaultBungeeGateway("Test", ProxySide.SERVER, this);
        gateway.registerListener(this);
    }

    @PacketHandler
    public void onPacket(ProxiedPlayer proxiedPlayer, TestPacket testPacket) {
        System.out.println("Received packet from '" + proxiedPlayer.getName() + "', data: " + testPacket.getMessage());
    }

    @Command(aliases = "mclybungeetestcommand", desc = "A test command.")
    public void testCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent("Intake is working in the MinecraftlyCore Bungee plugin :D"));
    }

}
