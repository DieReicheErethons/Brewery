package com.dre.brewery;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;


public class BRecipe {

	private String[] name;
	private Map<Material,Integer> ingredients = new HashMap<Material,Integer>();//material and amount
	private int cookingTime;//time to cook in cauldron
	private int distillruns;//runs through the brewer
	private int wood;//type of wood the barrel has to consist of
	private int age;//time in minecraft days for the potions to age in barrels
	private String color;//color of the destilled/finished potion
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
		List<String> ingredientsList = configSectionRecipes.getStringList(recipeId+".ingredients");
		for(String item:ingredientsList){
			String[] ingredParts = item.split("/");
			ingredParts[0] = ingredParts[0].toUpperCase();
			this.ingredients.put(Material.getMaterial(ingredParts[0]),P.p.parseInt(ingredParts[1]));
		}
		this.cookingTime = configSectionRecipes.getInt(recipeId+".cookingtime");
		this.distillruns = configSectionRecipes.getInt(recipeId+".distillruns");
		this.wood = configSectionRecipes.getInt(recipeId+".wood");
		this.age = configSectionRecipes.getInt(recipeId+".age");
		this.color = configSectionRecipes.getString(recipeId+".color");
		this.difficulty = configSectionRecipes.getInt(recipeId+".difficulty");
		this.alcohol = configSectionRecipes.getInt(recipeId+".alcohol");
	}



	//allowed deviation to the recipes count of ingredients at the given difficulty
	public int allowedCountDiff(int count){
		int allowedCountDiff = Math.round((float)((11.0 - difficulty) * (count / 10.0)));

		if(allowedCountDiff == 0){
			return 1;
		}
		return allowedCountDiff;
	}

	//allowed deviation to the recipes cooking-time at the given difficulty
	public int allowedTimeDiff(int time){
		int allowedTimeDiff = Math.round((float)((11.0 - difficulty) * (time / 10.0)));

		while(allowedTimeDiff >= time){
			allowedTimeDiff -= 1;
		}
		if(allowedTimeDiff == 0){
			return 1;
		}
		return allowedTimeDiff;
	}

	//difference between given and recipe-wanted woodtype
	public float getWoodDiff(byte wood){
		int woodType = 0;
		if(wood == 0x0){
			woodType = 2;
		}else if(wood == 0x1){
			woodType = 4;
		} else if(wood == 0x2){
			woodType = 1;
		} else if(wood == 0x3){
			woodType = 3;
		}
		return Math.abs(woodType - wood);
	}

	public boolean isCookingOnly(){
		if(age == 0 && distillruns == 0){
			return true;
		}
		return false;
	}

	public boolean needsToAge(){
		if(age == 0){
			return false;
		}
		return true;
	}

	//true if given map misses an ingredient
	public boolean isMissingIngredients(Map<Material,Integer> map){
		if(map.size() < ingredients.size()){
			return true;
		}
		for(Material ingredient:ingredients.keySet()){
			if(!map.containsKey(ingredient)){
				return true;
			}
		}
		return false;
	}



	//Getter

	//how many of a specific ingredient in the recipe
	public int amountOf(Material material){
		if(ingredients.containsKey(material)){
			return ingredients.get(material);
		}
		return 0;
	}

	//name that fits the quality
	public String getName(int quality){
		if(name.length > 2){
			if(quality <= 3){
				return name[0];
			} else if(quality <= 7){
				return name[1];
			} else {
				return name[2];
			}
		} else {
			return name[0];
		}
	}

	public int getCookingTime(){
		return cookingTime;
	}

	public int getDistillRuns(){
		return distillruns;
	}

	public String getColor(){
		return color.toUpperCase();
	}

	//get the woodtype in blockData-byte
	public byte getWood(){
		if(wood == 1){
			return 0x2;
		} else if(wood == 2){
			return 0x0;
		} else if(wood == 3){
			return 0x3;
		} else if(wood == 4){
			return 0x1;
		}
		return 0x8;
	}

	public float getAge(){
		return (float) age;
	}

	public int getDifficulty(){
		return difficulty;
	}


}