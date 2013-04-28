package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.BrewerInventory;

import com.dre.brewery.BIngredients;

import com.dre.brewery.P;

public class Brew {

	public static Map<Integer,Brew> potions=new HashMap<Integer,Brew>();

	//represents the liquid in the brewed Potions

	private BIngredients ingredients;
	private int quality;
	private int distillRuns;
	private float ageTime;

	public Brew(int uid,BIngredients ingredients){
		this.ingredients = ingredients;
		potions.put(uid,this);
	}

	//quality already set
	public Brew(int uid,int quality,BIngredients ingredients){
		this.ingredients = ingredients;
		this.quality = quality;
		potions.put(uid,this);
	}

	//remove potion from map (drinking, despawning, should be more!)
	public static void remove(ItemStack item){
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		Brew brew = get(meta);
		if(brew != null){
			potions.remove(brew);
		}
	}


	//generate an UID
	public static int generateUID(){
		int uid = -2;
		while(potions.containsKey(uid)){
			uid -= 1;
		}
		return uid;
	}

	//returns a Brew by its UID
	public static Brew getByUID(int uid){
		if(uid < -1){
			if(!potions.containsKey(uid)){
				P.p.log("Database failure! unable to find UID "+uid+" of a custom Potion in the db!");
				return null;//throw some exception?
			}
		} else {
			return null;
		}
		return potions.get(uid);

	}

	//returns a Brew by PotionMeta
	public static Brew get(PotionMeta meta){
		if(meta.hasCustomEffects()){
			for(PotionEffect effect:meta.getCustomEffects()){
				if(effect.getType().equals(PotionEffectType.REGENERATION)){
					return getByUID(effect.getDuration());
				}
			}
		}
		return null;
	}

	//returns a Brew by ItemStack
	/*public static Brew get(ItemStack item){
		if(item.getTypeId() == 373){
			PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
			return get(potionMeta);
		}
		return null;
	}*/




	//Distilling section ---------------


	//distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv,Integer[] contents){
		int slot = 0;
		while (slot < 3){
			if(contents[slot] == 1){
				distillSlot(inv,slot);
			}
			slot++;
		}
	}

	//distill custom potion in given slot
	public static void distillSlot(BrewerInventory inv,int slot){
		ItemStack slotItem = inv.getItem(slot);
		PotionMeta potionMeta = (PotionMeta) slotItem.getItemMeta();
		Brew brew = get(potionMeta);
		BRecipe recipe = brew.ingredients.getdistillRecipe();

		if(recipe != null){
			//calculate quality of ingredients and cookingtime
			float quality = brew.ingredients.getIngredientQuality(recipe) + brew.ingredients.getCookingQuality(recipe);
			quality /= 2;

			/*if(recipe.getDistillRuns() > 1){
				quality -= Math.abs(recipe.getDistillRuns() - (brew.distillRuns + 1)) * 2;
			}*/

			brew.quality = (int)Math.round(quality);
			P.p.log("destilled "+recipe.getName(5)+" has Quality: "+brew.quality);
			brew.distillRuns += 1;
			//distillRuns will have an effect on the amount of alcohol, not the quality
			if(brew.distillRuns > 1){
				ArrayList<String> lore = new ArrayList<String>();
				lore.add(brew.distillRuns+" fach Destilliert");
				potionMeta.setLore(lore);
			}

			potionMeta.setDisplayName(recipe.getName(brew.quality));

			//if the potion should be further distillable
			if(recipe.getDistillRuns() > 1){
				slotItem.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(true));
			} else {
				slotItem.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(false));
			}
		} else {
			potionMeta.setDisplayName("Undefinierbares Destillat");
			slotItem.setDurability(PotionColor.GREY.getColorId(true));
		}

		slotItem.setItemMeta(potionMeta);
	}




	//Ageing Section ------------------


	public static void age(ItemStack item,float time,byte wood){
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		Brew brew = get(potionMeta);
		if(brew != null){
			brew.ageTime += time;
			//if younger than half a day, it shouldnt get aged form
			if(brew.ageTime > 0.5){
				BRecipe recipe = brew.ingredients.getAgeRecipe(wood,brew.ageTime);
				if(recipe != null){

					//calculate quality from all of the factors
					float quality =(

					brew.ingredients.getIngredientQuality(recipe) +
					brew.ingredients.getCookingQuality(recipe) +
					brew.ingredients.getWoodQuality(recipe,wood) +
					brew.ingredients.getAgeQuality(recipe,brew.ageTime));

					quality /= 4;
					brew.quality = (int)Math.round(quality);
					P.p.log("Final "+recipe.getName(5)+" has Quality: "+brew.quality);

					potionMeta.setDisplayName(recipe.getName(brew.quality));
					item.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(false));
					item.setItemMeta(potionMeta);
				}
			}
		}
	}




	public static enum PotionColor{
		PINK(1),
		CYAN(2),
		ORANGE(3),
		GREEN(4),
		BRIGHT_RED(5),
		BLUE(6),
		BLACK(8),
		RED(9),
		GREY(10),
		WATER(11),
		DARK_RED(12),
		BRIGHT_GREY(14);

		private final int colorId;

		private PotionColor(int colorId) {
			this.colorId = colorId;
		}

		//gets the Damage Value, that sets a color on the potion
		//offset +32 is not accepted by brewer, so not further destillable
		public short getColorId(boolean destillable){
			if(destillable){
				return (short) (colorId + 64);
			}
			return (short) (colorId + 32);
		}
	}


}