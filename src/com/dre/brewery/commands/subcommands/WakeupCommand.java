package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.Wakeup;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WakeupCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            CommandUtil.cmdHelp(sender, args);
            return;
        }

        if (args[1].equalsIgnoreCase("add")) {

            Wakeup.set(sender);

        } else if (args[1].equalsIgnoreCase("list")){

            int page = 1;
            String world = null;
            if (args.length > 2) {
                page = breweryPlugin.parseInt(args[2]);
            }
            if (args.length > 3) {
                world = args[3];
            }
            Wakeup.list(sender, page, world);

        } else if (args[1].equalsIgnoreCase("remove")){

            if (args.length > 2) {
                int id = breweryPlugin.parseInt(args[2]);
                Wakeup.remove(sender, id);
            } else {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Etc_Usage"));
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Help_WakeupRemove"));
            }

        } else if (args[1].equalsIgnoreCase("check")){

            int id = -1;
            if (args.length > 2) {
                id = breweryPlugin.parseInt(args[2]);
                if (id < 0) {
                    id = 0;
                }
            }
            Wakeup.check(sender, id, id == -1);

        } else if (args[1].equalsIgnoreCase("cancel")){

            Wakeup.cancel(sender);

        } else {

            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_UnknownCommand"));
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_ShowHelp"));

        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.wakeup";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
