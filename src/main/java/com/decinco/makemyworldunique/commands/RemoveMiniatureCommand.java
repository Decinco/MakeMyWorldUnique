package com.decinco.makemyworldunique.commands;

import com.decinco.makemyworldunique.UniqueManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RemoveMiniatureCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }

        // Get the world we want to remove
        World miniature = Bukkit.getWorld(args[0]);

        if (miniature == null) {
            sender.sendPlainMessage("could not find that world!");
            return false;
        }

        try {
            UniqueManager.removeMiniature(miniature);
        } catch (IllegalArgumentException ex) {
            sender.sendPlainMessage("Error: " + ex.getMessage());
        }

        return true;
    }

}
