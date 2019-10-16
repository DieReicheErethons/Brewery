package com.dre.brewery;

import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelCreateEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.integration.LogBlockBarrel;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Barrel implements InventoryHolder {

	public static CopyOnWriteArrayList<Barrel> barrels = new CopyOnWriteArrayList<>(); // TODO find best collection
	private static int check = 0;

	private final Block spigot;
	private final BarrelBody body; // The Blocks that make up a Barrel in the World
	private boolean checked; // Checked by the random BarrelCheck routine
	private Inventory inventory;
	private float time;

	public Barrel(Block spigot, byte signoffset) {
		this.spigot = spigot;
		body = new BarrelBody(this, signoffset);
	}

	// load from file
	public Barrel(Block spigot, byte sign, String[] st, String[] wo, Map<String, Object> items, float time) {
		this.spigot = spigot;
		if (isLarge()) {
			this.inventory = P.p.getServer().createInventory(this, 27, P.p.languageReader.get("Etc_Barrel"));
		} else {
			this.inventory = P.p.getServer().createInventory(this, 9, P.p.languageReader.get("Etc_Barrel"));
		}
		if (items != null) {
			for (String slot : items.keySet()) {
				if (items.get(slot) instanceof ItemStack) {
					this.inventory.setItem(P.p.parseInt(slot), (ItemStack) items.get(slot));
				}
			}
		}
		this.time = time;

		body = new BarrelBody(this, sign, st, wo);
	}

	public static void onUpdate() {
		for (Barrel barrel : barrels) {
			// Minecraft day is 20 min, so add 1/20 to the time every minute
			barrel.time += (1.0 / 20.0);
		}
		if (check == 0 && barrels.size() > 0) {
			Barrel random = barrels.get((int) Math.floor(Math.random() * barrels.size()));
			if (random != null) {
				// You have been selected for a random search
				// We want to check at least one barrel every time
				random.checked = false;
			}
			new BarrelCheck().runTaskTimer(P.p, 1, 1);
		}
	}

	public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
		if (isLarge()) {
			if (!player.hasPermission("brewery.openbarrel.big")) {
				P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		} else {
			if (!player.hasPermission("brewery.openbarrel.small")) {
				P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		}

		// Call event
		BarrelAccessEvent accessEvent = new BarrelAccessEvent(this, player, event.getClickedBlock());
		// Listened to by WGBarrel7, WGBarrelNew, WGBarrelOld, GriefPreventionBarrel (IntegrationListener)
		P.p.getServer().getPluginManager().callEvent(accessEvent);
		if (accessEvent.isCancelled()) {
			P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		return true;
	}

	// Ask for permission to destroy barrel
	public boolean hasPermsDestroy(Player player, Block block, BarrelDestroyEvent.Reason reason) {
		// Listened to by LWCBarrel (IntegrationListener)
		BarrelDestroyEvent destroyEvent = new BarrelDestroyEvent(this, block, reason, player);
		P.p.getServer().getPluginManager().callEvent(destroyEvent);
		return !destroyEvent.isCancelled();
	}

	// player opens the barrel
	public void open(Player player) {
		if (inventory == null) {
			if (isLarge()) {
				inventory = P.p.getServer().createInventory(this, 27, P.p.languageReader.get("Etc_Barrel"));
			} else {
				inventory = P.p.getServer().createInventory(this, 9, P.p.languageReader.get("Etc_Barrel"));
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
						P.p.debugLog("opening Barrel with potions (" + ftime + "ms)");
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
				P.p.errorLog("Failed to Log Barrel to LogBlock!");
				P.p.errorLog("Brewery was tested with version 1.94 of LogBlock!");
				e.printStackTrace();
			}
		}
		player.openInventory(inventory);
	}

	public void playOpeningSound() {
		float randPitch = (float) (Math.random() * 0.1);
		Location location = getSpigot().getLocation();
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
		if (isLarge()) {
			location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.5f + randPitch);
			location.getWorld().playSound(location, Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.2f, 0.6f + randPitch);
		} else {
			location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
		}
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	public Block getSpigot() {
		return spigot;
	}

	public BarrelBody getBody() {
		return body;
	}

	public float getTime() {
		return time;
	}

	// Returns true if this Block is part of this Barrel
	public boolean hasBlock(Block block) {
		return body.hasBlock(block);
	}

	public boolean hasWoodBlock(Block block) {
		return body.hasWoodBlock(block);
	}

	public boolean hasStairsBlock(Block block) {
		return body.hasStairsBlock(block);
	}

	// Get the Barrel by Block, null if that block is not part of a barrel
	public static Barrel get(Block block) {
		if (block == null) {
			return null;
		}
		Material type = block.getType();
		if (LegacyUtil.isFence(type) || LegacyUtil.isSign(type) ) {
			return getBySpigot(block);
		} else if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
			return getByWood(block);
		}
		return null;
	}

	// Get the Barrel by Sign or Spigot (Fastest)
	public static Barrel getBySpigot(Block sign) {
		// convert spigot if neccessary
		Block spigot = BarrelBody.getSpigotOfSign(sign);

		byte signoffset = 0;
		if (!spigot.equals(sign)) {
			signoffset = (byte) (sign.getY() - spigot.getY());
		}

		for (Barrel barrel : barrels) {
			if (barrel.body.isSignOfBarrel(signoffset)) {
				if (barrel.body.equals(spigot)) {
					if (barrel.body.getSignoffset() == 0 && signoffset != 0) {
						// Barrel has no signOffset even though we clicked a sign, may be old
						barrel.body.setSignoffset(signoffset);
					}
					return barrel;
				}
			}
		}
		return null;
	}

	// Get the barrel by its corpus (Wood Planks, Stairs)
	public static Barrel getByWood(Block wood) {
		if (LegacyUtil.isWoodPlanks(wood.getType())) {
			for (Barrel barrel : barrels) {
				if (barrel.body.hasWoodBlock(wood)) {
					return barrel;
				}
			}
		} else if (LegacyUtil.isWoodStairs(wood.getType())) {
			for (Barrel barrel : Barrel.barrels) {
				if (barrel.body.hasStairsBlock(wood)) {
					return barrel;
				}
			}
		}
		return null;
	}

	// creates a new Barrel out of a sign
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
						P.p.msg(player, P.p.languageReader.get("Perms_NoSmallBarrelCreate"));
						return false;
					}
				} else {
					if (!player.hasPermission("brewery.createbarrel.big")) {
						P.p.msg(player, P.p.languageReader.get("Perms_NoBigBarrelCreate"));
						return false;
					}
				}
				BarrelCreateEvent createEvent = new BarrelCreateEvent(barrel, player);
				P.p.getServer().getPluginManager().callEvent(createEvent);
				if (!createEvent.isCancelled()) {
					barrels.add(barrel);
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

	// removes a barrel, throwing included potions to the ground
	public void remove(Block broken, Player breaker) {
		BarrelRemoveEvent event = new BarrelRemoveEvent(this);
		// Listened to by LWCBarrel (IntegrationListener)
		P.p.getServer().getPluginManager().callEvent(event);

		if (inventory != null) {
			List<HumanEntity> viewers = new ArrayList(inventory.getViewers());
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
					P.p.errorLog("Failed to Log Barrel-break to LogBlock!");
					P.p.errorLog("Brewery was tested with version 1.94 of LogBlock!");
					e.printStackTrace();
				}
			}
			if (event.willItemsDrop()) {
				for (ItemStack item : items) {
					if (item != null) {
						Brew brew = Brew.get(item);
						if (brew != null) {
							// Brew before throwing
							brew.age(item, time, body.getWood());
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

	// is this a Large barrel?
	public boolean isLarge() {
		return !isSmall();
	}

	// is this a Small barrel?
	public boolean isSmall() {
		return !LegacyUtil.isSign(spigot.getType());
	}

	// returns the Sign of a large barrel, the spigot if there is none
	public Block getSignOfSpigot() {
		return body.getSignOfSpigot();
	}

	// returns the fence above/below a block, itself if there is none
	public static Block getSpigotOfSign(Block block) {
		return BarrelBody.getSpigotOfSign(block);
	}

	// returns null if Barrel is correctly placed; the block that is missing when not
	// the barrel needs to be formed correctly
	// flag force to also check if chunk is not loaded
	public Block getBrokenBlock(boolean force) {
		return body.getBrokenBlock(force);
	}

	//unloads barrels that are in a unloading world
	public static void onUnload(String name) {
		for (Barrel barrel : barrels) {
			if (barrel.spigot.getWorld().getName().equals(name)) {
				barrels.remove(barrel);
			}
		}
	}

	// Saves all data
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
		if (oldData != null){
			for (String uuid : oldData.getKeys(false)) {
				if (!config.contains(uuid)) {
					config.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

	public static class BarrelCheck extends BukkitRunnable {
		@Override
		public void run() {
			boolean repeat = true;
			while (repeat) {
				if (check < barrels.size()) {
					Barrel barrel = barrels.get(check);
					if (!barrel.checked) {
						Block broken = barrel.body.getBrokenBlock(false);
						if (broken != null) {
							P.p.debugLog("Barrel at " + broken.getWorld().getName() + "/" + broken.getX() + "/" + broken.getY() + "/" + broken.getZ()
									+ " has been destroyed unexpectedly, contents will drop");
							// remove the barrel if it was destroyed
							barrel.remove(broken, null);
						} else {
							// Dont check this barrel again, its enough to check it once after every restart
							// as now this is only the backup if we dont register the barrel breaking, as sample
							// when removing it with some world editor
							barrel.checked = true;
						}
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
