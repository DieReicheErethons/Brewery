package com.dre.brewery;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class LegacyUtil {

    private static Method GET_MATERIAL;
    private static Method GET_BLOCK_TYPE_ID_AT;
    private static Method SET_DATA;

    static {
        try {
            GET_MATERIAL = Material.class.getDeclaredMethod("getMaterial", int.class);
            GET_BLOCK_TYPE_ID_AT = World.class.getDeclaredMethod("getBlockTypeIdAt", Location.class);
            SET_DATA = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".block.CraftBlock").getDeclaredMethod("setData", byte.class);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
        }
    }

    public static final Material CLOCK = get("CLOCK", "WATCH");
    public static final Material OAK_STAIRS = get("OAK_STAIRS", "WOOD_STAIRS");
    public static final Material SPRUCE_STAIRS = get("SPRUCE_STAIRS", "SPRUCE_WOOD_STAIRS");
    public static final Material BIRCH_STAIRS = get("BIRCH_STAIRS", "BIRCH_WOOD_STAIRS");
    public static final Material JUNGLE_STAIRS = get("JUNGLE_STAIRS", "JUNGLE_WOOD_STAIRS");
    public static final Material ACACIA_STAIRS = get("ACACIA_STAIRS");
    public static final Material DARK_OAK_STAIRS = get("DARK_OAK_STAIRS");

    private static Material get(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Material get(String oldName, String newName) {
        try {
            return Material.valueOf(P.use1_13 ? newName : oldName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean isWoodPlanks(Material type) {
        return type.name().contains("PLANKS") || type.name().equals("WOOD");
    }

    public static boolean isWoodStairs(Material type) {
        return type == OAK_STAIRS || type == SPRUCE_STAIRS || type == BIRCH_STAIRS || type == JUNGLE_STAIRS
                || (type == ACACIA_STAIRS && ACACIA_STAIRS != null) || (type == DARK_OAK_STAIRS && DARK_OAK_STAIRS != null);
    }

    public static boolean isFence(Material type) {
        return type.name().endsWith("FENCE");
    }

    public static boolean isSign(Material type) {
        return type.name().equals("SIGN_POST") || type == Material.SIGN || type == Material.WALL_SIGN;
    }

    // LAVA and STATIONARY_LAVA are merged as of 1.13
    public static boolean isLava(Material type) {
        return type.name().equals("STATIONARY_LAVA") || type == Material.LAVA;
    }

    public static boolean areStairsInverted(Block block) {
        if (P.use1_13) {
            MaterialData data = block.getState().getData();
            return data instanceof org.bukkit.material.Stairs && (((org.bukkit.material.Stairs) data).isInverted());
        } else {
            BlockData data = block.getBlockData();
            return data instanceof org.bukkit.block.data.type.Stairs && ((org.bukkit.block.data.type.Stairs) data).getHalf() == org.bukkit.block.data.type.Stairs.Half.TOP;
        }
    }

    public static byte getWoodType(Block wood) {
        TreeSpecies woodType = null;

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
            }

        } else {
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
            Levelled cauldron = ((Levelled) block);
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

    public static Material getMaterial(int id) {
        try {
            return GET_MATERIAL != null ? (Material) GET_MATERIAL.invoke(null, id) : null;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            return null;
        }
    }

    public static int getBlockTypeIdAt(Location location) {
        try {
            return GET_BLOCK_TYPE_ID_AT != null ? (int) GET_BLOCK_TYPE_ID_AT.invoke(location.getWorld(), location) : 0;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            return 0;
        }
    }

    // Setting byte data to blocks works in 1.13, but isn't part of the API anymore
    public static void setData(Block block, byte data) {
        try {
            SET_DATA.invoke(block, data);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        }
    }

}
