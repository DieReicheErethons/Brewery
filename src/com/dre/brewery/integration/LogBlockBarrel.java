package com.dre.brewery.integration;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.compareInventories;
import static de.diddiz.util.BukkitUtils.compressInventory;
import static de.diddiz.util.BukkitUtils.rawData;

public class LogBlockBarrel {
	private static final List<LogBlockBarrel> opened = new ArrayList<LogBlockBarrel>();
	public static Consumer consumer = LogBlock.getInstance().getConsumer();

	private HumanEntity player;
	private ItemStack[] items;
	private Location loc;

	public LogBlockBarrel(HumanEntity player, ItemStack[] items, Location spigotLoc) {
		this.player = player;
		this.items = items;
		this.loc = spigotLoc;
		opened.add(this);
	}

	private void compareInv(final ItemStack[] after) {
		if (consumer == null) {
			return;
		}
		final ItemStack[] diff = compareInventories(items, after);
		for (final ItemStack item : diff) {
			consumer.queueChestAccess(player.getName(), loc, loc.getWorld().getBlockTypeIdAt(loc), (short) item.getTypeId(), (short) item.getAmount(), rawData(item));
		}
	}

	public static LogBlockBarrel get(HumanEntity player) {
		for (LogBlockBarrel open : opened) {
			if (open.player.equals(player)) {
				return open;
			}
		}
		return null;
	}

	public static void openBarrel(HumanEntity player, Inventory inv, Location spigotLoc) {
		if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) return;
		new LogBlockBarrel(player, compressInventory(inv.getContents()), spigotLoc);
	}

	public static void closeBarrel(HumanEntity player, Inventory inv) {
		if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) return;
		LogBlockBarrel open = get(player);
		if (open != null) {
			open.compareInv(compressInventory(inv.getContents()));
			opened.remove(open);
		}
	}

	public static void breakBarrel(String playerName, ItemStack[] contents, Location spigotLoc) {
		if (consumer == null) {
			return;
		}
		if (!isLogging(spigotLoc.getWorld(), Logging.CHESTACCESS)) return;
		final ItemStack[] items = compressInventory(contents);
		for (final ItemStack item : items) {
			consumer.queueChestAccess(playerName, spigotLoc, spigotLoc.getWorld().getBlockTypeIdAt(spigotLoc), (short) item.getTypeId(), (short) (item.getAmount() * -1), rawData(item));
		}
	}

	public static void clear() {
		opened.clear();
	}
}
