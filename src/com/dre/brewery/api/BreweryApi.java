package com.dre.brewery.api;

import com.dre.brewery.BCauldron;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class BreweryApi {

/*
 * Convenience methods to get common objects or do common things
 */

	/*
	 * Remove any data that this Plugin may associate with the given Block
	 * Currently Cauldrons and Barrels (Cauldron, Wood, Woodstairs, Fence, Sign)
	 * Does not remove any actual Blocks
	 * Returns true if anything was removed
	 */
	public static boolean removeAny(Block block) {
		if (removeCauldron(block)) return true;
		return removeBarrel(block);
	}

	/*
	 * Like removeAny() but removes data as if the given player broke the Block
	 * Currently only makes a difference for Logging
	 */
	public static boolean removeAnyByPlayer(Block block, Player player) {
		if (removeCauldron(block)) return true;
		return removeBarrelByPlayer(block, player);
	}


	// # # # # #    Brew    # # # # #

	/*
	 * Get a Brew from an ItemStack
	 * Reads the Brew data from the saved data on the item
	 * Checks if item is actually a Brew
	 * Returns null if item is not a Brew
	 */
	@Nullable
	public static Brew getBrew(ItemStack item) {
		return Brew.get(item);
	}

	/*
	 * Get a Brew from an ItemMeta
	 * Reads the Brew data from the saved data in the Meta
	 * Checks if meta has a Brew saved
	 * Returns null if meta is not a Brew
	 */
	@Nullable
	public static Brew getBrew(ItemMeta meta) {
		return Brew.get(meta);
	}


	// # # # # #    Barrel    # # # # #

	/*
	 * Get a Barrel from a Block
	 * May be any Wood, Fence, Sign that is part of a Barrel
	 * Returns null if block is not part of a Barrel
	 */
	@Nullable
	public static Barrel getBarrel(Block block) {
		return Barrel.get(block);
	}

	/*
	 * Get the Inventory of a Block part of a Barrel
	 * May be any Wood, Fence or Sign that is part of a Barrel
	 * Returns null if block is not part of a Barrel
	 */
	@Nullable
	public static Inventory getBarrelInventory(Block block) {
		Barrel barrel = Barrel.get(block);
		if (barrel != null) {
			return barrel.getInventory();
		}
		return null;
	}

	/*
	 * Remove any Barrel that this Block may be Part of
	 * Returns true if a Barrel was removed
	 * Does not remove any actual Block
	 */
	public static boolean removeBarrel(Block block) { // TODO add dropItems flag
		return removeBarrelByPlayer(block, null);
	}

	/*
	 * Remove any Barrel that this Block may be Part of, as if broken by the Player
	 * Returns true if a Barrel was removed
	 * Does not remove any actual Block
	 */
	public static boolean removeBarrelByPlayer(Block block, Player player) {
		Barrel barrel = Barrel.get(block);
		if (barrel != null) {
			barrel.remove(block, player);
			return true;
		}
		return false;
	}


	// # # # # #    Cauldron    # # # # #

	/*
	 * Get a BCauldron from a Block
	 * Returns null if block is not a BCauldron
	 */
	@Nullable
	public static BCauldron getCauldron(Block block) {
		return BCauldron.get(block);
	}

	/*
	 * Remove any data associated with a Cauldron at that given Block
	 * Returns true if a Cauldron was removed
	 * Does not actually remove the Block
	 */
	public static boolean removeCauldron(Block block) {
		return BCauldron.remove(block);
	}


	// # # # # #    Recipe    # # # # #

	/*
	 * Get a BRecipe by its name
	 * The name is the middle one of the three if three are set in the config
	 * Returns null if recipe with that name does not exist
	 */
	@Nullable
	public static BRecipe getRecipe(String name) {
		return BRecipe.get(name);
	}

	/*
	 * Add a new recipe
	 * Not Implemented yet
	 */
	public static boolean addRecipe(BRecipe recipe) {
		throw new NotImplementedException(); // TODO implement
	}
}
