package com.dre.brewery;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.material.Cauldron;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wood;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("JavaReflectionMemberAccess")
public class LegacyUtil {

	private static Method GET_MATERIAL;
	private static Method GET_BLOCK_TYPE_ID_AT;
	private static Method SET_DATA;

	static {
		// <= 1.12.2 methods
		// These will be rarely used
		try {
			GET_MATERIAL = Material.class.getDeclaredMethod("getMaterial", int.class);
			GET_BLOCK_TYPE_ID_AT = World.class.getDeclaredMethod("getBlockTypeIdAt", Location.class);
			SET_DATA = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".block.CraftBlock").getDeclaredMethod("setData", byte.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException ignored) {
		}

		List<Material> planks = new ArrayList<>(6);
		for (Material m : Material.values()) {
			if (m.name().endsWith("PLANKS")) {
				planks.add(m);
			}
		}
		PLANKS = planks;

		List<Material> fences = new ArrayList<>(7);
		for (Material m : Material.values()) {
			if (m.name().endsWith("FENCE")) {
				fences.add(m);
			}
		}
		FENCES = fences;
	}

	public static final Material CLOCK = get("CLOCK", "WATCH");
	public static final Material OAK_STAIRS = get("OAK_STAIRS", "WOOD_STAIRS");
	public static final Material SPRUCE_STAIRS = get("SPRUCE_STAIRS", "SPRUCE_WOOD_STAIRS");
	public static final Material BIRCH_STAIRS = get("BIRCH_STAIRS", "BIRCH_WOOD_STAIRS");
	public static final Material JUNGLE_STAIRS = get("JUNGLE_STAIRS", "JUNGLE_WOOD_STAIRS");
	public static final Material ACACIA_STAIRS = get("ACACIA_STAIRS");
	public static final Material DARK_OAK_STAIRS = get("DARK_OAK_STAIRS");
	public static final List<Material> PLANKS;
	public static final List<Material> FENCES;

	// Materials removed in 1.13
	public static final Material STATIONARY_LAVA = get("STATIONARY_LAVA");
	public static final Material SIGN_POST = get("SIGN_POST");
	public static final Material WOOD = get("WOOD");

	private static Material get(String name) {
		try {
			return Material.valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Material get(String newName, String oldName) {
		try {
			return Material.valueOf(P.use1_13 ? newName : oldName);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static boolean isWoodPlanks(Material type) {
		return (WOOD != null && type == WOOD) || PLANKS.contains(type);
	}

	public static boolean isWoodStairs(Material type) {
		return type == OAK_STAIRS || type == SPRUCE_STAIRS || type == BIRCH_STAIRS || type == JUNGLE_STAIRS
				|| (type == ACACIA_STAIRS && ACACIA_STAIRS != null) || (type == DARK_OAK_STAIRS && DARK_OAK_STAIRS != null);
	}

	public static boolean isFence(Material type) {
		return FENCES.contains(type);
	}

	public static boolean isSign(Material type) {
		return type == Material.SIGN || type == Material.WALL_SIGN || (!P.use1_13 && type == SIGN_POST);
	}

	// LAVA and STATIONARY_LAVA are merged as of 1.13
	public static boolean isLava(Material type) {
		return type == Material.LAVA || (!P.use1_13 && type == STATIONARY_LAVA);
	}

	public static boolean areStairsInverted(Block block) {
		if (!P.use1_13) {
			@SuppressWarnings("deprecation")
			MaterialData data = block.getState().getData();
			return data instanceof org.bukkit.material.Stairs && (((org.bukkit.material.Stairs) data).isInverted());
		} else {
			BlockData data = block.getBlockData();
			return data instanceof org.bukkit.block.data.type.Stairs && ((org.bukkit.block.data.type.Stairs) data).getHalf() == org.bukkit.block.data.type.Stairs.Half.TOP;
		}
	}

	public static byte getWoodType(Block wood) throws NoSuchFieldError, NoClassDefFoundError {
		TreeSpecies woodType;

		if (P.use1_13 || isWoodStairs(wood.getType())) {
			String material = wood.getType().name();
			if (material.startsWith("OAK")) {
				woodType = TreeSpecies.GENERIC;
			} else if (material.startsWith("SPRUCE")) {
				woodType = TreeSpecies.REDWOOD;
			} else if (material.startsWith("BIRCH")) {
				woodType = TreeSpecies.BIRCH;
			} else if (material.startsWith("JUNGLE")) {
				woodType = TreeSpecies.JUNGLE;
			} else if (material.startsWith("ACACIA")) {
				woodType = TreeSpecies.ACACIA;
			} else if (material.startsWith("DARK_OAK")) {
				woodType = TreeSpecies.DARK_OAK;
			} else {
				return 0;
			}

		} else {
			@SuppressWarnings("deprecation")
			MaterialData data = wood.getState().getData();
			if (data instanceof Tree) {
				woodType = ((Tree) data).getSpecies();
			} else if (data instanceof Wood) {
				woodType = ((Wood) data).getSpecies();
			} else {
				return 0;
			}
		}

		switch (woodType) {
			case GENERIC:
				return 2;
			case REDWOOD:
				return 4;
			case BIRCH:
				return 1;
			case JUNGLE:
				return 3;
			case ACACIA:
				return 5;
			case DARK_OAK:
				return 6;
			default:
				return 0;
		}
	}

	// 0 = empty, 1 = something in, 2 = full
	public static byte getFillLevel(Block block) {
		if (block.getType() != Material.CAULDRON) {
			return 0;
		}

		if (P.use1_13) {
			Levelled cauldron = ((Levelled) block.getBlockData());
			if (cauldron.getLevel() == 0) {
				return 0;
			} else if (cauldron.getLevel() == cauldron.getMaximumLevel()) {
				return 2;
			} else {
				return 1;
			}

		} else {
			Cauldron cauldron = (Cauldron) block.getState().getData();
			if (cauldron.isEmpty()) {
				return 0;
			} else if (cauldron.isFull()) {
				return 2;
			} else {
				return 1;
			}
		}
	}

	/*
	 * only used to convert a very old Datafile or config from a very old version
	 */
	public static Material getMaterial(int id) {
		try {
			return GET_MATERIAL != null ? (Material) GET_MATERIAL.invoke(null, id) : null;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return null;
		}
	}

	// Only used for very old versions of LogBlock
	public static int getBlockTypeIdAt(Location location) {
		try {
			return GET_BLOCK_TYPE_ID_AT != null ? (int) GET_BLOCK_TYPE_ID_AT.invoke(location.getWorld(), location) : 0;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return 0;
		}
	}

	// Setting byte data to blocks for older versions
	public static void setData(Block block, byte data) {
		try {
			SET_DATA.invoke(block, data);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
		}
	}

}
