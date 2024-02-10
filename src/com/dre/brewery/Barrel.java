package com.dre.brewery;

import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelCreateEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.LegacyUtil;
import com.github.Anon8281.universalScheduler.UniversalRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Multi Block Barrel with Inventory
 */
public class Barrel implements InventoryHolder {

	public static volatile List<Barrel> barrels = new ArrayList<>();
	private static int check = 0; // Which Barrel was last checked

	private final Block spigot;
	private final BarrelBody body; // The Blocks that make up a Barrel in the World
	private boolean checked; // Checked by the random BarrelCheck routine
	private Inventory inventory;
	private float time;

	public Barrel(Block spigot, byte signoffset) {
		this.spigot = spigot;
		if (isLarge()) {
			inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 27, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
		} else {
			inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 9, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
		}
		body = new BarrelBody(this, signoffset);
	}

	/**
	 * load from file
	 */
	public Barrel(Block spigot, byte sign, BoundingBox bounds, Map<String, Object> items, float time) {
		this(spigot, sign, bounds, items, time, false);
	}

	/**
	 * Load from File
	 * <p>If async: true, The Barrel Bounds will not be recreated when missing/corrupt, getBody().getBounds() will be null if it needs recreating
	 */
	public Barrel(Block spigot, byte sign, BoundingBox bounds, Map<String, Object> items, float time, boolean async) {
		this.spigot = spigot;
		if (isLarge()) {
			this.inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 27, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
		} else {
			this.inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 9, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
		}
		if (items != null) {
			for (String slot : items.keySet()) {
				if (items.get(slot) instanceof ItemStack) {
					this.inventory.setItem(BreweryPlugin.getInstance().parseInt(slot), (ItemStack) items.get(slot));
				}
			}
		}
		this.time = time;

		body = new BarrelBody(this, sign, bounds, async);
	}

	public static void onUpdate() {
		for (Barrel barrel : barrels) {
			// Minecraft day is 20 min, so add 1/20 to the time every minute
			if (barrel != null) {
				barrel.time += (1.0 / 20.0);
			}
		}
		int numBarrels = barrels.size();
		if (check == 0 && numBarrels > 0) {
			Barrel random = barrels.get((int) Math.floor(Math.random() * numBarrels));
			if (random != null) {
				// You have been selected for a random search
				// We want to check at least one barrel every time
				random.checked = false;
			}
			if (numBarrels > 50) {
				Barrel randomInTheBack = barrels.get(numBarrels - 1 - (int) (Math.random() * (numBarrels >>> 2)));
				if (randomInTheBack != null) {
					// Prioritize checking one of the less recently used barrels as well
					randomInTheBack.checked = false;
				}
			}
			new BarrelCheck().runTaskTimer(BreweryPlugin.getInstance(), 1, 1);
		}
	}

	public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
		if (isLarge()) {
			if (!player.hasPermission("brewery.openbarrel.big")) {
				BreweryPlugin.getInstance().msg(player, BreweryPlugin.getInstance().languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		} else {
			if (!player.hasPermission("brewery.openbarrel.small")) {
				BreweryPlugin.getInstance().msg(player, BreweryPlugin.getInstance().languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		}

		// Call event
		BarrelAccessEvent accessEvent = new BarrelAccessEvent(this, player, event.getClickedBlock(), event.getBlockFace());
		// Listened to by IntegrationListener
		BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(accessEvent);
		return !accessEvent.isCancelled();
	}

	/**
	 * Ask for permission to destroy barrel
	 */
	public boolean hasPermsDestroy(Player player, Block block, BarrelDestroyEvent.Reason reason) {
		// Listened to by LWCBarrel (IntegrationListener)
		BarrelDestroyEvent destroyEvent = new BarrelDestroyEvent(this, block, reason, player);
		BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(destroyEvent);
		return !destroyEvent.isCancelled();
	}

	/**
	 * player opens the barrel
	 */
	public void open(Player player) {
		if (inventory == null) {
			if (isLarge()) {
				inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 27, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
			} else {
				inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 9, BreweryPlugin.getInstance().languageReader.get("Etc_Barrel"));
			}
		} else {
			if (time > 0) {
				// if nobody has the inventory opened
				if (inventory.getViewers().isEmpty()) {
					// if inventory contains potions
					if (inventory.contains(Material.POTION)) {
						byte wood = body.getWood();
						long loadTime = System.nanoTime();
						for (ItemStack item : inventory.getContents()) {
							if (item != null) {
								Brew brew = Brew.get(item);
								if (brew != null) {
									brew.age(item, time, wood);
								}
							}
						}
						loadTime = System.nanoTime() - loadTime;
						float ftime = (float) (loadTime / 1000000.0);
						BreweryPlugin.getInstance().debugLog("opening Barrel with potions (" + ftime + "ms)");
					}
				}
			}
		}
		// reset barreltime, potions have new age
		time = 0;

		if (BConfig.useLB) {
			try {
				LogBlockBarrel.openBarrel(player, inventory, spigot.getLocation());
			} catch (Throwable e) {
				BreweryPlugin.getInstance().errorLog("Failed to Log Barrel to LogBlock!");
				BreweryPlugin.getInstance().errorLog("Brewery was tested with version 1.94 of LogBlock!");
				e.printStackTrace();
			}
		}
		player.openInventory(inventory);
	}

	public void playOpeningSound() {
		float randPitch = (float) (Math.random() * 0.1);
		Location location = getSpigot().getLocation();
		if (location.getWorld() == null) return;
		if (isLarge()) {
			location.getWorld().playSound(location, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.4f, 0.55f + randPitch);
			//getSpigot().getWorld().playSound(getSpigot().getLocation(), Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.5f, 0.6f + randPitch);
			location.getWorld().playSound(location, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.4f, 0.45f + randPitch);
		} else {
			location.getWorld().playSound(location, Sound.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
		}
	}

	public void playClosingSound() {
		float randPitch = (float) (Math.random() * 0.1);
		Location location = getSpigot().getLocation();
		if (location.getWorld() == null) return;
		if (isLarge()) {
			location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.5f + randPitch);
			location.getWorld().playSound(location, Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.2f, 0.6f + randPitch);
		} else {
			location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
		}
	}

	@Override
	@NotNull
	public Inventory getInventory() {
		return inventory;
	}

	@NotNull
	public Block getSpigot() {
		return spigot;
	}

	@NotNull
	public BarrelBody getBody() {
		return body;
	}

	public float getTime() {
		return time;
	}

	/**
	 * Returns true if this Block is part of this Barrel
	 */
	public boolean hasBlock(Block block) {
		return body.hasBlock(block);
	}

	/**
	 * @deprecated just use hasBlock
	 */
	@Deprecated
	public boolean hasWoodBlock(Block block) {
		return body.hasBlock(block);
	}

	/**
	 * @deprecated just use hasBlock
	 */
	@Deprecated
	public boolean hasStairsBlock(Block block) {
		return body.hasBlock(block);
	}

	/**
	 * Get the Barrel by Block, null if that block is not part of a barrel
	 */
	@Nullable
	public static Barrel get(Block block) {
		if (block == null) {
			return null;
		}
		Material type = block.getType();
		if (LegacyUtil.isFence(type) || LegacyUtil.isSign(type)) {
			return getBySpigot(block);
		} else {
			return getByWood(block);
		}
	}

	/**
	 * Get the Barrel by Sign or Spigot (Fastest)
	 */
	@Nullable
	public static Barrel getBySpigot(Block sign) {
		// convert spigot if neccessary
		Block spigot = BarrelBody.getSpigotOfSign(sign);

		byte signoffset = 0;
		if (!spigot.equals(sign)) {
			signoffset = (byte) (sign.getY() - spigot.getY());
		}

		int i = 0;
		for (Barrel barrel : barrels) {
			if (barrel != null && barrel.body.isSignOfBarrel(signoffset)) {
				if (barrel.spigot.equals(spigot)) {
					if (barrel.body.getSignoffset() == 0 && signoffset != 0) {
						// Barrel has no signOffset even though we clicked a sign, may be old
						barrel.body.setSignoffset(signoffset);
					}
					moveMRU(i);
					return barrel;
				}
			}
			i++;
		}
		return null;
	}

	/**
	 * Get the barrel by its corpus (Wood Planks, Stairs)
	 */
	@Nullable
	public static Barrel getByWood(Block wood) {
		if (LegacyUtil.isWoodPlanks(wood.getType()) || LegacyUtil.isWoodStairs(wood.getType())) {
			int i = 0;
			for (Barrel barrel : barrels) {
				if (barrel.getSpigot().getWorld().equals(wood.getWorld()) && barrel.body.getBounds().contains(wood)) {
					moveMRU(i);
					return barrel;
				}
				i++;
			}
		}
		return null;
	}

	// Move Barrel that was recently used more towards the front of the List
	// Optimizes retrieve by Block over time
	private static void moveMRU(int index) {
		if (index > 0) {
			// Swap entry at the index with the one next to it
			barrels.set(index - 1, barrels.set(index, barrels.get(index - 1)));
		}
	}

	/**
	 * creates a new Barrel out of a sign
	 */
	public static boolean create(Block sign, Player player) {
		Block spigot = BarrelBody.getSpigotOfSign(sign);

		byte signoffset = 0;
		if (!spigot.equals(sign)) {
			signoffset = (byte) (sign.getY() - spigot.getY());
		}

		Barrel barrel = getBySpigot(spigot);
		if (barrel == null) {
			barrel = new Barrel(spigot, signoffset);
			if (barrel.body.getBrokenBlock(true) == null) {
				if (LegacyUtil.isSign(spigot.getType())) {
					if (!player.hasPermission("brewery.createbarrel.small")) {
						BreweryPlugin.getInstance().msg(player, BreweryPlugin.getInstance().languageReader.get("Perms_NoSmallBarrelCreate"));
						return false;
					}
				} else {
					if (!player.hasPermission("brewery.createbarrel.big")) {
						BreweryPlugin.getInstance().msg(player, BreweryPlugin.getInstance().languageReader.get("Perms_NoBigBarrelCreate"));
						return false;
					}
				}
				BarrelCreateEvent createEvent = new BarrelCreateEvent(barrel, player);
				BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(createEvent);
				if (!createEvent.isCancelled()) {
					barrels.add(0, barrel);
					return true;
				}
			}
		} else {
			if (barrel.body.getSignoffset() == 0 && signoffset != 0) {
				barrel.body.setSignoffset(signoffset);
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes a barrel, throwing included potions to the ground
	 *
	 * @param broken    The Block that was broken
	 * @param breaker   The Player that broke it, or null if not known
	 * @param dropItems If the items in the barrels inventory should drop to the ground
	 */
	public void remove(@Nullable Block broken, @Nullable Player breaker, boolean dropItems) {
		BarrelRemoveEvent event = new BarrelRemoveEvent(this, dropItems);
		// Listened to by LWCBarrel (IntegrationListener)
		BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);

		if (inventory != null) {
			List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
			// Copy List to fix ConcModExc
			for (HumanEntity viewer : viewers) {
				viewer.closeInventory();
			}
			ItemStack[] items = inventory.getContents();
			inventory.clear();
			if (BConfig.useLB && breaker != null) {
				try {
					LogBlockBarrel.breakBarrel(breaker, items, spigot.getLocation());
				} catch (Throwable e) {
					BreweryPlugin.getInstance().errorLog("Failed to Log Barrel-break to LogBlock!");
					BreweryPlugin.getInstance().errorLog("Brewery was tested with version 1.94 of LogBlock!");
					e.printStackTrace();
				}
			}
			if (event.willDropItems()) {
				byte wood = body.getWood();
				for (ItemStack item : items) {
					if (item != null) {
						Brew brew = Brew.get(item);
						if (brew != null) {
							// Brew before throwing
							brew.age(item, time, wood);
							PotionMeta meta = (PotionMeta) item.getItemMeta();
							if (BrewLore.hasColorLore(meta)) {
								BrewLore lore = new BrewLore(brew, meta);
								lore.convertLore(false);
								lore.write();
								item.setItemMeta(meta);
							}
						}
						// "broken" is the block that destroyed, throw them there!
						if (broken != null) {
							broken.getWorld().dropItem(broken.getLocation(), item);
						} else {
							spigot.getWorld().dropItem(spigot.getLocation(), item);
						}
					}
				}
			}
		}

		barrels.remove(this);
	}

	/**
	 * is this a Large barrel?
	 */
	public boolean isLarge() {
		return !isSmall();
	}

	/**
	 * is this a Small barrel?
	 */
	public boolean isSmall() {
		return LegacyUtil.isSign(spigot.getType());
	}

	/**
	 * returns the Sign of a large barrel, the spigot if there is none
	 */
	public Block getSignOfSpigot() {
		return body.getSignOfSpigot();
	}

	/**
	 * returns the fence above/below a block, itself if there is none
	 */
	public static Block getSpigotOfSign(Block block) {
		return BarrelBody.getSpigotOfSign(block);
	}

	/**
	 * returns null if Barrel is correctly placed; the block that is missing when not.
	 * <p>The barrel needs to be formed correctly
	 *
	 * @param force to also check even if chunk is not loaded
	 */
	public Block getBrokenBlock(boolean force) {
		return body.getBrokenBlock(force);
	}

	/**
	 * Are any Barrels in that World
	 */
	public static boolean hasDataInWorld(World world) {
		return barrels.stream().anyMatch(barrel -> barrel.spigot.getWorld().equals(world));
	}

	/**
	 * unloads barrels that are in a unloading world
	 */
	public static void onUnload(World world) {
		barrels.removeIf(barrel -> barrel.spigot.getWorld().equals(world));
	}

	/**
	 * Unload all Barrels that have a Block in a unloaded World
	 */
	public static void unloadWorlds() {
		List<World> worlds = BreweryPlugin.getInstance().getServer().getWorlds();
		barrels.removeIf(barrel -> !worlds.contains(barrel.spigot.getWorld()));
	}

	/**
	 * Saves all data
	 */
	public static void save(ConfigurationSection config, ConfigurationSection oldData) {
		BUtil.createWorldSections(config);

		if (!barrels.isEmpty()) {
			int id = 0;
			for (Barrel barrel : barrels) {

				String worldName = barrel.spigot.getWorld().getName();
				String prefix;

				if (worldName.startsWith("DXL_")) {
					prefix = BUtil.getDxlName(worldName) + "." + id;
				} else {
					prefix = barrel.spigot.getWorld().getUID().toString() + "." + id;
				}

				// block: x/y/z
				config.set(prefix + ".spigot", barrel.spigot.getX() + "/" + barrel.spigot.getY() + "/" + barrel.spigot.getZ());

				// save the body data into the section as well
				barrel.body.save(config, prefix);

				if (barrel.inventory != null) {
					int slot = 0;
					ItemStack item;
					ConfigurationSection invConfig = null;
					while (slot < barrel.inventory.getSize()) {
						item = barrel.inventory.getItem(slot);
						if (item != null) {
							if (invConfig == null) {
								if (barrel.time != 0) {
									config.set(prefix + ".time", barrel.time);
								}
								invConfig = config.createSection(prefix + ".inv");
							}
							// ItemStacks are configurationSerializeable, makes them
							// really easy to save
							invConfig.set(slot + "", item);
						}

						slot++;
					}
				}

				id++;
			}
		}
		// also save barrels that are not loaded
		if (oldData != null) {
			for (String uuid : oldData.getKeys(false)) {
				if (!config.contains(uuid)) {
					config.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

	public static class BarrelCheck extends UniversalRunnable {
		@Override
		public void run() {
			boolean repeat = true;
			while (repeat) {
				if (check < barrels.size()) {
					Barrel barrel = barrels.get(check);
					if (!barrel.checked) {
						BreweryPlugin.getScheduler().runTaskTimer(barrel.getSpigot().getLocation(), () -> {
							Block broken = barrel.body.getBrokenBlock(false);
							if (broken != null) {
								BreweryPlugin.getInstance().debugLog("Barrel at "
										+ broken.getWorld().getName() + "/" + broken.getX() + "/" + broken.getY() + "/" + broken.getZ()
										+ " has been destroyed unexpectedly, contents will drop");
								// remove the barrel if it was destroyed
								barrel.remove(broken, null, true);
							} else {
								// Dont check this barrel again, its enough to check it once after every restart (and when randomly chosen)
								// as now this is only the backup if we dont register the barrel breaking,
								// for example when removing it with some world editor
								barrel.checked = true;
							}
						}, 1, 1);
						repeat = false;
					}
					check++;
				} else {
					check = 0;
					repeat = false;
					cancel();
				}
			}
		}

	}

}
