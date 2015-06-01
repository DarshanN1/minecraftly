package com.minecraftly.core.bukkit.language;

import com.minecraftly.core.ContentOwner;
import com.minecraftly.core.bukkit.config.DataValue;
import org.bukkit.command.CommandSender;

/**
 * A {@link DataValue} where users are forced to use the {@link String} type.
 * This reduces code for language value handling.
 */
public class LanguageValue extends DataValue<String> {

    public LanguageValue(ContentOwner contentOwner, String def) {
        super(contentOwner, def, String.class);
    }

    public void send(CommandSender commandSender) {
        commandSender.sendMessage(getValue());
    }

    // make below methods visible to language manager

    @Override
    protected Object getHandler() {
        return super.getHandler();
    }

    @Override
    protected void setHandler(Object handler) {
        super.setHandler(handler);
    }
}
