package com.dre.brewery.listeners;


import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.PermissionUtil;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dre.brewery.utility.PermissionUtil.BPermission.DRINK;
import static com.dre.brewery.utility.PermissionUtil.BPermission.WAKEUP;

public class TabListener implements TabCompleter {

	private static final String[] COMPLETIONS = {"unLabel", "help"};
	private static final String[] EXT_COMPLETIONS = {"info", "create", "reload", "drink", "itemName", "seal", "static", "puke", "wakeup"};

	private static final String[] QUALITY = {"1", "10"};

	private static Set<Tuple<String, String>> mainSet;
	private static Set<Tuple<String, String>> altSet;


	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String command, String[] args) {
		if (args.length < 1) {
			return null;
		}
		if (args.length == 1) {

			List<String> commands;
			if (PermissionUtil.noExtendedPermissions(sender)) {
				commands = filterWithInput(COMPLETIONS, args[0]);
			} else {
				Stream<String> filteredCompletionsStream = Arrays.stream(COMPLETIONS)
					.filter(s -> s.startsWith(args[0]));
				Stream<String> filteredExtendedCompletionsStream = Arrays.stream(EXT_COMPLETIONS)
					.filter(s -> s.startsWith(args[0]));

				commands = Stream.concat(filteredCompletionsStream, filteredExtendedCompletionsStream)
					.collect(Collectors.toList());
			}
			if (commands.isEmpty()) {
				return null; // Player List
			} else {
				return commands;
			}

		} else if (args.length == 2) {

			if (args[0].equalsIgnoreCase("wakeup")) {
				if (WAKEUP.checkCached(sender)) {
					return tabWakeup(args[1]);
				}
			}

		}

		if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("drink")) {
			if (DRINK.checkCached(sender)) {
				return tabCreateAndDrink(args);
			}
		}

		return null;
	}


	private static final String[] WAK = {"list", "add", "check", "remove", "cancel"};
	public List<String> tabWakeup(String input) {
		return filterWithInput(WAK, input);
	}

	public List<String> tabCreateAndDrink(String[] args) {
		if (args.length == 2) {

			if (mainSet == null) {
				mainSet = new HashSet<>();
				altSet = new HashSet<>();
				for (BRecipe recipe : BRecipe.getAllRecipes()) {
					mainSet.addAll(createLookupFromName(recipe.getName(5)));

					Set<String> altNames = new HashSet<>(3);
					altNames.add(recipe.getName(1));
					altNames.add(recipe.getName(10));
					if (recipe.getOptionalID().isPresent()) {
						altNames.add(recipe.getOptionalID().get());
					}

					for (String altName : altNames) {
						altSet.addAll(createLookupFromName(altName));
					}

				}
			}

			final String input = args[1].toLowerCase();

			List<String> options = mainSet.stream()
				.filter(s -> s.a().startsWith(input))
				.map(Tuple::second)
				.collect(Collectors.toList());
			if (options.isEmpty()) {
				options = altSet.stream()
					.filter(s -> s.a().startsWith(input))
					.map(Tuple::second)
					.collect(Collectors.toList());
			}
			return options;
		} else {
			if (args[args.length - 2].matches("\\d")) {
				// Player list
				return null;
			} else {
				return filterWithInput(QUALITY, args[args.length - 1]);
			}
		}

	}

	private static List<Tuple<String, String>> createLookupFromName(final String name) {
		return Arrays.stream(name.split(" "))
			.map(word -> new Tuple<>(word.toLowerCase(), name))
			.collect(Collectors.toList());
	}

	public static List<String> filterWithInput(String[] options, String input) {
		return Arrays.stream(options)
			.filter(s -> s.startsWith(input))
			.collect(Collectors.toList());
	}

	public static List<String> filterWithInput(Collection<String> options, String input) {
		return options.stream()
			.filter(s -> s.startsWith(input))
			.collect(Collectors.toList());
	}

	public static void reload() {
		mainSet = null;
		altSet = null;
	}
}
