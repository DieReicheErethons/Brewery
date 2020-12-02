package com.dre.brewery.utility;

import java.util.SortedMap;
import java.util.TreeMap;

import com.dre.brewery.P;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class NamedRanges {
    SortedMap<Integer, String> rangeNames = new TreeMap<>();

    public void addName(int startingAt, String name) {
        rangeNames.put(startingAt, name);
    }

    public String getName(int atValue) {
        int prevKey = 0;
        for (int k : rangeNames.keySet()) {
            if (atValue < k) {
                return rangeNames.getOrDefault(prevKey, null);
            }
            prevKey = k;
        }
		
		if (atValue > prevKey) {
			return rangeNames.getOrDefault(prevKey, null);
		}
        return null;
	}
	
	public boolean isEmpty() {
		return rangeNames.isEmpty();
	}
    
	@Nullable
	public static NamedRanges fromConfigSection(ConfigurationSection sec) {
        NamedRanges rn = new NamedRanges();
		if (sec == null) {
			return rn;
		}
        
        for (String k : sec.getKeys(false)) {
			int k_int;
            try {
				k_int = Integer.parseInt(k);
            } catch (NumberFormatException e) {
                P.p.errorLog("Tried to create a named range but got something that isn't a number: '" + k + "'!");
                return null;
			}
			rn.addName(k_int, sec.getString(k));
		}
		return rn;
	}
}
