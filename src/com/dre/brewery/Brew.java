package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.BrewerInventory;

import com.dre.brewery.BIngredients;

public class Brew {

	public static Map<Integer,Brew> potions=new HashMap<Integer,Brew>();

	//represents the liquid in the brewed Potions

	private BIngredients ingredients;
	private int quality;
	private int distillRuns;
	private float ageTime;
	private int alcohol;

	public Brew(int uid,BIngredients ingredients){
		this.ingredients = ingredients;
		potions.put(uid,this);
	}

	//quality already set
	public Brew(int uid,int quality,int alcohol,BIngredients ingredients){
		this.ingredients = ingredients;
		this.quality = quality;
		this.alcohol = calcAlcohol(alcohol);
		potions.put(uid,this);
	}

	//loading from file
	public Brew(int uid,BIngredients ingredients,int quality,int distillRuns,float ageTime,int alcohol){
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.alcohol = alcohol;
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
				P.p.errorLog("Database failure! unable to find UID "+uid+" of a custom Potion!");
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

	//calculate alcohol from recipe
	public int calcAlcohol(int alc){
		if(distillRuns == 0){
			distillRuns = 1;
		}
		alc /= distillRuns;
		alc *= distillRuns * ((float)quality / 10.0);
		return alc;
	}

	//calculating quality
	public int calcQuality(BRecipe recipe,byte wood){
		//calculate quality from all of the factors
		float quality =(

		ingredients.getIngredientQuality(recipe) +
		ingredients.getCookingQuality(recipe) +
		ingredients.getWoodQuality(recipe,wood) +
		ingredients.getAgeQuality(recipe,ageTime));

		quality /= 4;
		return (int)Math.round(quality);
	}

	public int getQuality(){
		return quality;
	}

	//return prev calculated alcohol
	public int getAlcohol(){
		return alcohol;
	}




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
			brew.quality = brew.calcQuality(recipe,(byte)0);
			P.p.log("destilled "+recipe.getName(5)+" has Quality: "+brew.quality);
			brew.distillRuns += 1;
			//distillRuns will have an effect on the amount of alcohol, not the quality
			if(brew.distillRuns > 1){
				ArrayList<String> lore = new ArrayList<String>();
				lore.add(brew.distillRuns+" fach Destilliert");
				potionMeta.setLore(lore);
			}
			brew.alcohol = brew.calcAlcohol(recipe.getAlcohol());

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
					if(!recipe.needsDistilling() || brew.distillRuns > 0){

						brew.quality = brew.calcQuality(recipe,wood);
						brew.alcohol = brew.calcAlcohol(recipe.getAlcohol());
						P.p.log("Final "+recipe.getName(5)+" has Quality: "+brew.quality);

						potionMeta.setDisplayName(recipe.getName(brew.quality));
						item.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(false));
						item.setItemMeta(potionMeta);
					}
				}
			}
		}
	}

	//Saves all data
	public static void save(ConfigurationSection config){
		for(int uid:potions.keySet()){
			ConfigurationSection idConfig = config.createSection(""+uid);
			Brew brew = potions.get(uid);
			//not saving unneccessary data
			if(brew.quality != 0){
				idConfig.set("quality", brew.quality);
			}
			if(brew.distillRuns != 0){
				idConfig.set("distillRuns", brew.distillRuns);
			}
			if(brew.ageTime != 0){
				idConfig.set("ageTime", brew.ageTime);
			}
			if(brew.alcohol != 0){
				idConfig.set("alcohol", brew.alcohol);
			}
			//save the ingredients
			brew.ingredients.save(idConfig.createSection("ingredients"));
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