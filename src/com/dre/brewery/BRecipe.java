package com.dre.brewery;

//import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
//import org.bukkit.potion.Potion;
//import org.bukkit.potion.PotionType;
//import org.bukkit.potion.PotionEffectType;

import com.dre.brewery.P;
import com.dre.brewery.BIngredients;

public class BRecipe {

	private String[] name;// = new String[3];
	private Map<Material,Integer> ingredients = new HashMap<Material,Integer>();//material and amount
	private int cookingTime;//time to cook in cauldron
	private int destillruns;//runs through the brewer
	private Material filter;//filter on top of the brewer
	private Material destillPotion;//potion to add to custom ones in brewer
	private int wood;//type of wood the barrel has to consist of
	private int difficulty;//difficulty to brew the potion, how exact the instruction has to be followed
	private int alcohol;//Vol% of alcohol in perfect potion

	public BRecipe(ConfigurationSection configSectionRecipes,String recipeId){
		String[] name = configSectionRecipes.getString(recipeId+".name").split("/");
		if(name.length > 2){
			this.name = name;
		} else {
			this.name = new String[1];
			this.name[0] = name[0];
		}
		P.p.log("nameLength="+name.length);
		List<String> ingredientsList = configSectionRecipes.getStringList(recipeId+".ingredients");
		for(String item:ingredientsList){
			String[] ingredParts = item.split("/");
			ingredParts[0] = ingredParts[0].toUpperCase();
			this.ingredients.put(Material.getMaterial(ingredParts[0]),P.p.parseInt(ingredParts[1]));
		}
		this.cookingTime = configSectionRecipes.getInt(recipeId+".cookingtime");
		this.destillruns = configSectionRecipes.getInt(recipeId+".destillruns");
		this.filter = Material.getMaterial(configSectionRecipes.getString(recipeId+".filter"));
		this.destillPotion = Material.getMaterial(configSectionRecipes.getString(recipeId+".destillpotion"));
		if(destillPotion == null){
			destillPotion = Material.POTION;
		}
		this.wood = configSectionRecipes.getInt(recipeId+".wood");
		this.difficulty = configSectionRecipes.getInt(recipeId+".difficulty");
		this.alcohol = configSectionRecipes.getInt(recipeId+".alcohol");
	}



	public boolean isCookingOnly(){
		if(wood == 0 && destillruns == 0){
			return true;
		}
		return false;
	}


}