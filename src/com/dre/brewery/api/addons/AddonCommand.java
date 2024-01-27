package com.dre.brewery.api.addons;

import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AddonCommand extends BukkitCommand {
	protected AddonCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
		super(name, description, usageMessage, aliases);
	}
}
