package com.dre.brewery;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class LegacyUtil {

    private static Method GET_MATERIAL;
    private static Method GET_BLOCK_TYPE_ID_AT;

    static {
        try {
            GET_MATERIAL = Material.class.getDeclaredMethod("getMaterial", int.class);
            GET_BLOCK_TYPE_ID_AT = World.class.getDeclaredMethod("getBlockTypeIdAt", Location.class);
        } catch (NoSuchMethodException | SecurityException ex) {
        }
    }

    public static final Material FLOWING_LAVA = get("FLOWING_LAVA", "LAVA");
    public static final Material LAVA = get("LAVA", "STATIONARY_LAVA");
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

    public static boolean isFence(Material material) {
        return material.name().endsWith("FENCE");
    }

    public static boolean isSign(Material type) {
        return type.name().equals("SIGN_POST") || type == Material.SIGN || type == Material.WALL_SIGN;
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

}
