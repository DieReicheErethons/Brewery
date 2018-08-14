package com.dre.brewery;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;

public class BCauldron {
	public static CopyOnWriteArrayList<BCauldron> bcauldrons = new CopyOnWriteArrayList<>();

	private BIngredients ingredients = new BIngredients();
	private Block block;
	private int state = 1;
	private boolean someRemoved = false;

	public BCauldron(Block block, ItemStack ingredient) {
		this.block = block;
		add(ingredient);
		bcauldrons.add(this);
	}

	// loading from file
	public BCauldron(Block block, BIngredients ingredients, int state) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		bcauldrons.add(this);
	}

	public void onUpdate() {
		// Check if fire still alive
		if (!block.getChunk().isLoaded() || block.getRelative(BlockFace.DOWN).getType() == Material.FIRE || LegacyUtil.isLava(block.getRelative(BlockFace.DOWN).getType())) {
			// add a minute to cooking time
			state++;
			if (someRemoved) {
				ingredients = ingredients.clone();
				someRemoved = false;
			}
		}
	}

	// add an ingredient to the cauldron
	public void add(ItemStack ingredient) {
		if (someRemoved) {
			ingredients = ingredients.clone();
			someRemoved = false;
		}
		ingredient = new ItemStack(ingredient.getType(), 1, ingredient.getDurability());
		ingredients.add(ingredient);
		block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
		if (state > 1) {
			state--;
		}
	}

	// get cauldron by Block
	public static BCauldron get(Block block) {
		for (BCauldron bcauldron : bcauldrons) {
			if (bcauldron.block.equals(block)) {
				return bcauldron;
			}
		}
		return null;
	}

	// get cauldron from block and add given ingredient
	public static boolean ingredientAdd(Block block, ItemStack ingredient) {
		// if not empty
		if (LegacyUtil.getFillLevel(block) != 0) {
			BCauldron bcauldron = get(block);
			if (bcauldron != null) {
				bcauldron.add(ingredient);
				return true;
			} else {
				new BCauldron(block, ingredient);
				return true;
			}
		}
		return false;
	}

	// fills players bottle with cooked brew
	public static boolean fill(Player player, Block block) {
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (!player.hasPermission("brewery.cauldron.fill")) {
				P.p.msg(player, P.p.languageReader.get("Perms_NoCauldronFill"));
				return true;
			}
			ItemStack potion = bcauldron.ingredients.cook(bcauldron.state);
			if (potion != null) {
				byte data = block.getData();
				if (data > 3) {
					data = 3;
					block.setData(data);
				} else if (data <= 0) {
					bcauldrons.remove(bcauldron);
					return false;
				}
				data -= 1;
				block.setData(data);

				if (data == 0) {
					bcauldrons.remove(bcauldron);
				} else {
					bcauldron.someRemoved = true;
				}
				// Bukkit Bug, inventory not updating while in event so this
				// will delay the give
				// but could also just use deprecated updateInventory()
				giveItem(player, potion);
				// player.getInventory().addItem(potion);
				// player.getInventory().updateInventory();
				return true;
			}
		}
		return false;
	}

	// prints the current cooking time to the player
	public static void printTime(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			P.p.msg(player, P.p.languageReader.get("Error_NoPermissions"));
			return;
		}
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (bcauldron.state > 1) {
				P.p.msg(player, P.p.languageReader.get("Player_CauldronInfo1", "" + bcauldron.state));
			} else {
				P.p.msg(player, P.p.languageReader.get("Player_CauldronInfo2"));
			}
		}
	}

	// reset to normal cauldron
	public static void remove(Block block) {
		if (LegacyUtil.getFillLevel(block) != 0) {
			BCauldron bcauldron = get(block);
			if (bcauldron != null) {
				bcauldrons.remove(bcauldron);
			}
		}
	}

	// unloads cauldrons that are in a unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(String name) {
		for (BCauldron bcauldron : bcauldrons) {
			if (bcauldron.block.getWorld().getName().equals(name)) {
				bcauldrons.remove(bcauldron);
			}
		}
	}

	public static void save(ConfigurationSection config, ConfigurationSection oldData) {
		P.p.createWorldSections(config);

		if (!bcauldrons.isEmpty()) {
			int id = 0;
			for (BCauldron cauldron : bcauldrons) {
				String worldName = cauldron.block.getWorld().getName();
				String prefix;

				if (worldName.startsWith("DXL_")) {
					prefix = P.p.getDxlName(worldName) + "." + id;
				} else {
					prefix = cauldron.block.getWorld().getUID().toString() + "." + id;
				}

				config.set(prefix + ".block", cauldron.block.getX() + "/" + cauldron.block.getY() + "/" + cauldron.block.getZ());
				if (cauldron.state != 1) {
					config.set(prefix + ".state", cauldron.state);
				}
				config.set(prefix + ".ingredients", cauldron.ingredients.serializeIngredients());
				id++;
			}
		}
		// copy cauldrons that are not loaded
		if (oldData != null){
			for (String uuid : oldData.getKeys(false)) {
				if (!config.contains(uuid)) {
					config.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

	// bukkit bug not updating the inventory while executing event, have to
	// schedule the give
	public static void giveItem(final Player player, final ItemStack item) {
		P.p.getServer().getScheduler().runTaskLater(P.p, new Runnable() {
			public void run() {
				player.getInventory().addItem(item);
			}
		}, 1L);
	}

}