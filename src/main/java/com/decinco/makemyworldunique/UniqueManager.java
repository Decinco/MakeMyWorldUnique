package com.decinco.makemyworldunique;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

import java.util.HashMap;
import java.util.Map;

public class UniqueManager {

    private static final String MINIATURE_WORLD_PREFIX = "mmwu.miniature";

    private static final String LINKED_MINIATURE_WORLD_INTERFIX = ".linked";

    /**
     * Helper function that simply makes an empty world with a bedrock platform (using the MultiVerse API)
     * @param name The name of the world (must be a valid Bukkit world name or bad things might happen)
     * @return The world created
     */
    @NotNull
    public static World createEmptyWorld(@NotNull String name) {
        // Create the world using Multiverse API
        UniqueMain.getMvWorldManager().createWorld(
                CreateWorldOptions
                        .worldName(name)                                   // world name
                        .environment(World.Environment.NORMAL)             // environment type
                        .seed(null)                               // world seed
                        .worldType(WorldType.NORMAL)                       // world type
                        .generateStructures(false)      // generate structures
                        .generator("MiniWorld2:empty")         // custom generators
                        .useSpawnAdjust(false)            // search for a safe spawn
        );

        // Get the created world
        World world = Bukkit.getWorld(name);
        assert world != null;

        // Put a 32x1x32 pad of bedrock to start out
        for (int x = -16; x < 16; x++) {
            for (int z = -16; z < 16; z++) {
                world.getBlockAt(x, 64, z).setType(Material.BEDROCK);
            }
        }

        return world;
    }

    /**
     * Keeps track of how many clones have been created for a given world.
     * <p>
     * The only problem this may have is that if too many clones for one
     * world are created the integer limit could be reached. Not like that
     * will likely happen though.
     */
    private static final Map<World, Integer> numberOfMiniatures = new HashMap<>();

    /**
     * Creates a "miniature" world <i>(a temporary clone of a world)</i>.
     * Cloned world name will be named using this format:
     * mmwu.miniature.[clone id]_[parent world name]</pre>
     *
     * @param world The world to clone
     * @return The clone
     */
    @NotNull
    public static World createMiniatureOf(@NotNull World world) {
        if (world.getName().startsWith(MINIATURE_WORLD_PREFIX)) {
            throw new IllegalArgumentException("Miniature worlds can't be cloned!");
        }

        // Get the id for this clone
        int cloneNum = numberOfMiniatures.getOrDefault(world, 0);

        // Increment numberOfClones
        numberOfMiniatures.put(world, cloneNum + 1);

        // Get the Multiverse-Loaded World corresponding to the given world
        LoadedMultiverseWorld loadedWorld = UniqueMain.getMvWorldManager().getLoadedWorld(world).get();

        // Get the name that will be used for this clone
        String cloneName = MINIATURE_WORLD_PREFIX + "." + cloneNum + "_" + world.getName();

        // Clone the world with Multiverse
        UniqueMain.getMvWorldManager().cloneWorld(
                CloneWorldOptions.fromTo(
                        loadedWorld,
                        cloneName
                )
        );

        // Get the clone we just made
        World clonedWorld = Bukkit.getWorld(cloneName);
        assert clonedWorld != null;

        // This world is going to be deleted when the server stops, no need to auto save
        clonedWorld.setAutoSave(false);

        // If world guard integration is on copy the regions from the original world to the clone
        if (UniqueMain.isWorldGuardIntegration()) {
            WorldGuardIntegration.copyRegions(world, clonedWorld);
        }

        return clonedWorld;
    }

    /**
     * Creates a "miniature" world linked to the main world <i>(a temporary clone of a world)</i>.
     * This allows the plugins to refer to a world by its regular name, but use the cloned version either way.
     * Cloned world name will be named using this format:
     * mmwu.miniature.linked.[parent world name]</pre>
     * Only 1 linked miniature may be created.
     *
     * @param world The world to clone
     * @return The clone
     */
    @NotNull
    public static World createLinkedMiniatureOf(@NotNull World world) {
        if (world.getName().startsWith(MINIATURE_WORLD_PREFIX)) {
            throw new IllegalArgumentException("Miniature worlds can't be cloned!");
        }

        // Limit linked worlds to 1
        if (getLinkedMiniature(world) == null) {
            throw new IllegalArgumentException("This world already has a miniature linked to it!");
        }

        // Get the Multiverse-Loaded World corresponding to the given world
        LoadedMultiverseWorld loadedWorld = UniqueMain.getMvWorldManager().getLoadedWorld(world).get();

        // Get the name that will be used for this clone
        String cloneName = MINIATURE_WORLD_PREFIX + "." + LINKED_MINIATURE_WORLD_INTERFIX + "." + world.getName();

        // Clone the world with Multiverse
        UniqueMain.getMvWorldManager().cloneWorld(
                CloneWorldOptions.fromTo(
                        loadedWorld,
                        cloneName
                )
        );

        // Get the clone we just made
        World clonedWorld = Bukkit.getWorld(cloneName);
        assert clonedWorld != null;

        // This world is going to be deleted when the server stops, no need to auto save
        clonedWorld.setAutoSave(false);

        // If world guard integration is on copy the regions from the original world to the clone
        if (UniqueMain.isWorldGuardIntegration()) {
            WorldGuardIntegration.copyRegions(world, clonedWorld);
        }

        return clonedWorld;
    }

    public static World getLinkedMiniature (@NotNull World world) {
        if (world.getName().startsWith(MINIATURE_WORLD_PREFIX)) {
            throw new IllegalArgumentException("Provided world is a miniature world!");
        }

        String linkedMiniatureName = MINIATURE_WORLD_PREFIX + "." + LINKED_MINIATURE_WORLD_INTERFIX + "." + world.getName();

        return Bukkit.getWorld(linkedMiniatureName);
    }

    /**
     * Deletes a miniature world using the Multiverse API
     * @param world The miniature world to delete
     * @throws IllegalArgumentException if the world is not a miniature world
     */
    public static void removeMiniature(@NotNull World world) {
        if (!world.getName().startsWith(MINIATURE_WORLD_PREFIX)) {
            throw new IllegalArgumentException("Provided world was not a miniature world!");
        }

        // Get the Multiverse-Loaded World corresponding to the world
        LoadedMultiverseWorld loadedWorld = UniqueMain.getMvWorldManager().getLoadedWorld(world).get();

        // Delete this world
        UniqueMain.getMvWorldManager().deleteWorld(DeleteWorldOptions.world(loadedWorld));
    }

    /**
     * Loops over every world on the server and deletes it if it is a miniature world
     */
    public static void cleanMiniatures() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(MINIATURE_WORLD_PREFIX)) {
                removeMiniature(world);
            }
        }
    }

}
