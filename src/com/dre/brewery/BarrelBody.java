package com.dre.brewery;

import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.TownyUtil;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The Blocks that make up a Barrel in the World
 */
public class BarrelBody {

	private final Barrel barrel;
	private final Block spigot;
	private BoundingBox bounds;
	private byte signoffset;

	public BarrelBody(Barrel barrel, byte signoffset) {
		this.barrel = barrel;
		this.signoffset = signoffset;
		spigot = barrel.getSpigot();
		this.bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
	}

	/**
	 * Loading from file
	 */
	public BarrelBody(Barrel barrel, byte signoffset, BoundingBox bounds, boolean async) {
		this(barrel, signoffset);

		if (boundsSeemBad(bounds)) {
			if (async) {
				this.bounds = null;
				return;
			}
			// If loading from old data, or block locations are missing, or other error, regenerate BoundingBox
			// This will only be done in those extreme cases.
			regenerateBounds();
		} else {
			this.bounds = bounds;
		}
	}

	public Barrel getBarrel() {
		return barrel;
	}

	public Block getSpigot() {
		return spigot;
	}

	@NotNull
	public BoundingBox getBounds() {
		return bounds;
	}

	public void setBounds(@NotNull BoundingBox bounds) {
		Objects.requireNonNull(bounds);
		this.bounds = bounds;
	}

	public byte getSignoffset() {
		return signoffset;
	}

	public void setSignoffset(byte signoffset) {
		this.signoffset = signoffset;
	}

	/**
	 * If the Sign of a Large Barrel gets destroyed, set signOffset to 0
	 */
	public void destroySign() {
		signoffset = 0;
	}

	/**
	 * Quick check if the bounds are valid or seem corrupt
	 */
	public static boolean boundsSeemBad(BoundingBox bounds) {
		if (bounds == null) return true;
		long area = bounds.area();
		return area > 64 || area < 4;
	}

	/**
	 * direction of the barrel from the spigot
	 */
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

	/**
	 * is this a Large barrel?
	 */
	public boolean isLarge() {
		return barrel.isLarge();
	}

	/**
	 * is this a Small barrel?
	 */
	public boolean isSmall() {
		return barrel.isSmall();
	}

	/**
	 * woodtype of the block the spigot is attached to
	 */
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

	/**
	 * Returns true if this Block is part of this Barrel
	 *
	 * @param block the block to check
	 * @return true if the given block is part of this Barrel
 	 */
	public boolean hasBlock(Block block) {
		if (block != null) {
			if (spigot.equals(block)) {
				return true;
			}
			if (spigot.getWorld().equals(block.getWorld())) {
				return bounds != null && bounds.contains(block.getX(), block.getY(), block.getZ());
			}
		}
		return false;
	}

	/**
	 * Returns true if the Offset of the clicked Sign matches the Barrel.
	 * <p>This prevents adding another sign to the barrel and clicking that.
	 */
	public boolean isSignOfBarrel(byte offset) {
		return offset == 0 || signoffset == 0 || signoffset == offset;
	}

	/**
	 * returns the Sign of a large barrel, the spigot if there is none
	 */
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

	/**
	 * returns the fence above/below a block, itself if there is none
	 */
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

	/**
	 * Regenerate the Barrel Bounds.
	 *
	 * @return true if successful, false if Barrel was broken and should be removed.
	 */
	public boolean regenerateBounds() {
		P.p.log("Regenerating Barrel BoundingBox: " + (bounds == null ? "was null" : "area=" + bounds.area()));
		Block broken = getBrokenBlock(true);
		if (broken != null) {
			barrel.remove(broken, null, true);
			return false;
		}
		return true;
	}

	/**
	 * returns null if Barrel is correctly placed; the block that is missing when not.
	 * <p>the barrel needs to be formed correctly
	 *
	 * @param force to also check even if chunk is not loaded
	 */
	public Block getBrokenBlock(boolean force) {
		return getBrokenBlock(force, null);
	}

	public Block getBrokenBlock(boolean force, Player player) {
		if (force || BUtil.isChunkLoaded(spigot)) {
			//spigot = getSpigotOfSign(spigot);
			if (LegacyUtil.isSign(spigot.getType())) {
				return checkSBarrel(player);
			} else {
				return checkLBarrel(player);
			}
		}
		return null;
	}

	public Block checkSBarrel(Player player) {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		if (direction == 1) {
			startX = 1;
			startZ = -1;
		} else if (direction == 2) {
			startX = -2;
			startZ = 0;
		} else if (direction == 3) {
			startX = 0;
			startZ = 1;
		} else {
			startX = -1;
			startZ = -2;
		}
		endX = startX + 1;
		endZ = startZ + 1;

		Material type;
		int x = startX;
		int y = 0;
		int z = startZ;
		while (y <= 1) {
			while (x <= endX) {
				while (z <= endZ) {
					Block block = spigot.getRelative(x, y, z);
					
					if(!TownyUtil.isInsideTown(block.getLocation(), player)){
						if(player != null)
							P.p.msg(player, P.p.languageReader.get("Towny_BarrelOverreachingBorders"));
						return block;
					}
					
					type = block.getType();

					if (LegacyUtil.isWoodStairs(type)) {
						if (y == 0) {
							// stairs have to be upside down
							if (!LegacyUtil.areStairsInverted(block)) {
								return block;
							}
						}
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
		bounds = new BoundingBox(
			spigot.getX() + startX,
			spigot.getY(),
			spigot.getZ() + startZ,
			spigot.getX() + endX,
			spigot.getY() + 1,
			spigot.getZ() + endZ);
		return null;
	}

	public Block checkLBarrel(Player player) {
		int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
		if (direction == 0) {
			return spigot;
		}
		int startX;
		int startZ;
		int endX;
		int endZ;

		if (direction == 1) {
			startX = 1;
			startZ = -1;
		} else if (direction == 2) {
			startX = -4;
			startZ = -1;
		} else if (direction == 3) {
			startX = -1;
			startZ = 1;
		} else {
			startX = -1;
			startZ = -4;
		}
		if (direction == 1 || direction == 2) {
			endX = startX + 3;
			endZ = startZ + 2;
		} else {
			endX = startX + 2;
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

					if(!TownyUtil.isInsideTown(block.getLocation(), player)){
						if(player != null)
							P.p.msg(player, P.p.languageReader.get("Towny_BarrelOverreachingBorders"));
						return block;
					}

					type = block.getType();
					if (direction == 1 || direction == 2) {
						if (y == 1 && z == 0) {
							z++;
							continue;
						}
					} else {
						if (y == 1 && x == 0) {
							z++;
							continue;
						}
					}
					if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
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
		bounds = new BoundingBox(
			spigot.getX() + startX,
			spigot.getY(),
			spigot.getZ() + startZ,
			spigot.getX() + endX,
			spigot.getY() + 2,
			spigot.getZ() + endZ);

		return null;
	}

	public void save(ConfigurationSection config, String prefix) {
		if (signoffset != 0) {
			config.set(prefix + ".sign", signoffset);
		}
		config.set(prefix + ".bounds", bounds.serialize());
	}
}
