package com.dre.brewery.filedata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dre.brewery.utility.Tuple;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageReader {
	private Map<String, String> entries = new HashMap<>(128);

	private File file;
	private boolean changed;

	public LanguageReader(File file) {
		List<Tuple<String, String>> defaults = getDefaults();

		/* Load */
		this.file = file;

		FileConfiguration configFile = YamlConfiguration.loadConfiguration(file);

		Set<String> keySet = configFile.getKeys(false);
		for (String key : keySet) {
			entries.put(key, configFile.getString(key));
		}

		/* Check */
		check(defaults);
		if (changed) {
			save();
		}
	}

	private List<Tuple<String, String>> getDefaults() {
		List<Tuple<String, String>> defaults = new ArrayList<>(128);

		/* Player */
		defaults.add(new Tuple<>("Player_BarrelCreated", "Barrel created"));
		defaults.add(new Tuple<>("Player_BarrelFull", "&cThis barrel can''t hold any more drinks"));
		defaults.add(new Tuple<>("Player_CauldronInfo1", "This cauldron has been boiling for &v1 minutes."));
		defaults.add(new Tuple<>("Player_CauldronInfo2", "This cauldron has just started boiling."));
		defaults.add(new Tuple<>("Player_CantDrink", "You can't drink any more."));
		defaults.add(new Tuple<>("Player_DrunkPassOut", "You drank too much and passed out."));
		defaults.add(new Tuple<>("Player_LoginDeny", "Your character tries to log in, but is too drunk to find the server. Try again!"));
		defaults.add(new Tuple<>("Player_LoginDenyLong", "Your character is really drunk and has passed out. Try again in 10 minutes!"));
		defaults.add(new Tuple<>("Player_Wake", "Ohh no! I cannot remember how I got here..."));
		defaults.add(new Tuple<>("Player_WakeCreated", "&aWakeup Point with id: &6&v1 &awas created successfully!"));
		defaults.add(new Tuple<>("Player_WakeNotExist", "&cThe Wakeup Point with the id: &6&v1 &cdoesn't exist!"));
		defaults.add(new Tuple<>("Player_WakeDeleted", "&aThe Wakeup Point with the id: &6&v1 &awas successfully deleted!"));
		defaults.add(new Tuple<>("Player_WakeAlreadyDeleted", "&cThe Wakeup Point with the id: &6&v1 &chas already been deleted!"));
		defaults.add(new Tuple<>("Player_WakeFilled", "&cThe Wakeup Point with the id: &6&v1&c at position &6&v2 &v3, &v4, &v5&c is filled with Blocks!"));
		defaults.add(new Tuple<>("Player_WakeNoPoints", "&cThere are no Wakeup Points!"));
		defaults.add(new Tuple<>("Player_WakeLast", "&aThis was the last Wakeup Point"));
		defaults.add(new Tuple<>("Player_WakeTeleport", "Teleport to Wakeup Point with the id: &6&v1&f At position: &6&v2 &v3, &v4, &v5"));
		defaults.add(new Tuple<>("Player_WakeHint1", "To Next Wakeup Point: Punch your fist in the air"));
		defaults.add(new Tuple<>("Player_WakeHint2", "To Cancel: &9/br wakeup cancel"));
		defaults.add(new Tuple<>("Player_WakeCancel", "&6Wakeup Point Check was cancelled"));
		defaults.add(new Tuple<>("Player_WakeNoCheck", "&cNo Wakeup Point Check is currently active"));
		defaults.add(new Tuple<>("Player_TriedToSay", "&v1 tried to say: &0&v2"));

		/* Brew */
		defaults.add(new Tuple<>("Brew_Distilled", "Distilled"));
		defaults.add(new Tuple<>("Brew_BarrelRiped", "Barrel aged"));
		defaults.add(new Tuple<>("Brew_Undefined", "Indefinable Brew"));
		defaults.add(new Tuple<>("Brew_DistillUndefined", "Indefinable Distillate"));
		defaults.add(new Tuple<>("Brew_BadPotion", "Ruined Potion"));
		defaults.add(new Tuple<>("Brew_Ingredients", "Ingredients"));
		defaults.add(new Tuple<>("Brew_minute", "minute"));
		defaults.add(new Tuple<>("Brew_MinutePluralPostfix", "s"));
		defaults.add(new Tuple<>("Brew_fermented", "fermented"));
		defaults.add(new Tuple<>("Brew_-times", "-times"));
		defaults.add(new Tuple<>("Brew_OneYear", "One Year"));
		defaults.add(new Tuple<>("Brew_Years", "Years"));
		defaults.add(new Tuple<>("Brew_HundredsOfYears", "Hundreds of Years"));
		defaults.add(new Tuple<>("Brew_Woodtype", "Woodtype"));
		defaults.add(new Tuple<>("Brew_ThickBrew", "Muddy Brew"));
		defaults.add(new Tuple<>("Brew_Alc", "Alc &v1ml"));

		/* Commands */
		defaults.add(new Tuple<>("CMD_Reload", "&aConfig was successfully reloaded"));
		defaults.add(new Tuple<>("CMD_Configname", "&aName for the Config is: &f&v1"));
		defaults.add(new Tuple<>("CMD_Configname_Error", "&cCould not find item in your hand"));
		defaults.add(new Tuple<>("CMD_Player", "&a&v1 is now &6&v2% &adrunk, with a quality of &6&v3"));
		defaults.add(new Tuple<>("CMD_Player_Error", "&cThe quality has to be between 1 and 10!"));
		defaults.add(new Tuple<>("CMD_Info_NotDrunk", "&v1 is not drunk"));
		defaults.add(new Tuple<>("CMD_Info_Drunk", "&v1 is &6&v2% &fdrunk, with a quality of &6&v3"));
		defaults.add(new Tuple<>("CMD_UnLabel", "&aLabel removed!"));
		defaults.add(new Tuple<>("CMD_Persistent", "&aPotion is now Persistent and Static and may now be copied like any other item. You can remove the persistence with the same command."));
		defaults.add(new Tuple<>("CMD_PersistRemove", "&cPersistent Brews cannot be removed from the Database. It would render any copies of them useless!"));
		defaults.add(new Tuple<>("CMD_UnPersist", "&aPersistence and static Removed. &eEvery Potential copy NOT made with '/brew copy' could become useless now!"));
		defaults.add(new Tuple<>("CMD_Copy_Error", "&6&v1 &cPotions did not fit into your inventory"));
		defaults.add(new Tuple<>("CMD_CopyNotPersistent", "&eThese copies of this Brew will not be persistent or static!"));
		defaults.add(new Tuple<>("CMD_Static", "&aPotion is now static and will not change in barrels or brewing stands."));
		defaults.add(new Tuple<>("CMD_NonStatic", "&ePotion is not static anymore and will normally age in barrels."));

		/* Error */
		defaults.add(new Tuple<>("Error_UnknownCommand", "Unknown Command"));
		defaults.add(new Tuple<>("Error_ShowHelp", "Use &6/brew help &fto display the help"));
		defaults.add(new Tuple<>("Error_PlayerCommand", "&cThis command can only be executed as a player!"));
		defaults.add(new Tuple<>("Error_ItemNotPotion", "&cThe item in your hand could not be identified as a potion!"));
		defaults.add(new Tuple<>("Error_NoBrewName", "&cNo Recipe with Name: '&v1&c' found!"));
		defaults.add(new Tuple<>("Error_Recipeload", "&cNot all recipes could be restored: More information in the server log!"));
		defaults.add(new Tuple<>("Error_ConfigUpdate", "Unknown Brewery config version: v&v1, config was not updated!"));
		defaults.add(new Tuple<>("Error_PersistStatic", "&cPersistent potions are always static!"));

		/* Permissions */
		defaults.add(new Tuple<>("Error_NoPermissions", "&cYou don't have permissions to do this!"));
		defaults.add(new Tuple<>("Error_NoBarrelAccess", "&cYou don't have permissions to access this barrel!"));
		defaults.add(new Tuple<>("Perms_NoBarrelCreate", "&cYou don't have permissions to create barrels!"));
		defaults.add(new Tuple<>("Perms_NoSmallBarrelCreate", "&cYou don't have permissions to create small barrels!"));
		defaults.add(new Tuple<>("Perms_NoBigBarrelCreate", "&cYou don't have permissions to create big barrels!"));
		defaults.add(new Tuple<>("Perms_NoCauldronInsert", "&cYou don't have permissions to put ingredients into cauldrons!"));
		defaults.add(new Tuple<>("Perms_NoCauldronFill", "&cYou don't have permissions to fill bottles from this cauldron!"));

		/* Help */
		defaults.add(new Tuple<>("Help_Help", "&6/brew help [Page] &9Shows a specific help-page"));
		defaults.add(new Tuple<>("Help_Player", "&6/brew <Player> <%Drunkeness> [Quality]&9 Sets Drunkeness (and Quality) of a Player"));
		defaults.add(new Tuple<>("Help_Info", "&6/brew info&9 Displays your current Drunkeness and Quality"));
		defaults.add(new Tuple<>("Help_UnLabel", "&6/brew unlabel &9Removes the detailled label of a potion"));
		defaults.add(new Tuple<>("Help_Copy", "&6/brew copy [Quantity]>&9 Copies the potion in your hand"));
		defaults.add(new Tuple<>("Help_Delete", "&6/brew delete &9Deletes the potion in your hand"));
		defaults.add(new Tuple<>("Help_InfoOther", "&6/brew info [Player]&9 Displays the current Drunkeness and Quality of [Player]"));
		defaults.add(new Tuple<>("Help_Wakeup", "&6/brew wakeup list <Page>&9 Lists all wakeup points"));
		defaults.add(new Tuple<>("Help_WakeupList", "&6/brew wakeup list <Page> [World]&9 Lists all wakeup points of [world]"));
		defaults.add(new Tuple<>("Help_WakeupCheck", "&6/brew wakeup check &9Teleports to all wakeup points"));
		defaults.add(new Tuple<>("Help_WakeupCheckSpecific", "&6/brew wakeup check <id> &9Teleports to the wakeup point with <id>"));
		defaults.add(new Tuple<>("Help_WakeupAdd", "&6/brew wakeup add &9Adds a wakeup point at your current position"));
		defaults.add(new Tuple<>("Help_WakeupRemove", "&6/brew wakeup remove <id> &9Removes the wakeup point with <id>"));
		defaults.add(new Tuple<>("Help_Reload", "&6/brew reload &9Reload config"));
		defaults.add(new Tuple<>("Help_Configname", "&6/brew ItemName &9Display name of item in hand for the config"));
		defaults.add(new Tuple<>("Help_Persist", "&6/brew persist &9Make Brew persistent -> copyable by any plugin and technique"));
		defaults.add(new Tuple<>("Help_Static", "&6/brew static &9Make Brew static -> No further ageing or distilling"));
		defaults.add(new Tuple<>("Help_Create", "&6/brew create <Recipe> [Quality] [Player] &9Create a Brew with optional quality (1-10)"));

		/* Etc. */
		defaults.add(new Tuple<>("Etc_Usage", "Usage:"));
		defaults.add(new Tuple<>("Etc_Page", "Page"));
		defaults.add(new Tuple<>("Etc_Barrel", "Barrel"));

		return defaults;
	}

	private void check(List<Tuple<String, String>> defaults) {
		for (Tuple<String, String> def : defaults) {
			if (!entries.containsKey(def.a())) {
				entries.put(def.a(), def.b());
				changed = true;
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void save() {
		if (changed) {
			/* Copy old File */
			File source = new File(file.getPath());
			String filePath = file.getPath();
			File temp = new File(filePath.substring(0, filePath.length() - 4) + "_old.yml");

			if (temp.exists())
				temp.delete();

			source.renameTo(temp);

			/* Save */
			FileConfiguration configFile = new YamlConfiguration();

			for (String key : entries.keySet()) {
				configFile.set(key, entries.get(key));
			}

			try {
				configFile.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String get(String key, String... args) {
		String entry = entries.get(key);

		if (entry != null) {
			int i = 0;
			for (String arg : args) {
				if (arg != null) {
					i++;
					entry = entry.replace("&v" + i, arg);
				}
			}
		} else {
			entry = "%placeholder%";
		}

		return entry;
	}
}
