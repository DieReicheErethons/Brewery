package com.dre.brewery.listeners;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabListener implements TabCompleter {

	private static final Map<String, List<String>> completions = new HashMap<>();
	private static final List<String> topCompletions = new ArrayList<>(2);

	static {
		completions.put("", Arrays.asList("info", "unlabel", "help"));
		topCompletions.add("brew");
		//topCompletions.add("brewery");
	}



	@Override
	public List<String> onTabComplete(CommandSender commandSender, Command cmd, String command, String[] args) {
		if (args.length == 0) {
			return topCompletions;
		} else if (args.length == 1) {
			for (Map.Entry<String, List<String>> entry : completions.entrySet()) {
				List<String> list = new ArrayList<>();
				for (String comp : entry.getValue()) {
					if (comp.startsWith(args[0])) {
						list.add(comp);
					}
				}
				if (list.isEmpty()) {
					return null;
				}
				return list;
			}
		}
		return null;
	}
}
