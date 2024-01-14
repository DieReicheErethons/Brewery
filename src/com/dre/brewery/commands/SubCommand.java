package com.dre.brewery.commands;

import com.dre.brewery.P;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    /**
     * Executes the subcommand's code
     * @param p Instance of the Brewery plugin
     * @param sender The CommandSender that executed the command
     * @param label The command label (alias)
     * @param args The command arguments
     */
    void execute(P p, CommandSender sender, String label, String[] args);

    /**
     * Returns a list of possible tab completions for the subcommand
     * @param p Instance of the Brewery plugin
     * @param sender The CommandSender that executed the command
     * @param label The command label (alias)
     * @param args The command arguments
     * @return A list of possible tab completions for the subcommand
     */
    List<String> tabComplete(P p, CommandSender sender, String label, String[] args);

    /**
     * @return the subcommand's required permission node
     */
    String permission();

    /**
     * @return if the command can only be executed by a player
     */
    boolean playerOnly();
}
