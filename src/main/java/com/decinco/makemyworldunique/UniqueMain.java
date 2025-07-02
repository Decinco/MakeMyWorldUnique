package com.decinco.makemyworldunique;

import com.decinco.makemyworldunique.commands.CreateEmptyWorldCommand;
import com.decinco.makemyworldunique.commands.CreateMiniatureCommand;
import com.decinco.makemyworldunique.commands.RemoveMiniatureCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;

public final class UniqueMain extends JavaPlugin implements Listener {

    private static MultiverseCoreApi mvApi;

    @NotNull
    public static WorldManager getMvWorldManager() {
        return mvApi.getWorldManager();
    }

    private static boolean worldGuardIntegration;

    public static boolean isWorldGuardIntegration() {
        return worldGuardIntegration;
    }

    private static UniqueMain instance = null;

    public static UniqueMain getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.getLogger().info("hello world!");

        // Instance should only ever get set once
        assert instance == null;
        instance = this;

        // If a config.yml does not exist, create the default one (src/main/resources/config.yml)
        this.saveDefaultConfig();

        this.getServer().getPluginManager().registerEvents(this, this);

        // Get Multiverse Core plugin object, so we can access the MultiVerse plugin API
        MultiverseCore multiverseCore = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        assert multiverseCore != null;

        mvWorldManager = multiverseCore.getMVWorldManager();

        this.getCommand("CreateVoidWorld").setExecutor(new CreateEmptyWorldCommand());
        this.getCommand("CreateMiniature").setExecutor(new CreateMiniatureCommand());
        this.getCommand("RemoveMiniature").setExecutor(new RemoveMiniatureCommand());
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent e) {
        // This event is called after everything is done loading
        // We can check for WorldGuard here

        // Get if we should load WorldGuard from the config
        boolean shouldLoadWG = this.getConfig().getBoolean("WorldGuardIntegration");

        if (shouldLoadWG) {
            // Try to get WorldGuard
            try {
                WorldGuardIntegration.instantiateContainer();
                worldGuardIntegration = true;
                this.getLogger().info("world guard integration on");
            } catch (NoClassDefFoundError ex) {
                // WorldGuard isn't being used
                worldGuardIntegration = false;
                this.getLogger().info("world guard was not found! disabling world guard integration");
            }
        } else {
            worldGuardIntegration = false;
            this.getLogger().info("world guard integration off");
        }
    }

    @Override
    public void onDisable() {
        // Remove all miniatures created before MiniWorld2 gets turned off
        UniqueManager.cleanMiniatures();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        // This method is for the CreateVoidWorld command
        // It just lets other plugins access the void world gen via "MiniWorld2:empty"

        if (id != null && id.equals("empty")) {
            Location spawnLocation = new Location(null, 0, 65, 0);
            return new EmptyChunkGenerator(spawnLocation);
        }

        return null;
    }

}
