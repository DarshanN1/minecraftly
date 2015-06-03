package com.minecraftly.modules.readonlyworlds;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.config.ConfigManager;
import com.minecraftly.core.bukkit.config.DataValue;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Keir on 02/06/2015.
 */
public class ReadOnlyWorldsModule extends Module implements Listener {

    public static long SESSION_ID = 666;

    // todo remove "? extends", this was to workaround a bug in the Java 8 compiler
    // https://bugs.openjdk.java.net/browse/JDK-8044053
    private final DataValue<? extends List<String>> readOnlyWorlds = new DataValue<>(this, Collections.singletonList("world"), List.class);
    private final DataValue<Integer> breakWarningDelay = new DataValue<>(this, 60 * 5, Integer.class);

    private final LanguageValue langBreakWarning = new LanguageValue(this, "&cThis world may not be modified, it is a read-only world.");

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        ConfigManager configManager = plugin.getConfigManager();
        String parentKey = "module.read-only-worlds";
        configManager.register(parentKey + ".worlds", readOnlyWorlds);
        configManager.register(parentKey + ".blockBreakWarningDelay", breakWarningDelay);

        plugin.getLanguageManager().register(parentKey + ".breakWarning", langBreakWarning);

        registerListener(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        langBreakWarning.send(e.getPlayer());
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldSave(WorldSaveEvent e) {
        String worldName = e.getWorld().getName();

        if (readOnlyWorlds.getValue().contains(worldName)) {
            getLogger().severe("World saved (this shouldn't have happened): " + worldName + ".");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();
        String worldName = world.getName();

        if (readOnlyWorlds.getValue().contains(worldName)) {
            try {
                // look away, deez hax will hurt your eyes
                Method getHandleMethod = world.getClass().getDeclaredMethod("getHandle");
                getHandleMethod.setAccessible(true);
                Object worldServer = getHandleMethod.invoke(world);
                Class<?> worldServerClass = worldServer.getClass();

                Field savingDisabledField = worldServerClass.getDeclaredField("savingDisabled");
                savingDisabledField.set(worldServer, true);

                Method getDataManagerMethod = worldServerClass.getMethod("getDataManager");
                Object dataManager = getDataManagerMethod.invoke(worldServer);
                Class<?> dataManagerClass = dataManager.getClass();
                Class<?> worldNbtStorageClass = Class.forName(getPackageNameOfClass(worldServerClass) + ".WorldNBTStorage");

                if (!worldNbtStorageClass.isAssignableFrom(dataManagerClass)) {
                    getLogger().severe("Data manager (" + dataManagerClass.getName() + "), doesn't extend WorldNBTStorage, don't know how to handle.");
                    return;
                }

                Field sessionIdField = worldNbtStorageClass.getDeclaredField("sessionId");
                sessionIdField.setAccessible(true);
                removeFinalModifier(sessionIdField);
                sessionIdField.set(dataManager, SESSION_ID);
            } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e1) {
                getLogger().log(Level.SEVERE, "Exception whilst attempting to make world read-only: " + worldName + ".", e1);
            }

            File sessionLock = new File(Bukkit.getWorldContainer(), worldName + "/session.lock");

            try {
                sessionLock.createNewFile();
                try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(sessionLock))) {
                    dataOutputStream.writeLong(SESSION_ID);
                }
            } catch (IOException e1) {
                getLogger().log(Level.SEVERE, "Error writing new session id to session.lock.", e1);
            }
        }
    }

    public static void removeFinalModifier(Field field) throws NoSuchFieldException, IllegalAccessException {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    public static String getPackageNameOfClass(Class<?> clazz) {
        String className = clazz.getName();
        return className.substring(0, className.lastIndexOf('.'));
    }

}
