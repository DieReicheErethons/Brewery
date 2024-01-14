package com.dre.brewery.commands;

import com.dre.brewery.P;
import com.dre.brewery.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements TabExecutor {

    private static final P BREWERY_PLUGIN = P.p;

    private static final Map<String, SubCommand> subCommands = new HashMap<>();

    public CommandManager() {
        subCommands.put("help" , new HelpCommand());
        subCommands.put("reload", new ReloadCommand(BREWERY_PLUGIN));
        subCommands.put("wakeup", new WakeupCommand());
        subCommands.put("itemName", new ItemName());
        subCommands.put("create", new CreateCommand(BREWERY_PLUGIN));
        subCommands.put("info", new InfoCommand(BREWERY_PLUGIN));
        subCommands.put("seal", new SealCommand());
        subCommands.put("copy", new CopyCommand(BREWERY_PLUGIN));
        subCommands.put("delete", new DeleteCommand(BREWERY_PLUGIN));
        subCommands.put("static", new StaticCommand());
        subCommands.put("unLabel", new UnLabelCommand());
        subCommands.put("debuginfo", new DebugInfoCommand(BREWERY_PLUGIN));
        subCommands.put("showstats", new ShowStatsCommand());
        subCommands.put("puke", new PukeCommand());
        subCommands.put("drink", new DrinkCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 1) {
            CommandUtil.cmdHelp(sender, args);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0]); //
        if (subCommand == null) {
            CommandUtil.cmdHelp(sender, args);
            return true;
        }
        boolean playerOnly = subCommand.playerOnly();
        String permission = subCommand.permission();

        if (playerOnly && !(sender instanceof Player)) {
            BREWERY_PLUGIN.msg(sender, BREWERY_PLUGIN.languageReader.get("Error_NotPlayer"));
            return true;
        } else if (permission != null && !sender.hasPermission(permission)) {
            BREWERY_PLUGIN.msg(sender, BREWERY_PLUGIN.languageReader.get("Error_NoPermission"));
            return true;
        }

        subCommand.execute(BREWERY_PLUGIN, sender, s, args);
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            List<String> commands = new ArrayList<>();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                String perm = entry.getValue().permission();
                if (perm != null && commandSender.hasPermission(perm)) {
                    commands.add(entry.getKey());
                }
            }
            return commands;
        }

        SubCommand subCommand = subCommands.get(strings[0].toLowerCase());
        if (subCommand != null) {
            return subCommand.tabComplete(BREWERY_PLUGIN, commandSender, s, strings);
        }
        return null;
    }



}
