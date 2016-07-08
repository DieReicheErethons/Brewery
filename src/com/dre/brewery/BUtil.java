package com.dre.brewery;

import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class BUtil {

	// Returns the Index of a String from the list that contains this substring
	public static int indexOfSubstring(List<String> list, String substring) {
		if (list.isEmpty()) return -1;
		for (int index = 0, size = list.size(); index < size; index++) {
			String string = list.get(index);
			if (string.contains(substring)) {
				return index;
			}
		}
		return -1;
	}

	// Returns the index of a String from the list that starts with 'lineStart', returns -1 if not found;
	public static int indexOfStart(List<String> list, String lineStart) {
		for (int i = 0, size = list.size(); i < size; i++) {
			if (list.get(i).startsWith(lineStart)) {
				return i;
			}
		}
		return -1;
	}

	/*
	   ---- Barrel ----
	*/

	// Returns true if the Block can be destroyed by the Player or something else (null)
	public static boolean blockDestroy(Block block, Player player, BarrelDestroyEvent.Reason reason) {
		switch (block.getType()) {
			case CAULDRON:
				// will only remove when existing
				BCauldron.remove(block);
				return true;
			case FENCE:
			case NETHER_FENCE:
			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case DARK_OAK_FENCE:
			case IRON_FENCE:
			case JUNGLE_FENCE:
			case SPRUCE_FENCE:
				// remove barrel and throw potions on the ground
				Barrel barrel = Barrel.getBySpigot(block);
				if (barrel != null) {
					if (barrel.hasPermsDestroy(player, block, reason)) {
						barrel.remove(null, player);
						return true;
					} else {
						return false;
					}
				}
				return true;
			case SIGN_POST:
			case WALL_SIGN:
				// remove small Barrels
				Barrel barrel2 = Barrel.getBySpigot(block);
				if (barrel2 != null) {
					if (!barrel2.isLarge()) {
						if (barrel2.hasPermsDestroy(player, block, reason)) {
							barrel2.remove(null, player);
							return true;
						} else {
							return false;
						}
					} else {
						barrel2.destroySign();
					}
				}
				return true;
			case WOOD:
			case WOOD_STAIRS:
			case BIRCH_WOOD_STAIRS:
			case JUNGLE_WOOD_STAIRS:
			case SPRUCE_WOOD_STAIRS:
			case ACACIA_STAIRS:
			case DARK_OAK_STAIRS:
				Barrel barrel3 = Barrel.getByWood(block);
				if (barrel3 != null) {
					if (barrel3.hasPermsDestroy(player, block, reason)) {
						barrel3.remove(block, player);
					} else {
						return false;
					}
				}
			default:
				break;
		}
		return true;
	}
}
