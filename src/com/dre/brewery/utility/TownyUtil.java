package com.dre.brewery.utility;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TownyUtil {
    public static boolean isInsideTown(Location location) {
        try {
            final Town town = TownyAPI.getInstance().getTownBlock(location).getTown();
            if(town != null) {
            //Execute your code here
                return true;
            }
        }  catch (NullPointerException | NotRegisteredException e) { }
        return false;
    }
    public static boolean isInsideTown(Location location, Player player) {
        if(player == null)
            return isInsideTown(location);
        try {
            final Resident resident = TownyAPI.getInstance().getDataSource().getResident(player.getName());
            final Town town = TownyAPI.getInstance().getTownBlock(location).getTown();
            if (resident.getTown().equals(town)) {
            //Execute your code here
                return true;
            }
        }  catch (NullPointerException | NotRegisteredException e) { }
        return false;
    }
}