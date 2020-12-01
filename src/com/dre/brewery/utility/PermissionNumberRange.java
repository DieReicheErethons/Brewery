package com.dre.brewery.utility;

import java.util.HashMap;
import java.util.Map;

import com.dre.brewery.P;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

public class PermissionNumberRange {
    public static Map<String, PermissionNumberRange> permNums = new HashMap<>();
    
    public static void register(String nodePrefix, String lastNode, int from, int to, int incr, int def) {
        PermissionNumberRange pn = new PermissionNumberRange(nodePrefix + lastNode, from, to, incr, def);
        permNums.put(pn.permNode, pn);
    }

	@Nullable
	public static boolean updateFromConfig(ConfigurationSection sec, String nodePrefix, String lastNode) {
        boolean validKey = permNums.containsKey(nodePrefix + lastNode);
        if (validKey) {
            PermissionNumberRange pn = permNums.get(nodePrefix + lastNode);
            pn.searchFrom = sec.getInt(lastNode + ".from", pn.searchFrom);
            pn.searchTo = sec.getInt(lastNode + ".to", pn.searchTo);
            pn.searchIncr = sec.getInt(lastNode + ".incr", pn.searchIncr);
            pn.defaultVal = sec.getInt(lastNode + ".default", pn.defaultVal);
            pn.enforceValidIncrement();
        }
        return validKey;
	}

	/**
	 * Finds the largest value given by a permission node that ends in an integer
	 * @param permNode the node without an integer (eg for brewery.tolerance.recovery75, permNode is "brewery.tolerance.recovery")
	 * @param p the Permissible (player/sender/etc) to check the permission of
	 * @return the highest-value integer of the nodes consisting of the permNode
	 */
    public static int getPermNum(String permNode, Permissible p) {
        if (permNums.containsKey(permNode)) {
            return permNums.get(permNode).getPermNumOf(p);
        }
        P.p.errorLog("Failed to find permission number node " + permNode);
        return 0;
    }



    private int searchFrom = 100;
    private int searchTo = 0;
    private int searchIncr = 1;
    int defaultVal = 0;
    String permNode = "";
    
    public PermissionNumberRange(String fullNode, int from, int to, int incr, int def) {
        setSearch(from, to, incr);
        defaultVal = def;
        permNode = fullNode;
    }

    public void setSearch(int from, int to, int incr) {
        searchFrom = from;
        searchTo = to;
        searchIncr = incr;
        enforceValidIncrement();
    }

    /**
     * Changes the sign of searchIncr to match the direction of searching from searchFrom to searchTo
     */
    private void enforceValidIncrement() {
        if ((searchFrom > searchTo && searchIncr > 0) || (searchFrom < searchTo && searchIncr < 0)) {
            searchIncr *= -1;
        }
    }
    
	/**
	 * @see PermissionNumberRange#getPermNum
	 */
	public int getPermNumOf(Permissible p) {
        if (searchTo == searchFrom || searchIncr == 0) {
            return defaultVal;
        }

        int i = searchFrom;
        while (searchIncr > 0 ? i <= searchTo : i >= searchTo) {
			if (p.hasPermission(permNode + i)) {
				return i;
            }
            i += searchIncr;
        }
        return defaultVal;
	}
}
