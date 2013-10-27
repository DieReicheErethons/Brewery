package com.dre.brewery;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageReader {
	private Map<String, String> entries = new TreeMap<String, String>();
	private Map<String, String> defaults = new TreeMap<String, String>();

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
		defaults.put("Player_CauldronInfo1", "This Cauldron seethes since &v1 minutes");
		defaults.put("Player_CauldronInfo2", "This Cauldron seethes since less than one minute");
		defaults.put("Player_CantDrink", "You can't drink any more");
		defaults.put("Player_DrunkPassOut", "You have drunken too much and fainted!");
		defaults.put("Player_LoginDeny", "Your character is drunk and does not react. Try again!");
		defaults.put("Player_LoginDenyLong", "Your character is really drunk and unconscious. Try again in 10 minutes!");
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
		defaults.put("CMD_Player_Error", "&cThe Quality has to be between 1 and 10!");
		defaults.put("CMD_Info_NotDrunk", "&v1 is not drunk");
		defaults.put("CMD_Info_Drunk", "&v1 is &6&v2% &fdrunk, with a quality of &6&v3");
		defaults.put("CMD_UnLabel", "&aLabel removed!");
		defaults.put("CMD_Copy_Error", "&6&v1 &cPotions did not fit into your inventory");
		
		/* Error */
		defaults.put("Error_NoPermissions", "&cYou have no permission to do this!");
		defaults.put("Error_UnknownCommand", "Unknown Command");
		defaults.put("Error_ShowHelp", "use &6/br help &fto display the help");
		defaults.put("Error_PlayerCommand", "&cThis command can only be executed as player");
		defaults.put("Error_ItemNotPotion", "&cThe Item in your hand could not be identified as Potion");
		defaults.put("Error_Recipeload", "&cNot all recipes could be restored: More information in the Serverlog!");
		
		/* Help */
		defaults.put("Help_Help", "&6/br help <Page> &9Shows a specific help-page");
		defaults.put("Help_Player", "&6/br <Player> <%Drunkeness> <Quality>&9 Sets Drunkeness (and Quality) of a Player");
		defaults.put("Help_Info", "&6/br Info&9 Displays your current Drunkeness and Quality");
		defaults.put("Help_UnLabel", "&6/br UnLabel &9Removes the detailled label of a Potion");
		defaults.put("Help_Copy", "&6/br Copy <Quanitiy>&9 Copies the Potion in your Hand");
		defaults.put("Help_Delete", "&6/br Delete &9Deletes the Potion in your Hand");
		defaults.put("Help_InfoOther", "&6/br Info <Player>&9 Displays the current Drunkeness and Quality of <Player>");
		defaults.put("Help_Wakeup", "&6/br Wakeup List <Page>&9 Lists all Wakeup Points");
		defaults.put("Help_WakeupList", "&6/br Wakeup List <Page> <World>&9 Lists all Wakeup Points of a World");
		defaults.put("Help_WakeupCheck", "&6/br Wakeup Check &9Teleports to all Wakeup Points");
		defaults.put("Help_WakeupCheckSpecific", "&6/br Wakeup Check <id> &9Teleports to the Wakeup Point with <id>");
		defaults.put("Help_WakeupAdd", "&6/br Wakeup Add &9Adds a Wakeup Point at your current Position");
		defaults.put("Help_WakeupRemove", "&6/br Wakeup Remove <id> &9Removes the Wakeup Point with <id>");
		defaults.put("Help_Reload", "&6/br reload &9Reload config");
		
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
