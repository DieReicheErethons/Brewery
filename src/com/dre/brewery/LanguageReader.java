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
		defaults.put("Player_BarrelCreated", "Fass erfolgreich erstellt");
		defaults.put("Player_CauldronInfo1", "Dieser Kessel siedet nun seit &v1 Minuten");
		defaults.put("Player_CauldronInfo2", "Dieser Kessel siedet seit weniger als einer Minute");
		defaults.put("Player_CantDrink", "Du kannst nicht mehr trinken");
		defaults.put("Player_DrunkPassOut", "Du hast zu viel getrunken und bist in Ohnmacht gefallen!");
		defaults.put("Player_Wake", "Ohh nein! Ich kann mich nicht erinnern, wie ich hierhergekommen bin...");
		defaults.put("Player_WakeCreated", "&aAufwachpunkt mit id: &6&v1 &awurde erfolgreich erstellt!");
		defaults.put("Player_WakeNotExist", "&cDer Aufwachpunkt mit der id: &6&v1 &cexistiert nicht!");
		defaults.put("Player_WakeDeleted", "&aDer Aufwachpunkt mit der id: &6&v1 &awurde erfolgreich gelöscht!");
		defaults.put("Player_WakeAlreadyDeleted", "&cDer Aufwachpunkt mit der id: &6&v1 &cwurde bereits gelöscht!");
		defaults.put("Player_WakeFilled", "&cDer Aufwachpunkt mit der id: &6&v1&c an Position &6&v2 &v3, &v4, &v5&c ist mit Blöcken gefüllt!");
		defaults.put("Player_WakeNoPoints", "&cEs wurden noch keine Aufwachpunkte erstellt!");
		defaults.put("Player_WakeLast", "&aDies war der letzte Aufwachpunkt");
		defaults.put("Player_WakeTeleport", "Teleport zu Aufwachpunkt mit der id: &6&v1&f An Position: &6&v2 &v3, &v4, &v5");
		defaults.put("Player_WakeHint1", "Zum nächsten Aufwachpunkt: Mit Faust in die Luft schlagen");
		defaults.put("Player_WakeHint2", "Zum Abbrechen: &9/br wakeup cancel");
		defaults.put("Player_WakeCancel", "&6Aufwachpunkte-Check wurde abgebrochen");
		defaults.put("Player_WakeNoCheck", "&cEs läuft kein Aufwachpunkte-Check");

		/* Brew */
		defaults.put("Brew_Distilled", "Destilliert");
		defaults.put("Brew_BarrelRiped", "Fassgereift");
		defaults.put("Brew_Undefined", "Undefinierbarer Sud");
		defaults.put("Brew_DistillUndefined", "Undefinierbares Destillat");
		defaults.put("Brew_BadPotion", "Verdorbenes Getränk");
		defaults.put("Brew_Ingredients", "Zutaten");
		defaults.put("Brew_minute", "minute");
		defaults.put("Brew_MinutePluralPostfix", "n");
		defaults.put("Brew_fermented", "gegärt");
		defaults.put("Brew_-times", "-fach");
		defaults.put("Brew_OneYear", "Ein Jahr");
		defaults.put("Brew_Years", "Jahre");
		defaults.put("Brew_HundredsOfYears", "Hunderte Jahre");
		defaults.put("Brew_Woodtype", "Holzart");
		defaults.put("Brew_ThickBrew", "Schlammiger Sud");
		
		/* Commands */
		defaults.put("CMD_Reload", "&aConfig wurde neu eingelesen");
		defaults.put("CMD_Player", "&a&v1 ist nun &6&v2% &abetrunken, mit einer Qualität von &6&v3");
		defaults.put("CMD_Player_Error", "&cDie Qualität muss zwischen 1 und 10 liegen!");
		defaults.put("CMD_Info_NotDrunk", "&v1 ist nicht betrunken");
		defaults.put("CMD_Info_Drunk", "&v1 ist &6&v2% &fbetrunken, mit einer Qualität von &6&v1");
		defaults.put("CMD_UnLabel", "&aDas Label wurde entfernt");
		defaults.put("CMD_Copy_Error", "&6&v1 &cTränke haben nicht mehr in das Inventar gepasst");
		
		/* Error */
		defaults.put("Error_NoPermissions", "&cDu hast keine Rechte dies zu tun!");
		defaults.put("Error_UnknownCommand", "Unbekannter Befehl");
		defaults.put("Error_ShowHelp", "benutze &6/br help &fum die Hilfe anzuzeigen");
		defaults.put("Error_PlayerCommand", "&cDieser Befehl kann nur als Spieler ausgeführt werden");
		defaults.put("Error_ItemNotPotion", "&cDas Item in deiner Hand konnte nicht als Trank identifiziert werden");
		defaults.put("Error_Recipeload", "&cEs konnten nicht alle Rezepte wiederhergesellt werden: Siehe Serverlog!");
		
		/* Help */
		defaults.put("Help_Help", "&6/br help <Seite> &9Zeigt eine bestimmte Hilfeseite an");
		defaults.put("Help_Player", "&6/br <Spieler> <%Trunkenheit> <Qualität>&9 Setzt Trunkenheit (und Qualität) eines Spielers");
		defaults.put("Help_Info", "&6/br Info&9 Zeigt deine aktuelle Trunkenheit und Qualität an");
		defaults.put("Help_UnLabel", "&6/br UnLabel &9Entfernt die genaue Beschriftung des Trankes");
		defaults.put("Help_Copy", "&6/br Copy <Anzahl>&9 Kopiert den Trank in deiner Hand");
		defaults.put("Help_Delete", "&6/br Delete &9Entfernt den Trank in deiner Hand");
		defaults.put("Help_InfoOther", "&6/br Info <Spieler>&9 Zeigt die aktuelle Trunkenheit und Qualität von <Spieler> an");
		defaults.put("Help_Wakeup", "&6/br Wakeup List <Seite>&9 Listet alle Aufwachpunkte auf");
		defaults.put("Help_WakeupList", "&6/br Wakeup List <Seite> <Welt>&9 Listet die Aufwachpunkte einer Welt auf");
		defaults.put("Help_WakeupCheck", "&6/br Wakeup Check &9Teleportiert zu allen Aufwachpunkten");
		defaults.put("Help_WakeupCheckSpecific", "&6/br Wakeup Check <id> &9Teleportiert zu einem Aufwachpunkt");
		defaults.put("Help_WakeupAdd", "&6/br Wakeup Add &9Setzt einen Aufwachpunkt");
		defaults.put("Help_WakeupRemove", "&6/br Wakeup Remove <id> &9Entfernt einen Aufwachpunkt");
		defaults.put("Help_Reload", "&6/br reload &9Config neuladen");
		
		/* Etc. */
		defaults.put("Etc_Usage", "Benutzung:");
		defaults.put("Etc_Page", "Seite");
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
