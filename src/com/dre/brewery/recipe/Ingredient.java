package com.dre.brewery.recipe;

import org.bukkit.inventory.ItemStack;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Item used in a BIngredients, inside BCauldron or Brew,
 * Represents the Items used as ingredients in the Brewing process.
 * <p>Can be a copy of a recipe item
 * <p>Will be saved and loaded with a DataStream
 * <p>Each implementing class needs to register a static function as Item Loader
 */
public interface Ingredient {

	/**
	 * Saves this Ingredient to the DataOutputStream.
	 * <p>The first data HAS to be storing the SaveID like:
	 * out.writeUTF("AB");
	 * <p>Amount will be saved automatically and does not have to be saved here.
	 * <p>Saving is done to Brew or for BCauldron into data.yml
	 *
	 * @param out The outputstream to write to
	 * @throws IOException Any IOException
	 */
	void saveTo(DataOutputStream out) throws IOException;

	int getAmount();

	void setAmount(int amount);

	/**
	 * Does this Ingredient match the given ItemStack
	 *
	 * @param item The given ItemStack to match
	 * @return true if all required data is contained on the item
	 */
	boolean matches(ItemStack item);

	/*
	 * Does this Item match the given RecipeItem.
	 * <p>An IngredientItem matches a RecipeItem if all required info of the RecipeItem are fulfilled on this IngredientItem
	 * <p>This does not imply that the same holds the other way round, as this item might have more info than needed
	 *
	 * @param recipeItem The recipeItem whose requirements need to be fulfilled
	 * @return True if this matches the required info of the recipeItem
	 */
	//boolean matches(RecipeItem recipeItem);

	/**
	 * The other Ingredient is Similar if it is equal except amount.
	 *
	 * @param item The item to check similarity with
	 * @return True if this is equal to item except for amount
	 */
	boolean isSimilar(Ingredient item);

	String displayName();

}
