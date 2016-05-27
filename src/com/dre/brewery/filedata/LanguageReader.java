package com.dre.brewery.filedata;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageReader {
	private Map<String, String> entries = new TreeMap<>();
	private Map<String, String> defaults = new TreeMap<>();

	private File file;
	private boolean changed;

	public LanguageReader(File file) {
		this.setDefaults();

		/* Load */
		this.file = file;

		FileConfiguration configFile = YamlConfiguration.loadConfiguration(file);

		Set<String> keySet = configFile.getKeys(false);
		for (String key : keySet) {
			entries.put(key, configFile.getString(key));
		}

		/* Check */
		this.check();
	}

	private void setDefaults() {
		
		/* Player */
		defaults.put("Player_BarrelCreated", "Barrel created");
		defaults.put("Player_CauldronInfo1", "This cauldron has been boiling for &v1 minutes.");
		defaults.put("Player_CauldronInfo2", "This cauldron has just started boiling.");
		defaults.put("Player_CantDrink", "You can't drink any more.");
		defaults.put("Player_DrunkPassOut", "You drank too much and passed out.");
		defaults.put("Player_LoginDeny", "Your character tries to log in, but is too drunk to find the server. Try again!");
		defaults.put("Player_LoginDenyLong", "Your character is really drunk and has passed out. Try again in 10 minutes!");
		defaults.put("Player_Wake", "Ohh no! I cannot remember how I got here...");
		defaults.put("Player_WakeCreated", "&aWakeup Point with id: &6&v1 &awas created successfully!");
		defaults.put("Player_WakeNotExist", "&cThe Wakeup Point with the id: &6&v1 &cdoesn't exist!");
		defaults.put("Player_WakeDeleted", "&aThe Wakeup Point with the id: &6&v1 &awas successfully deleted!");
		defaults.put("Player_WakeAlreadyDeleted", "&cThe Wakeup Point with the id: &6&v1 &chas already been deleted!");
		defaults.put("Player_WakeFilled", "&cThe Wakeup Point with the id: &6&v1&c at position &6&v2 &v3, &v4, &v5&c is filled with Blocks!");
		defaults.put("Player_WakeNoPoints", "&cThere are no Wakeup Points!");
		defaults.put("Player_WakeLast", "&aThis was the last Wakeup Point");
		defaults.put("Player_WakeTeleport", "Teleport to Wakeup Point with the id: &6&v1&f At position: &6&v2 &v3, &v4, &v5");
		defaults.put("Player_WakeHint1", "To Next Wakeup Point: Punch your fist in the air");
		defaults.put("Player_WakeHint2", "To Cancel: &9/br wakeup cancel");
		defaults.put("Player_WakeCancel", "&6Wakeup Point Check was cancelled");
		defaults.put("Player_WakeNoCheck", "&cNo Wakeup Point Check is currently active");
		defaults.put("Player_TriedToSay", "&v1 tried to say: &0&v2");

		/* Brew */
		defaults.put("Brew_Distilled", "Distilled");
		defaults.put("Brew_BarrelRiped", "Barrel aged");
		defaults.put("Brew_Undefined", "Indefinable Brew");
		defaults.put("Brew_DistillUndefined", "Indefinable Distillate");
		defaults.put("Brew_BadPotion", "Ruined Potion");
		defaults.put("Brew_Ingredients", "Ingredients");
		defaults.put("Brew_minute", "minute");
		defaults.put("Brew_MinutePluralPostfix", "s");
		defaults.put("Brew_fermented", "fermented");
		defaults.put("Brew_-times", "-times");
		defaults.put("Brew_OneYear", "One Year");
		defaults.put("Brew_Years", "Years");
		defaults.put("Brew_HundredsOfYears", "Hundreds of Years");
		defaults.put("Brew_Woodtype", "Woodtype");
		defaults.put("Brew_ThickBrew", "Muddy Brew");
		
		/* Commands */
		defaults.put("CMD_Reload", "&aConfig was successfully reloaded");
		defaults.put("CMD_Player", "&a&v1 is now &6&v2% &adrunk, with a quality of &6&v3");
		defaults.put("CMD_Player_Error", "&cThe quality has to be between 1 and 10!");
		defaults.put("CMD_Info_NotDrunk", "&v1 is not drunk");
		defaults.put("CMD_Info_Drunk", "&v1 is &6&v2% &fdrunk, with a quality of &6&v3");
		defaults.put("CMD_UnLabel", "&aLabel removed!");
		defaults.put("CMD_Persistent", "&aPotion is now Persistent and Static and may now be copied like any other item. You can remove the persistence with the same command.");
		defaults.put("CMD_PersistRemove", "&cPersistent Brews cannot be removed from the Database. It would render any copies of them useless!");
		defaults.put("CMD_UnPersist", "&aPersistence and static Removed. &eEvery Potential copy NOT made with '/brew copy' could become useless now!");
		defaults.put("CMD_Copy_Error", "&6&v1 &cPotions did not fit into your inventory");
		defaults.put("CMD_CopyNotPersistent", "&eThese copies of this Brew will not be persistent or static!");
		defaults.put("CMD_Static", "&aPotion is now static and will not change in barrels or brewing stands.");
		defaults.put("CMD_NonStatic", "&ePotion is not static anymore and will normally age in barrels.");
		
		/* Error */
		defaults.put("Error_UnknownCommand", "Unknown Command");
		defaults.put("Error_ShowHelp", "Use &6/brew help &fto display the help");
		defaults.put("Error_PlayerCommand", "&cThis command can only be executed as a player!");
		defaults.put("Error_ItemNotPotion", "&cThe item in your hand could not be identified as a potion!");
		defaults.put("Error_NoBrewName", "&cNo Recipe with Name: '&v1&c' found!");
		defaults.put("Error_Recipeload", "&cNot all recipes could be restored: More information in the server log!");
		defaults.put("Error_ConfigUpdate", "Unknown Brewery config version: v&v1, config was not updated!");
		defaults.put("Error_PersistStatic", "&cPersistent potions are always static!");

		/* Permissions */
		defaults.put("Error_NoPermissions", "&cYou don't have permissions to do this!");
		defaults.put("Error_NoBarrelAccess", "&cYou don't have permissions to access this barrel!");
		defaults.put("Perms_NoBarrelCreate", "&cYou don't have permissions to create barrels!");
		defaults.put("Perms_NoSmallBarrelCreate", "&cYou don't have permissions to create small barrels!");
		defaults.put("Perms_NoBigBarrelCreate", "&cYou don't have permissions to create big barrels!");
		defaults.put("Perms_NoCauldronInsert", "&cYou don't have permissions to put ingredients into cauldrons!");
		defaults.put("Perms_NoCauldronFill", "&cYou don't have permissions to fill bottles from this cauldron!");
		
		/* Help */
		defaults.put("Help_Help", "&6/brew help <Page> &9Shows a specific help-page");
		defaults.put("Help_Player", "&6/brew <Player> <%Drunkeness> <Quality>&9 Sets Drunkeness (and Quality) of a Player");
		defaults.put("Help_Info", "&6/brew info&9 Displays your current Drunkeness and Quality");
		defaults.put("Help_UnLabel", "&6/brew unlabel &9Removes the detailled label of a potion");
		defaults.put("Help_Copy", "&6/brew copy <Quanitiy>&9 Copies the potion in your hand");
		defaults.put("Help_Delete", "&6/brew delete &9Deletes the potion in your hand");
		defaults.put("Help_InfoOther", "&6/brew info <Player>&9 Displays the current Drunkeness and Quality of <Player>");
		defaults.put("Help_Wakeup", "&6/brew wakeup list <Page>&9 Lists all wakeup points");
		defaults.put("Help_WakeupList", "&6/brew wakeup list <Page> <World>&9 Lists all wakeup points of <world>");
		defaults.put("Help_WakeupCheck", "&6/brew wakeup check &9Teleports to all wakeup points");
		defaults.put("Help_WakeupCheckSpecific", "&6/brew wakeup check <id> &9Teleports to the wakeup point with <id>");
		defaults.put("Help_WakeupAdd", "&6/brew wakeup add &9Adds a wakeup point at your current position");
		defaults.put("Help_WakeupRemove", "&6/brew wakeup remove <id> &9Removes the wakeup point with <id>");
		defaults.put("Help_Reload", "&6/brew reload &9Reload config");
		defaults.put("Help_Persist", "&6/brew persist &9Make Brew persistent -> copyable by any plugin and technique");
		defaults.put("Help_Static", "&6/brew static &9Make Brew static -> No further ageing or distilling");
		defaults.put("Help_Create", "&6/brew create <Recipe> <Quality> &9Create a Brew with optional quality (1-10)");
		
		/* Etc. */
		defaults.put("Etc_Usage", "Usage:");
		defaults.put("Etc_Page", "Page");
		defaults.put("Etc_Barrel", "Barrel");
	}

	private void check() {
		for (String defaultEntry : defaults.keySet()) {
			if (!entries.containsKey(defaultEntry)) {
				entries.put(defaultEntry, defaults.get(defaultEntry));
				changed = true;
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void save() {
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
