package com.minecraftly.core.bukkit.modules.readonlyworlds;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Keir on 02/06/2015.
 */
public class ModuleReadOnlyWorlds extends Module implements Listener {

    public static long SESSION_ID = 666;

    private World readOnlyWorld = null;
    private final LanguageValue langBreakWarning = new LanguageValue("&cThis world may not be modified, it is a read-only world.");

    public ModuleReadOnlyWorlds(MclyCoreBukkitPlugin plugin) {
        super("ReadOnlyWorlds", plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().getLanguageManager().register("module.readOnlyWorlds.breakWarning", langBreakWarning);
        registerListener(this);

        List<World> worldList = Bukkit.getWorlds();
        if (worldList.size() > 0) {
            readOnlyWorld = worldList.get(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getWorld() == readOnlyWorld) {
            langBreakWarning.send(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        if (e.getWorld() == readOnlyWorld) {
            readOnlyWorld = null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (e.isNewChunk() && e.getWorld() == readOnlyWorld) {
            e.getChunk().unload(false, false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();

        if (readOnlyWorld == null && world == Bukkit.getWorlds().get(0)) { // todo remove this when MinecraftlyCore loads after worlds
            readOnlyWorld = world;
        }

        if (world == readOnlyWorld) {
            String worldName = world.getName();

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
