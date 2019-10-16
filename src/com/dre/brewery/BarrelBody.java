package com.dre.brewery;

import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

// The Blocks that make up a Barrel in the World
public class BarrelBody {

	private final Barrel barrel;
	private final Block spigot;
	private int[] woodsloc = null; // location of wood Blocks
	private int[] stairsloc = null; // location of stair Blocks
	private byte signoffset;

	public BarrelBody(Barrel barrel, byte signoffset) {
		this.barrel = barrel;
		this.signoffset = signoffset;
		spigot = barrel.getSpigot();
	}

	// Loading from file
	public BarrelBody(Barrel barrel, byte signoffset, String[] st, String[] wo) {
		this(barrel, signoffset);

		int i = 0;
		if (wo.length > 1) {
			woodsloc = new int[wo.length];
			for (String wos : wo) {
				woodsloc[i] = P.p.parseInt(wos);
				i++;
			}
			i = 0;
		}
		if (st.length > 1) {
			stairsloc = new int[st.length];
			for (String sts : st) {
				stairsloc[i] = P.p.parseInt(sts);
				i++;
			}
		}

		if (woodsloc == null && stairsloc == null) {
			// If loading from old data, or block locations are missing, regenerate them
			// This will only be done in those extreme cases.
			Block broken = getBrokenBlock(true);
			if (broken != null) {
				barrel.remove(broken, null);
			}
		}
	}

	public Barrel getBarrel() {
		return barrel;
	}

	public Block getSpigot() {
		return spigot;
	}

	public int[] getWoodsloc() {
		return woodsloc;
	}

	public void setWoodsloc(int[] woodsloc) {
		this.woodsloc = woodsloc;
	}

	public int[] getStairsloc() {
		return stairsloc;
	}

	public void setStairsloc(int[] stairsloc) {
		this.stairsloc = stairsloc;
	}

	public byte getSignoffset() {
		return signoffset;
	}

	public void setSignoffset(byte signoffset) {
		this.signoffset = signoffset;
	}

	// If the Sign of a Large Barrel gets destroyed, set signOffset to 0
	public void destroySign() {
		signoffset = 0;
	}

	// direction of the barrel from the spigot
	public static int getDirection(Block spigot) {
		int direction = 0;// 1=x+ 2=x- 3=z+ 4=z-
		Material type = spigot.getRelative(0, 0, 1).getType();
		if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
			direction = 3;
		}
		type = spigot.getRelative(0, 0, -1).getType();
		if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
			if (direction == 0) {
				direction = 4;
			} else {
				return 0;
			}
		}
		type = spigot.getRelative(1, 0, 0).getType();
		if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
			if (direction == 0) {
				direction = 1;
			} else {
				return 0;
			}
		}
		type = spigot.getRelative(-1, 0, 0).getType();
		if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
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
		return barrel.isLarge();
	}

	// is this a Small barrel?
	public boolean isSmall() {
		return barrel.isSmall();
	}

	// woodtype of the block the spigot is attached to
	public byte getWood() {
		Block wood;
		switch (getDirection(spigot)) { // 1=x+ 2=x- 3=z+ 4=z-
			case 0:
				return 0;
			case 1:
				wood = spigot.getRelative(1, 0, 0);
				break;
			case 2:
				wood = spigot.getRelative(-1, 0, 0);
				break;
			case 3:
				wood = spigot.getRelative(0, 0, 1);
				break;
			default:
				wood = spigot.getRelative(0, 0, -1);
		}
		try {
			return LegacyUtil.getWoodType(wood);
		} catch (NoSuchFieldError | NoClassDefFoundError noSuchFieldError) {
			// Using older minecraft versions some fields and classes do not exist
			return 0;
		}
	}

	// Returns true if this Block is part of this Barrel
	public boolean hasBlock(Block block) {
		if (block != null) {
			if (LegacyUtil.isWoodPlanks(block.getType())) {
				return hasWoodBlock(block);
			} else if (LegacyUtil.isWoodStairs(block.getType())) {
				return hasStairsBlock(block);
			}
		}
		return false;
	}

	public boolean hasWoodBlock(Block block) {
		if (woodsloc != null) {
			if (spigot.getWorld() != null && spigot.getWorld().equals(block.getWorld())) {
				if (woodsloc.length > 2) {
					int x = block.getX();
					if (Math.abs(x - woodsloc[0]) < 10) {
						for (int i = 0; i < woodsloc.length - 2; i += 3) {
							if (woodsloc[i] == x) {
								if (woodsloc[i + 1] == block.getY()) {
									if (woodsloc[i + 2] == block.getZ()) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public boolean hasStairsBlock(Block block) {
		if (stairsloc != null) {
			if (spigot.getWorld() != null && spigot.getWorld().equals(block.getWorld())) {
				if (stairsloc.length > 2) {
					int x = block.getX();
					if (Math.abs(x - stairsloc[0]) < 10) {
						for (int i = 0; i < stairsloc.length - 2; i += 3) {
							if (stairsloc[i] == x) {
								if (stairsloc[i + 1] == block.getY()) {
									if (stairsloc[i + 2] == block.getZ()) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	// Returns true if the Offset of the clicked Sign matches the Barrel.
	// This prevents adding another sign to the barrel and clicking that.
	public boolean isSignOfBarrel(byte offset) {
		return offset == 0 || signoffset == 0 || signoffset == offset;
	}

	// returns the Sign of a large barrel, the spigot if there is none
	public Block getSignOfSpigot() {
		if (signoffset != 0) {
			if (LegacyUtil.isSign(spigot.getType())) {
				return spigot;
			}

			if (LegacyUtil.isSign(spigot.getRelative(0, signoffset, 0).getType())) {
				return spigot.getRelative(0, signoffset, 0);
			} else {
				signoffset = 0;
			}
		}
		return spigot;
	}

	// returns the fence above/below a block, itself if there is none
	public static Block getSpigotOfSign(Block block) {

		int y = -2;
		while (y <= 1) {
			// Fence and Netherfence
			Block relative = block.getRelative(0, y, 0);
			if (LegacyUtil.isFence(relative.getType())) {
				return (relative);
			}
			y++;
		}
		return block;
	}

	// returns null if Barrel is correctly placed; the block that is missing when not
	// the barrel needs to be formed correctly
	// flag force to also check if chunk is not loaded
	public Block getBrokenBlock(boolean force) {
		if (force || BUtil.isChunkLoaded(spigot)) {
			//spigot = getSpigotOfSign(spigot);
			if (LegacyUtil.isSign(spigot.getType())) {
				return checkSBarrel();
			} else {
				return checkLBarrel();
			}
		}
		return null;
	}

	public Block checkSBarrel() {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		ArrayList<Integer> stairs = new ArrayList<>();

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

		Material type;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 1) {
			while (x <= endX) {
				while (z <= endZ) {
					Block block = spigot.getRelative(x, y, z);
					type = block.getType();

					if (LegacyUtil.isWoodStairs(type)) {
						if (y == 0) {
							// stairs have to be upside down
							if (!LegacyUtil.areStairsInverted(block)) {
								return block;
							}
						}
						stairs.add(block.getX());
						stairs.add(block.getY());
						stairs.add(block.getZ());
						z++;
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
		stairsloc = ArrayUtils.toPrimitive(stairs.toArray(new Integer[0]));
		return null;
	}

	public Block checkLBarrel() {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		ArrayList<Integer> stairs = new ArrayList<>();
		ArrayList<Integer> woods = new ArrayList<>();

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

		Material type;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 2) {
			while (x <= endX) {
				while (z <= endZ) {
					Block block = spigot.getRelative(x, y, z);
					type = block.getType();
					if (direction == 1 || direction == 2) {
						if (y == 1 && z == 0) {
							if (x == -1 || x == -4 || x == 1 || x == 4) {
								woods.add(block.getX());
								woods.add(block.getY());
								woods.add(block.getZ());
							}
							z++;
							continue;
						}
					} else {
						if (y == 1 && x == 0) {
							if (z == -1 || z == -4 || z == 1 || z == 4) {
								woods.add(block.getX());
								woods.add(block.getY());
								woods.add(block.getZ());
							}
							z++;
							continue;
						}
					}
					if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
						if (LegacyUtil.isWoodPlanks(type)) {
							woods.add(block.getX());
							woods.add(block.getY());
							woods.add(block.getZ());
						} else {
							stairs.add(block.getX());
							stairs.add(block.getY());
							stairs.add(block.getZ());
						}
						z++;
					} else {
						return block;
					}
				}
				z = startZ;
				x++;
			}
			z = startZ;
			x = startX;
			y++;
		}
		stairsloc = ArrayUtils.toPrimitive(stairs.toArray(new Integer[0]));
		woodsloc = ArrayUtils.toPrimitive(woods.toArray(new Integer[0]));

		return null;
	}

	public void save(ConfigurationSection config, String prefix) {
		if (signoffset != 0) {
			config.set(prefix + ".sign", signoffset);
		}
		if (stairsloc != null && stairsloc.length > 0) {
			StringBuilder st = new StringBuilder();
			for (int i : stairsloc) {
				st.append(i).append(",");
			}
			config.set(prefix + ".st", st.substring(0, st.length() - 1));
		}
		if (woodsloc != null && woodsloc.length > 0) {
			StringBuilder wo = new StringBuilder();
			for (int i : woodsloc) {
				wo.append(i).append(",");
			}
			config.set(prefix + ".wo", wo.substring(0, wo.length() - 1));
		}
	}
}
