package com.dre.brewery.commands.subcommands;

import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements SubCommand {

    private final P p;

    public ReloadCommand(P p) {
        this.p = p;
    }

    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        p.reload(sender);
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.reload";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
