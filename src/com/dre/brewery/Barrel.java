package com.dre.brewery;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.configuration.ConfigurationSection;

public class Barrel {

	public static CopyOnWriteArrayList<Barrel> barrels = new CopyOnWriteArrayList<Barrel>();

	// private CopyOnWriteArrayList<Brew> brews = new
	// CopyOnWriteArrayList<Brew>();
	private Block spigot;
	private Inventory inventory;
	private float time;

	public Barrel(Block spigot) {
		this.spigot = spigot;
	}

	// load from file
	public Barrel(Block spigot, Map<String, Object> items, float time) {
		this.spigot = spigot;
		if (isLarge()) {
			this.inventory = org.bukkit.Bukkit.createInventory(null, 27, "Fass");
		} else {
			this.inventory = org.bukkit.Bukkit.createInventory(null, 9, "Fass");
		}
		for (String slot : items.keySet()) {
			if (items.get(slot) instanceof ItemStack) {
				this.inventory.setItem(P.p.parseInt(slot), (ItemStack) items.get(slot));
			}
		}
		this.time = time;
		barrels.add(this);
	}

	// load from file (without inventory)
	public Barrel(Block spigot, float time) {
		this.spigot = spigot;
		if (isLarge()) {
			this.inventory = org.bukkit.Bukkit.createInventory(null, 27, "Fass");
		} else {
			this.inventory = org.bukkit.Bukkit.createInventory(null, 9, "Fass");
		}
		this.time = time;
		barrels.add(this);
	}

	public static void onUpdate() {
		Block broken;
		for (Barrel barrel : barrels) {
			broken = getBrokenBlock(barrel.spigot);
			// remove the barrel if it was destroyed
			if (broken != null) {
				barrel.remove(broken);
			} else {
				// Minecraft day is 20 min, so add 1/20 to the time every minute
				barrel.time += 1.0 / 20.0;
			}
		}
	}

	// player opens the barrel
	public void open(Player player) {
		if (inventory == null) {
			if (isLarge()) {
				inventory = org.bukkit.Bukkit.createInventory(null, 27, "Fass");
			} else {
				inventory = org.bukkit.Bukkit.createInventory(null, 9, "Fass");
			}
		} else {
			if (time > 0) {
				// if nobody has the inventory opened
				if (inventory.getViewers().isEmpty()) {
					// if inventory contains potions
					if (inventory.contains(373)) {
						byte wood = getWood();
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
		player.openInventory(inventory);
	}

	public static Barrel get(Block spigot) {
		// convert spigot if neccessary
		spigot = getSpigotOfSign(spigot);
		for (Barrel barrel : barrels) {
			if (barrel.spigot.equals(spigot)) {
				return barrel;
			}
		}
		return null;
	}

	// creates a new Barrel out of a sign
	public static boolean create(Block spigot) {
		spigot = getSpigotOfSign(spigot);
		if (getBrokenBlock(spigot) == null) {
			if (get(spigot) == null) {
				barrels.add(new Barrel(spigot));
				return true;
			}
		}
		return false;
	}

	// removes a barrel, throwing included potions to the ground
	public void remove(Block broken) {
		if (inventory != null) {
			ItemStack[] items = inventory.getContents();
			for (ItemStack item : items) {
				if (item != null) {
					Brew brew = Brew.get(item);
					if (brew != null) {
						// Brew before throwing
						brew.age(item, time, getWood());
						PotionMeta meta = (PotionMeta) item.getItemMeta();
						if (Brew.hasColorLore(meta)) {
							brew.convertLore(meta, false);
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
		barrels.remove(this);
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
		P.p.createWorldSections(config);

		if (!barrels.isEmpty()) {
			int id = 0;
			for (Barrel barrel : barrels) {

				String worldName = barrel.spigot.getWorld().getName();
				String prefix = null;

				if (worldName.startsWith("DXL_")) {
					prefix = P.p.getDxlName(worldName) + "." + id;
				} else {
					prefix = barrel.spigot.getWorld().getUID().toString() + "." + id;
				}

				// block: x/y/z
				config.set(prefix + ".spigot", barrel.spigot.getX() + "/" + barrel.spigot.getY() + "/" + barrel.spigot.getZ());

				if (barrel.inventory != null) {
					int slot = 0;
					ItemStack item = null;
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

	// direction of the barrel from the spigot
	public static int getDirection(Block spigot) {
		int direction = 0;// 1=x+ 2=x- 3=z+ 4=z-
		int typeId = spigot.getRelative(0, 0, 1).getTypeId();
		if (typeId == 5 || typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
			direction = 3;
		}
		typeId = spigot.getRelative(0, 0, -1).getTypeId();
		if (typeId == 5 || typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
			if (direction == 0) {
				direction = 4;
			} else {
				return 0;
			}
		}
		typeId = spigot.getRelative(1, 0, 0).getTypeId();
		if (typeId == 5 || typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
			if (direction == 0) {
				direction = 1;
			} else {
				return 0;
			}
		}
		typeId = spigot.getRelative(-1, 0, 0).getTypeId();
		if (typeId == 5 || typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
			if (direction == 0) {
				direction = 2;
			} else {
				return 0;
			}
		}
		return direction;
	}

	// is this a Large barrel?
	public boolean isLarge() {
		if (spigot.getTypeId() == 63 || spigot.getTypeId() == 68) {
			return false;
		}
		return true;
	}

	// true for small barrels
	public static boolean isSign(Block spigot) {
		if (spigot.getTypeId() == 63 || spigot.getTypeId() == 68) {
			return true;
		}
		return false;
	}

	// woodtype of the block the spigot is attached to
	public byte getWood() {
		int direction = getDirection(this.spigot);// 1=x+ 2=x- 3=z+ 4=z-
		Block wood = null;
		if (direction == 0) {
			return 0;
		} else if (direction == 1) {
			wood = this.spigot.getRelative(1, 0, 0);
		} else if (direction == 2) {
			wood = this.spigot.getRelative(-1, 0, 0);
		} else if (direction == 3) {
			wood = this.spigot.getRelative(0, 0, 1);
		} else {
			wood = this.spigot.getRelative(0, 0, -1);
		}
		if (wood.getTypeId() == 5) {
			byte data = wood.getData();
			if (data == 0x0) {
				return 2;
			} else if (data == 0x1) {
				return 4;
			} else if (data == 0x2) {
				return 1;
			} else {
				return 3;
			}
		}
		if (wood.getTypeId() == 53) {
			return 2;
		}
		if (wood.getTypeId() == 134) {
			return 4;
		}
		if (wood.getTypeId() == 135) {
			return 1;
		}
		if (wood.getTypeId() == 136) {
			return 3;
		}
		return 0;
	}

	// returns the fence above/below a block, itself if there is none
	public static Block getSpigotOfSign(Block block) {

		int y = -2;
		while (y <= 1) {
			// Fence and Netherfence
			if (block.getRelative(0, y, 0).getTypeId() == 85 || block.getRelative(0, y, 0).getTypeId() == 113) {
				return (block.getRelative(0, y, 0));
			}
			y++;
		}
		return block;
	}

	// returns null if Barrel is correctly placed; the block that is missing
	// when not
	// the barrel needs to be formed correctly
	public static Block getBrokenBlock(Block spigot) {
		if (spigot.getChunk().isLoaded()) {
			spigot = getSpigotOfSign(spigot);
			if (isSign(spigot)) {
			return checkSBarrel(spigot);
			} else {
			return checkLBarrel(spigot);
			}
		}
		return null;
	}

	public static Block checkSBarrel(Block spigot) {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX = 0;
		int startZ = 0;
		int endX;
		int endZ;

		if (direction == 1) {
			startX = 1;
			endX = startX + 1;
			startZ = -1;
			endZ = 0;
		} else if (direction == 2) {
			startX = -2;
			endX = startX + 1;
			startZ = 0;
			endZ = 1;
		} else if (direction == 3) {
			startX = 0;
			endX = 1;
			startZ = 1;
			endZ = startZ + 1;
		} else {
			startX = -1;
			endX = 0;
			startZ = -2;
			endZ = startZ + 1;
		}

		int typeId;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 1) {
			while (x <= endX) {
				while (z <= endZ) {
					typeId = spigot.getRelative(x, y, z).getTypeId();

					if (typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
						if (y == 0) {
							// stairs have to be upside down
							if (spigot.getRelative(x, y, z).getData() < 4) {
								return spigot.getRelative(x, y, z);
							}
						}
						z++;
						continue;
					} else {
						return spigot.getRelative(x, y, z);
					}
				}
				z = startZ;
				x++;
			}
			z = startZ;
			x = startX;
			y++;
		}
		return null;

	}

	public static Block checkLBarrel(Block spigot) {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX = 0;
		int startZ = 0;
		int endX;
		int endZ;

		if (direction == 1) {
			startX = 1;
			endX = startX + 3;
			startZ = -1;
			endZ = 1;
		} else if (direction == 2) {
			startX = -4;
			endX = startX + 3;
			startZ = -1;
			endZ = 1;
		} else if (direction == 3) {
			startX = -1;
			endX = 1;
			startZ = 1;
			endZ = startZ + 3;
		} else {
			startX = -1;
			endX = 1;
			startZ = -4;
			endZ = startZ + 3;
		}

		int typeId;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 2) {
			while (x <= endX) {
				while (z <= endZ) {
					typeId = spigot.getRelative(x, y, z).getTypeId();
					if (direction == 1 || direction == 2) {
						if (y == 1 && z == 0) {
							if (x == -2 || x == -3 || x == 2 || x == 3) {
								z++;
								continue;
							} else if (x == -1 || x == -4 || x == 1 || x == 4) {
								if (typeId != 0) {
									z++;
									continue;
								}
							}
						}
					} else {
						if (y == 1 && x == 0) {
							if (z == -2 || z == -3 || z == 2 || z == 3) {
								z++;
								continue;
							} else if (z == -1 || z == -4 || z == 1 || z == 4) {
								if (typeId != 0) {
									z++;
									continue;
								}
							}
						}
					}
					if (typeId == 5 || typeId == 53 || typeId == 134 || typeId == 135 || typeId == 136) {
						z++;
						continue;
					} else {
						return spigot.getRelative(x, y, z);
					}
				}
				z = startZ;
				x++;
			}
			z = startZ;
			x = startX;
			y++;
		}
		return null;

	}

}
