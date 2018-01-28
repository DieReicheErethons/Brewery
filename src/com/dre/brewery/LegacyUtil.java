package com.dre.brewery;

import org.bukkit.Material;

public class LegacyUtil {

    public static final Material FLOWING_LAVA = Material.valueOf(P.use1_13 ? "FLOWING_LAVA" : "LAVA");
    public static final Material LAVA = Material.valueOf(P.use1_13 ? "LAVA" : "STATIONARY_LAVA");
    public static final Material CLOCK = Material.valueOf(P.use1_13 ? "CLOCK" : "WATCH");

    public static boolean isWoodPlanks(Material type) {
        return type.name().contains("PLANKS") || type.name().equals("WOOD");
    }

    public static boolean isSign(Material type) {
        return type.name().equals("LEGACY_SIGN_POST") || type == Material.SIGN || type == Material.WALL_SIGN;
    }

}
