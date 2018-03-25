package com.dre.brewery.integration;

import com.dre.brewery.LegacyUtil;
import com.dre.brewery.P;

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
	private static final List<LogBlockBarrel> opened = new ArrayList<>();
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
			if (P.use1_13) {
				consumer.queueChestAccess(player.getName(), loc, LegacyUtil.getBlockTypeIdAt(loc), (short) item.getType().getId(), (short) item.getAmount(), rawData(item));
			} else {
				// TODO: New method for LogBlock for 1.13+
			}
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
			if (P.use1_13) {
				consumer.queueChestAccess(playerName, spigotLoc, LegacyUtil.getBlockTypeIdAt(spigotLoc), (short) item.getType().getId(), (short) (item.getAmount() * -1), rawData(item));
			} else {
				// TODO: New method for LogBlock for 1.13+
			}
		}
	}

	public static void clear() {
		opened.clear();
	}
}
