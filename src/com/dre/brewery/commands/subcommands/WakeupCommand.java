package com.dre.brewery.commands.subcommands;

import com.dre.brewery.P;
import com.dre.brewery.Wakeup;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WakeupCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
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
                page = p.parseInt(args[2]);
            }
            if (args.length > 3) {
                world = args[3];
            }
            Wakeup.list(sender, page, world);

        } else if (args[1].equalsIgnoreCase("remove")){

            if (args.length > 2) {
                int id = p.parseInt(args[2]);
                Wakeup.remove(sender, id);
            } else {
                p.msg(sender, p.languageReader.get("Etc_Usage"));
                p.msg(sender, p.languageReader.get("Help_WakeupRemove"));
            }

        } else if (args[1].equalsIgnoreCase("check")){

            int id = -1;
            if (args.length > 2) {
                id = p.parseInt(args[2]);
                if (id < 0) {
                    id = 0;
                }
            }
            Wakeup.check(sender, id, id == -1);

        } else if (args[1].equalsIgnoreCase("cancel")){

            Wakeup.cancel(sender);

        } else {

            p.msg(sender, p.languageReader.get("Error_UnknownCommand"));
            p.msg(sender, p.languageReader.get("Error_ShowHelp"));

        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
