package com.dre.brewery.api;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BPlayer;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.BCauldronRecipe;
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

import java.util.List;

/**
 * Convenience methods to get common objects or do common things.
 * <p>Contains shortcuts and collects of some of the main functions of this Plugin
 *
 * <p>Next to this there are lots of public Methods in many Objects
 * like Brew, Barrel, BCauldron, BRecipe, etc
 * <p>In the api package, you can also find custom Events.
 */
public class BreweryApi {

	/**
	 * Get the Current Version of the Brewery API.
	 * <p>Higher numbers mean newer API, but it doesn't necessarily mean that something has changed, may be additions only
	 */
	public static int getApiVersion() {
		return 2;
	}

	/**
	 * Remove any data that this Plugin may associate with the given Block.
	 * <p>Currently Cauldrons and Barrels (Cauldron, Wood, Woodstairs, Fence, Sign)
	 * <p>Does not remove any actual Blocks
	 * <p>Returns true if anything was removed
	 *
	 * @return true if anything was removed
	 */
	public static boolean removeAny(Block block) {
		if (removeCauldron(block)) return true;
		return removeBarrel(block, true);
	}

	/**
	 * <p>Like removeAny() but removes data as if the given player broke the Block.
	 * <p>Currently only makes a difference for Logging
	 */
	public static boolean removeAnyByPlayer(Block block, Player player) {
		if (removeCauldron(block)) return true;
		return removeBarrelByPlayer(block, player, true);
	}


	// # # # # # #        # # # # # #
	// # # # # #    Player    # # # # #
	// # # # # # #        # # # # # #

	/**
	 * Get the BPlayer for the given Player, containing drunkeness and hangover data.
	 */
	public static BPlayer getBPlayer(Player player) {
		return BPlayer.get(player);
	}

	/**
	 * Set the Players drunkeness state.
	 *
	 * @param player The Player to set the drunkeness on
	 * @param drunkeness The amount of drunkeness 0-100 to apply to the player
	 * @param quality The Quality 1-10 the drunkeness of the player should have.
	 *                <br>zero Quality keeps the players current quality
	 */
	public static void setPlayerDrunk(Player player, int drunkeness, int quality) {
		if (drunkeness < 0) {
			throw new IllegalArgumentException("Drunkeness can not be <0");
		}
		if (quality > 10) {
			throw new IllegalArgumentException("Quality can not be >10");
		}
		BPlayer bPlayer = BPlayer.get(player);
		if (bPlayer == null && player != null) {
			if (drunkeness == 0) {
				return;
			}
			bPlayer = BPlayer.addPlayer(player);
		}
		if (bPlayer == null) {
			return;
		}

		if (drunkeness == 0) {
			bPlayer.remove();
		} else {
			bPlayer.setData(drunkeness, quality);
		}

		if (drunkeness > 100) {
			if (player != null) {
				bPlayer.drinkCap(player);
			} else {
				if (!BConfig.overdrinkKick) {
					bPlayer.setData(100, 0);
				}
			}
		}
	}


	// # # # # # #        # # # # # #
	// # # # # #    Brew    # # # # #
	// # # # # # #        # # # # # #

	/**
	 * Get a Brew from an ItemStack.
	 * <p>Reads the Brew data from the saved data on the item
	 * <p>Checks if item is actually a Brew
	 * <p>Returns null if item is not a Brew
	 */
	@Nullable
	public static Brew getBrew(ItemStack item) {
		return Brew.get(item);
	}

	/**
	 * Get a Brew from an ItemMeta.
	 * <p>Reads the Brew data from the saved data in the Meta
	 * <p>Checks if meta has a Brew saved
	 * <p>Returns null if meta is not a Brew
	 */
	@Nullable
	public static Brew getBrew(ItemMeta meta) {
		return Brew.get(meta);
	}

	/**
	 * Performant way to check if an item is a brew.
	 * <p>Does not give any guarantees that getBrew() will return notnull for this item, i.e. if it is a brew but couldn't be loaded
	 */
	public static boolean isBrew(ItemStack item) {
		return Brew.isBrew(item);
	}

	/**
	 * Create a Brew from the given Recipe.
	 *
	 * @param recipe The Recipe to create a brew from
	 * @return The Brew that was created. Can use brew.createItem() to get an ItemStack
	 */
	public static Brew createBrew(BRecipe recipe, int quality) {
		return recipe.createBrew(quality);
	}


	// # # # # # #          # # # # # #
	// # # # # #    Barrel    # # # # #
	// # # # # # #          # # # # # #

	/**
	 * Get a Barrel from a Block.
	 * <p>May be any Wood, Fence, Sign that is part of a Barrel
	 * <p>Returns null if block is not part of a Barrel
	 */
	@Nullable
	public static Barrel getBarrel(Block block) {
		return Barrel.get(block);
	}

	/**
	 * Get the Inventory of a Block part of a Barrel.
	 * <p>May be any Wood, Fence or Sign that is part of a Barrel
	 * <p>Returns null if block is not part of a Barrel
	 */
	@Nullable
	public static Inventory getBarrelInventory(Block block) {
		Barrel barrel = Barrel.get(block);
		if (barrel != null) {
			return barrel.getInventory();
		}
		return null;
	}

	/**
	 * Remove any Barrel that this Block may be Part of.
	 * Does not remove any actual Block
	 *
	 * @param block The Block thats part of the barrel, potions will drop there
	 * @param dropItems If the items in the barrels inventory should drop to the ground
	 * @return True if a Barrel was removed
	 */
	public static boolean removeBarrel(Block block, boolean dropItems) {
		return removeBarrelByPlayer(block, null, dropItems);
	}

	/**
	 * Remove any Barrel that this Block may be Part of, as if broken by the Player.
	 * Does not remove any actual Block from the World
	 *
	 * @param block The Block thats part of the barrel, potions will drop there
	 * @param player The Player that broke the Block
	 * @param dropItems If the items in the barrels inventory should drop to the ground
	 * @return True if a Barrel was removed
	 */
	public static boolean removeBarrelByPlayer(Block block, Player player, boolean dropItems) {
		Barrel barrel = Barrel.get(block);
		if (barrel != null) {
			barrel.remove(block, player, dropItems);
			return true;
		}
		return false;
	}

	// # # # # # #            # # # # # #
	// # # # # #    Cauldron    # # # # #
	// # # # # # #            # # # # # #

	/**
	 * Get a BCauldron from a Block.
	 * <p>Returns null if block is not a BCauldron
	 */
	@Nullable
	public static BCauldron getCauldron(Block block) {
		return BCauldron.get(block);
	}

	/**
	 * Remove any data associated with a Cauldron at that given Block.
	 * <p>Returns true if a Cauldron was removed
	 * <p>Does not remove the Block from the World
	 */
	public static boolean removeCauldron(Block block) {
		return BCauldron.remove(block);
	}


	// # # # # # #          # # # # # #
	// # # # # #    Recipe    # # # # #
	// # # # # # #          # # # # # #

	/**
	 * Get a BRecipe by its name.
	 * <p>The name is the middle one of the three if three are set in the config
	 * <p>Returns null if recipe with that name does not exist
	 */
	@Nullable
	public static BRecipe getRecipe(String name) {
		return BRecipe.get(name);
	}

	/**
	 * Add a New Recipe.
	 * <p>Brews can be made out of this Recipe.
	 * <p>The recipe can be changed or removed later.
	 *
	 * @param recipe The Recipe to add
	 * @param saveForever Not Implemented yet.
	 *                    <br>If the recipe should be saved forever, even after the Server restarts
	 *                    <br>If True: Recipe will be saved until removed manually
	 *                    <br>If False: Recipe will be removed when the Server restarts, existing potions using
	 *                    <br>this Recipe will become bad after continued aging, if the recipe is not added again.
	 */
	public static void addRecipe(BRecipe recipe, boolean saveForever) {
		//recipe.setSaveInData(saveForever);
		if (saveForever) {
			throw new NotImplementedException("SaveForever is not implemented yet");
		}
		BRecipe.getAddedRecipes().add(recipe);
		recipe.updateAcceptedLists();
	}

	/**
	 * Removes a Recipe from the List of all Recipes.
	 * <p>This can also remove Recipes that were loaded from config, though these will be readded when reloading the config
	 *
	 * @param name The name of the recipe to remove
	 * @return The Recipe that was removed, null if none was removed
	 */
	@Nullable
	public static BRecipe removeRecipe(String name) {
		List<BRecipe> recipes = BRecipe.getAllRecipes();
		for (int i = 0; i < recipes.size(); i++) {
			if (recipes.get(i).getRecipeName().equalsIgnoreCase(name)) {
				BRecipe remove = recipes.remove(i);
				if (i < BRecipe.numConfigRecipes) {
					// We removed one of the Config Recipes
					BRecipe.numConfigRecipes--;
				}
				return remove;
			}
		}
		return null;
	}

	/**
	 * Create a New Recipe with a Recipe Builder.
	 *
	 * @param recipeNames Either 1 or 3 names. Sets the Name for Quality (Bad, Normal, Good)
	 * @return A Recipe Builder
	 */
	public static BRecipe.Builder recipeBuilder(String... recipeNames) {
		return new BRecipe.Builder(recipeNames);
	}



	// # # # # # #                   # # # # # #
	// # # # # #    Cauldron Recipe    # # # # #
	// # # # # # #                   # # # # # #

	/**
	 * Get A BCauldronRecipe by its name.
	 * <p>Returns null if recipe with that name does not exist
	 */
	@Nullable
	public static BCauldronRecipe getCauldronRecipe(String name) {
		return BCauldronRecipe.get(name);
	}

	/**
	 * Add a New Cauldron Recipe.
	 * <p>Base Brews coming out of the Cauldron can be made from this recipe
	 * <p>The recipe can be changed or removed later.
	 *
	 * @param recipe The Cauldron Recipe to add
	 * @param saveForever Not Implemented yet.
	 *                    <br>If the recipe should be saved forever, even after the Server restarts
	 *                    <br>If True: Recipe will be saved until removed manually
	 *                    <br>If False: Recipe will be removed when the Server restarts
	 */
	public static void addCauldronRecipe(BCauldronRecipe recipe, boolean saveForever) {
		//recipe.setSaveInData(saveForever);
		if (saveForever) {
			throw new NotImplementedException();
		}
		BCauldronRecipe.getAddedRecipes().add(recipe);
		recipe.updateAcceptedLists();
	}

	/**
	 * Removes a Cauldron Recipe from the List of all Cauldron Recipes.
	 * <p>This can also remove Cauldron Recipes that were loaded from config,
	 * though these will be readded when reloading the config
	 *
	 * @param name The name of the cauldron recipe to remove
	 * @return The Cauldron Recipe that was removed, null if none was removed
	 */
	@Nullable
	public static BCauldronRecipe removeCauldronRecipe(String name) {
		List<BCauldronRecipe> recipes = BCauldronRecipe.getAllRecipes();
		for (int i = 0; i < recipes.size(); i++) {
			if (recipes.get(i).getName().equalsIgnoreCase(name)) {
				BCauldronRecipe remove = recipes.remove(i);
				if (i < BCauldronRecipe.numConfigRecipes) {
					// We removed one of the Config Recipes
					BCauldronRecipe.numConfigRecipes--;
				}
				return remove;
			}
		}
		return null;
	}

	/**
	 * Create a New Cauldron Recipe with a Recipe Builder.
	 *
	 * @param name The name of the new Cauldron Recipe
	 * @return A Cauldron Recipe Builder
	 */
	public static BCauldronRecipe.Builder cauldronRecipeBuilder(String name) {
		return new BCauldronRecipe.Builder(name);
	}


}
