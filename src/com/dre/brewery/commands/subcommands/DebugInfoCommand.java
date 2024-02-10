package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.RecipeItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DebugInfoCommand implements SubCommand {

    private final BreweryPlugin breweryPlugin;

    public DebugInfoCommand(BreweryPlugin breweryPlugin) {
        this.breweryPlugin = breweryPlugin;
    }

    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        debugInfo(sender, args.length > 1 ? args[1] : null);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.debuginfo";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }


    public void debugInfo(CommandSender sender, String recipeName) {
        if (!BreweryPlugin.use1_9 || !sender.isOp()) return;
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null) {
            Brew brew = Brew.get(hand);
            if (brew == null) return;
            BreweryPlugin.getInstance().log(brew.toString());
            BIngredients ingredients = brew.getIngredients();
            if (recipeName == null) {
                BreweryPlugin.getInstance().log("&lIngredients:");
                for (Ingredient ing : ingredients.getIngredientList()) {
                    BreweryPlugin.getInstance().log(ing.toString());
                }
                BreweryPlugin.getInstance().log("&lTesting Recipes");
                for (BRecipe recipe : BRecipe.getAllRecipes()) {
                    int ingQ = ingredients.getIngredientQuality(recipe);
                    int cookQ = ingredients.getCookingQuality(recipe, false);
                    int cookDistQ = ingredients.getCookingQuality(recipe, true);
                    int ageQ = ingredients.getAgeQuality(recipe, brew.getAgeTime());
                    BreweryPlugin.getInstance().log(recipe.getRecipeName() + ": ingQlty: " + ingQ + ", cookQlty:" + cookQ + ", cook+DistQlty: " + cookDistQ + ", ageQlty: " + ageQ);
                }
                BRecipe distill = ingredients.getBestRecipe(brew.getWood(), brew.getAgeTime(), true);
                BRecipe nonDistill = ingredients.getBestRecipe(brew.getWood(), brew.getAgeTime(), false);
                BreweryPlugin.getInstance().log("&lWould prefer Recipe: " + (nonDistill == null ? "none" : nonDistill.getRecipeName()) + " and Distill-Recipe: " + (distill == null ? "none" : distill.getRecipeName()));
            } else {
                BRecipe recipe = BRecipe.getMatching(recipeName);
                if (recipe == null) {
                    BreweryPlugin.getInstance().msg(player, "Could not find Recipe " + recipeName);
                    return;
                }
                BreweryPlugin.getInstance().log("&lIngredients in Recipe " + recipe.getRecipeName() + ":");
                for (RecipeItem ri : recipe.getIngredients()) {
                    BreweryPlugin.getInstance().log(ri.toString());
                }
                BreweryPlugin.getInstance().log("&lIngredients in Brew:");
                for (Ingredient ingredient : ingredients.getIngredientList()) {
                    int amountInRecipe = recipe.amountOf(ingredient);
                    BreweryPlugin.getInstance().log(ingredient.toString() + ": " + amountInRecipe + " of this are in the Recipe");
                }
                int ingQ = ingredients.getIngredientQuality(recipe);
                int cookQ = ingredients.getCookingQuality(recipe, false);
                int cookDistQ = ingredients.getCookingQuality(recipe, true);
                int ageQ = ingredients.getAgeQuality(recipe, brew.getAgeTime());
                BreweryPlugin.getInstance().log("ingQlty: " + ingQ + ", cookQlty:" + cookQ + ", cook+DistQlty: " + cookDistQ  + ", ageQlty: " + ageQ);
            }

            BreweryPlugin.getInstance().msg(player, "Debug Info for item written into Log");
        }
    }
}
