package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.utility.BUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        int page = 1;
        if (args.length > 1) {
            page = breweryPlugin.parseInt(args[1]);
        }

        ArrayList<String> commands = CommandUtil.getCommands(sender);

        if (page == 1) {
            breweryPlugin.msg(sender, "&6" + breweryPlugin.getDescription().getName() + " v" + breweryPlugin.getDescription().getVersion());
        }

        BUtil.list(sender, commands, page);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return null;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}


