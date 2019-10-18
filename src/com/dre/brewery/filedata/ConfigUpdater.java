package com.dre.brewery.filedata;

import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.P;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigUpdater {

	private ArrayList<String> config = new ArrayList<>();
	private File file;

	public ConfigUpdater(File file) {
		this.file = file;
		getConfigString();
	}

	// Returns the index of the line that starts with 'lineStart', returns -1 if not found;
	public int indexOfStart(String lineStart) {
		for (int i = 0; i < config.size(); i++) {
			if (config.get(i).startsWith(lineStart)) {
				return i;
			}
		}
		return -1;
	}

	// Adds some lines to the end
	public void appendLines(String... lines) {
		config.addAll(Arrays.asList(lines));
	}

	// Replaces the line at the index with the new Line
	public void setLine(int index, String newLine) {
		config.set(index, newLine);
	}

	// adds some Lines at the index
	// Will push all lines including the one at index down
	public void addLines(int index, String... newLines) {
		config.addAll(index, Arrays.asList(newLines));
	}

	public void saveConfig() {
		StringBuilder stringBuilder = new StringBuilder();
		for (String line : config) {
			stringBuilder.append(line).append("\n");
		}
		String configString = stringBuilder.toString().trim();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(configString);
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getConfigString() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String currentLine;
			while((currentLine = reader.readLine()) != null) {
				config.add(currentLine);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	// ---- Updating Scramble Seed ----

	public void setEncodeKey(long key) {
		int index = indexOfStart("encodeKey:");
		if (index != -1) {
			setLine(index, "encodeKey: " + key);
			return;
		}

		// Old key not present
		index = indexOfStart("enableEncode:");
		if (index == -1) {
			index = indexOfStart("# So enable this if you want to make recipe cheating harder");
		}
		if (index == -1) {
			index = indexOfStart("version:");
		}
		if (index != -1) {
			addLines(index + 1, "encodeKey: " + key);
		} else {
			addLines(1, "encodeKey: " + key);
		}

	}

	// ---- Updating to newer Versions ----

	// Update from a specified Config version and language to the newest version
	public void update(String fromVersion, boolean oldMat, String lang) {
		if (fromVersion.equals("0.5")) {
			// Version 0.5 was only released for de, but with en as setting, so default to de
			if (!lang.equals("de")) {
				lang = "de";
			}
		}
		boolean de = lang.equals("de");

		if (fromVersion.equals("0.5") || fromVersion.equals("1.0")) {
			if (de) {
				update05de();
			} else {
				update10en();
			}
			fromVersion = "1.1";
		}
		if (fromVersion.equals("1.1") || fromVersion.equals("1.1.1")) {
			if (de) {
				update11de();
			} else {
				update11en();
			}
			fromVersion = "1.2";
		}

		if (fromVersion.equals("1.2")) {
			if (de) {
				update12de();
			} else {
				update12en();
			}
			fromVersion = "1.3";
		}

		if (fromVersion.equals("1.3")) {
			if (de) {
				update13de();
			} else {
				update13en();
			}
			fromVersion = "1.3.1";
		}

		if (fromVersion.equals("1.3.1")) {
			if (de) {
				update131de();
			} else {
				update131en();
			}
			fromVersion = "1.4";
		}

		if (fromVersion.equals("1.4")) {
			if (de) {
				update14de();
			} else {
				update14en();
			}
			fromVersion = "1.5";
		}

		if (fromVersion.equals("1.5") || fromVersion.equals("1.6")) {
			update15(P.use1_13, de);
			fromVersion = "1.7";
			oldMat = false;
		}

		if (fromVersion.equals("1.7")) {
			if (de) {
				update17de();
			} else {
				update17en();
			}
			fromVersion = "1.8";
		}

		if (P.use1_13 && oldMat) {
			updateMaterials(true);
			updateMaterialDescriptions(de);
		}

		if (!fromVersion.equals("1.8")) {
			P.p.log(P.p.languageReader.get("Error_ConfigUpdate", fromVersion));
			return;
		}
		saveConfig();
	}

	// Update the Version String
	private void updateVersion(String to) {
		int index = indexOfStart("version");
		String line = "version: '" + to + "'";
		if (index != -1) {
			setLine(index, line);
		} else {
			index = indexOfStart("# Config Version");
			if (index == -1) {
				index = indexOfStart("autosave");
			}
			if (index == -1) {
				appendLines(line);
			} else {
				addLines(index, line);
			}
		}
	}

	// Updates de from 0.5 to 1.1
	private void update05de() {
		updateVersion("1.1");

		// Default language to de
		int index = indexOfStart("language: en");
		if (index != -1) {
			setLine(index, "language: de");
			P.p.language = "de";
		}

		// Add the new entries for the Word Distortion above the words section
		String[] entries = {
			"# -- Chat Veränderungs Einstellungen --",
			"",
			"# Text nach den angegebenen Kommandos wird bei Trunkenheit ebenfalls Verändert (Liste) [- /gl]",
			"distortCommands:",
			"- /gl",
			"- /global",
			"- /fl",
			"- /s",
			"- /letter",
			"",
			"# Geschriebenen Text auf Schildern bei Trunkenheit verändern [false]",
			"distortSignText: false",
			"",
			"# Text, der zwischen diesen Buchstaben steht, wird nicht verändert (\",\" als Trennung verwenden) (Liste) [- '[,]']",
			"distortBypass:",
			"- '*,*'",
			"- '[,]'",
			""
			};
		index = indexOfStart("# words");
		if (index == -1) {
			index = indexOfStart("# Diese werden von oben");
		}
		if (index == -1) {
			index = indexOfStart("# replace");
		}
		if (index == -1) {
			index = indexOfStart("words:");
		}
		if (index == -1) {
			appendLines(entries);
		} else {
			addLines(index, entries);
		}

		// Add some new separators for overview
		String line = "# -- Verschiedene Einstellungen --";
		index = indexOfStart("# Verschiedene Einstellungen");
		if (index != -1) {
			setLine(index, line);
		}

		line = "# -- Rezepte für Getränke --";
		index = indexOfStart("# Rezepte für Getränke");
		if (index != -1) {
			setLine(index, line);
		}
	}

	// Updates en from 1.0 to 1.1
	private void update10en() {
		// Update version String
		updateVersion("1.1");

		// Add the new entries for the Word Distortion above the words section
		String[] entries = {
			"# -- Chat Distortion Settings --",
			"",
			"# Text after specified commands will be distorted when drunk (list) [- /gl]",
			"distortCommands:",
			"- /gl",
			"- /global",
			"- /fl",
			"- /s",
			"- /letter",
			"",
			"# Distort the Text written on a Sign while drunk [false]",
			"distortSignText: false",
			"",
			"# Enclose a text with these Letters to bypass Chat Distortion (Use \",\" as Separator) (list) [- '[,]']",
			"distortBypass:",
			"- '*,*'",
			"- '[,]'",
			""
			};
		int index = indexOfStart("# words");
		if (index == -1) {
			index = indexOfStart("# Will be processed");
		}
		if (index == -1) {
			index = indexOfStart("# replace");
		}
		if (index == -1) {
			index = indexOfStart("words:");
		}
		if (index == -1) {
			appendLines(entries);
		} else {
			addLines(index, entries);
		}

		// Add some new separators for overview
		String line = "# -- Settings --";
		index = indexOfStart("# Settings");
		if (index != -1) {
			setLine(index, line);
		}

		line = "# -- Recipes for Potions --";
		index = indexOfStart("# Recipes for Potions");
		if (index != -1) {
			setLine(index, line);
		}
	}

	// Updates de from 1.1 to 1.2
	private void update11de() {
		updateVersion("1.2");

		int index = indexOfStart("# Das Item kann nicht aufgesammelt werden");
		if (index != -1) {
			setLine(index, "# Das Item kann nicht aufgesammelt werden und bleibt bis zum Despawnen liegen. (Achtung: Kann nach Serverrestart aufgesammelt werden!)");
		}

		// Add the BarrelAccess Setting
		String[] lines = {
				"# Ob große Fässer an jedem Block geöffnet werden können, nicht nur an Zapfhahn und Schild. Bei kleinen Fässern geht dies immer. [true]",
				"openLargeBarrelEverywhere: true",
				""
		};
		index = indexOfStart("colorInBrewer") + 2;
		if (index == 1) {
			index = indexOfStart("colorInBarrels") + 2;
		}
		if (index == 1) {
			index = indexOfStart("# Autosave");
		}
		if (index == -1) {
			index = indexOfStart("language") + 2;
		}
		if (index == 1) {
			addLines(3, lines);
		} else {
			addLines(index, lines);
		}

		// Add Plugin Support Settings
		lines = new String[] {
				"",
				"# -- Plugin Kompatiblität --",
				"",
				"# Andere Plugins (wenn installiert) nach Rechten zum öffnen von Fässern checken [true]",
				"useWorldGuard: true",
				"useLWC: true",
				"useGriefPrevention: true",
				"",
				"# Änderungen an Fassinventaren mit LogBlock aufzeichen [true]",
				"useLogBlock: true",
				"",
				""
		};
		index = indexOfStart("# -- Chat Veränderungs Einstellungen");
		if (index == -1) {
			index = indexOfStart("# words");
		}
		if (index == -1) {
			index = indexOfStart("distortCommands");
			if (index > 4) {
				index -= 4;
			}
		}
		if (index != -1) {
			addLines(index, lines);
		} else {
			appendLines(lines);
		}
	}

	// Updates en from 1.1 to 1.2
	private void update11en() {
		updateVersion("1.2");

		int index = indexOfStart("# The item can not be collected");
		if (index != -1) {
			setLine(index, "# The item can not be collected and stays on the ground until it despawns. (Warning: Can be collected after Server restart!)");
		}

		// Add the BarrelAccess Setting
		String[] lines = {
				"# If a Large Barrel can be opened by clicking on any of its blocks, not just Spigot or Sign. This is always true for Small Barrels. [true]",
				"openLargeBarrelEverywhere: true",
				""
		};
		index = indexOfStart("colorInBrewer") + 2;
		if (index == 1) {
			index = indexOfStart("colorInBarrels") + 2;
		}
		if (index == 1) {
			index = indexOfStart("# Autosave");
		}
		if (index == -1) {
			index = indexOfStart("language") + 2;
		}
		if (index == 1) {
			addLines(3, lines);
		} else {
			addLines(index, lines);
		}

		// Add Plugin Support Settings
		lines = new String[] {
				"",
				"# -- Plugin Compatibility --",
				"",
				"# Enable checking of other Plugins (if installed) for Barrel Permissions [true]",
				"useWorldGuard: true",
				"useLWC: true",
				"useGriefPrevention: true",
				"",
				"# Enable the Logging of Barrel Inventories to LogBlock [true]",
				"useLogBlock: true",
				"",
				""
		};
		index = indexOfStart("# -- Chat Distortion Settings");
		if (index == -1) {
			index = indexOfStart("# words");
		}
		if (index == -1) {
			index = indexOfStart("distortCommands");
			if (index > 4) {
				index -= 4;
			}
		}
		if (index != -1) {
			addLines(index, lines);
		} else {
			appendLines(lines);
		}
	}

	// Update de from 1.2 to 1.3
	private void update12de() {
		updateVersion("1.3");

		// Add the new Wood Types to the Description
		int index = indexOfStart("# wood:");
		if (index != -1) {
			setLine(index, "# wood: Holz des Fasses 0=alle Holzsorten 1=Birke 2=Eiche 3=Jungel 4=Fichte 5=Akazie 6=Schwarzeiche");
		}

		// Add the Example to the Cooked Section
		index = indexOfStart("# cooked:");
		if (index != -1) {
			addLines(index + 1, "# [Beispiel] MATERIAL_oder_id: Name nach Gähren");
		}

		// Add new ingredients description
		String replacedLine = "# ingredients: Auflistung von 'Material oder ID,Data/Anzahl'";
		String[] lines = new String[] {
				"#   (Item-ids anstatt Material werden von Bukkit nicht mehr unterstützt und funktionieren möglicherweise in Zukunft nicht mehr!)",
				"#   Eine Liste von allen Materialien kann hier gefunden werden: http://jd.bukkit.org/beta/apidocs/org/bukkit/Material.html",
				"#   Es kann ein Data-Wert angegeben werden, weglassen ignoriert diesen beim hinzufügen einer Zutat"
		};
		index = indexOfStart("# ingredients:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# name:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			} else {
				index = indexOfStart("# -- Rezepte für Getränke --");
				if (index != -1) {
					addLines(index + 2, lines);
					addLines(index + 2, "", replacedLine);
				}
			}
		}

		// Split the Color explanation into two lines
		replacedLine = "# color: Farbe des Getränks nach destillieren/reifen.";
		lines = new String[] {
				"#   Benutzbare Farben: DARK_RED, RED, BRIGHT_RED, ORANGE, PINK, BLUE, CYAN, WATER, GREEN, BLACK, GREY, BRIGHT_GREY"
		};

		index = indexOfStart("# color:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# age:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			}
		}

		// Add all the new info to the effects description
		replacedLine = "# effects: Auflistung Effekt/Level/Dauer  Besonderere Trank-Effekte beim Trinken, Dauer in sek.";
		lines = new String[] {
				"#   Ein 'X' an den Namen anhängen, um ihn zu verbergen. Bsp: 'POISONX/2/10' (WEAKNESS, INCREASE_DAMAGE, SLOW und SPEED sind immer verborgen.)",
				"#   Mögliche Effekte: http://jd.bukkit.org/rb/apidocs/org/bukkit/potion/PotionEffectType.html",
				"#   Minimale und Maximale Level/Dauer können durch \"-\" festgelegt werden, Bsp: 'SPEED/1-2/30-40' = Level 1 und 30 sek minimal, Level 2 und 40 sek maximal",
				"#   Diese Bereiche funktionieren auch umgekehrt, Bsp: 'POISON/3-1/20-5' für abschwächende Effekte bei guter Qualität",
				"#   Längste mögliche Effektdauer: 1638 sek. Es muss keine Dauer für Effekte mit sofortiger Wirkung angegeben werden."
		};

		index = indexOfStart("# effects:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# alcohol:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			} else {
				index = indexOfStart("# -- Rezepte für Getränke --");
				if (index != -1) {
					addLines(index + 2, lines);
					addLines(index + 2, "", replacedLine);
				}
			}
		}
		if (index != -1) {
			index = indexOfStart("#   (WEAKNESS, INCREASE_DAMAGE, SLOW und SPEED sind immer verborgen.)  Mögliche Effekte:");
			if (index != -1) {
				config.remove(index);
			}
		}
		index = indexOfStart("#   Bei Effekten mit sofortiger Wirkung ");
		if (index != -1) {
			config.remove(index);
		}

	}

	// Update en from 1.2 to 1.3
	private void update12en() {
		updateVersion("1.3");

		// Add the new Wood Types to the Description
		int index = indexOfStart("# wood:");
		if (index != -1) {
			setLine(index, "# wood: Wood of the barrel 0=any 1=Birch 2=Oak 3=Jungle 4=Spruce 5=Acacia 6=Dark Oak");
		}

		// Add the Example to the Cooked Section
		index = indexOfStart("# cooked:");
		if (index != -1) {
			addLines(index + 1, "# [Example] MATERIAL_or_id: Name after cooking");
		}

		// Add new ingredients description
		String replacedLine = "# ingredients: List of 'material or id,data/amount'";
		String[] lines = new String[] {
				"#   (Item-ids instead of material are deprecated by bukkit and may not work in the future!)",
				"#   A list of materials can be found here: http://jd.bukkit.org/beta/apidocs/org/bukkit/Material.html",
				"#   You can specify a data value, omitting it will ignore the data value of the added ingredient"
		};
		index = indexOfStart("# ingredients:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# name:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			} else {
				index = indexOfStart("# -- Recipes for Potions --");
				if (index != -1) {
					addLines(index + 2, lines);
					addLines(index + 2, "", replacedLine);
				}
			}
		}

		// Split the Color explanation into two lines
		replacedLine = "# color: Color of the potion after distilling/aging.";
		lines = new String[] {
				"#   Usable Colors: DARK_RED, RED, BRIGHT_RED, ORANGE, PINK, BLUE, CYAN, WATER, GREEN, BLACK, GREY, BRIGHT_GREY"
		};

		index = indexOfStart("# color:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# age:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			}
		}

		// Add all the new info to the effects description
		replacedLine = "# effects: List of effect/level/duration  Special potion-effect when drinking, duration in sek.";
		lines = new String[] {
				"#   Suffix name with 'X' to hide effect from label. Sample: 'POISONX/2/10' (WEAKNESS, INCREASE_DAMAGE, SLOW and SPEED are always hidden.)",
				"#   Possible Effects: http://jd.bukkit.org/rb/apidocs/org/bukkit/potion/PotionEffectType.html",
				"#   Level or Duration ranges may be specified with a \"-\", ex. 'SPEED/1-2/30-40' = lvl 1 and 30 sec at worst and lvl 2 and 40 sec at best",
				"#   Ranges also work high-low, ex. 'POISON/3-1/20-5' for weaker effects at good quality.",
				"#   Highest possible Duration: 1638 sec. Instant Effects dont need any duration specified."
		};

		index = indexOfStart("# effects:");
		if (index != -1) {
			setLine(index, replacedLine);
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("# alcohol:");
			if (index != -1) {
				addLines(index + 1, lines);
				addLines(index + 1, replacedLine);
			} else {
				index = indexOfStart("# -- Recipes for Potions --");
				if (index != -1) {
					addLines(index + 2, lines);
					addLines(index + 2, "", replacedLine);
				}
			}
		}
		if (index != -1) {
			index = indexOfStart("#   (WEAKNESS, INCREASE_DAMAGE, SLOW and SPEED are always hidden.)  Possible Effects:");
			if (index != -1) {
				config.remove(index);
			}
		}
		index = indexOfStart("#   instant effects ");
		if (index != -1) {
			config.remove(index);
		}

	}

	// Update de from 1.3 to 1.3.1
	private void update13de() {
		updateVersion("1.3.1");

		int index = indexOfStart("# Autosave");
		String[] lines = new String[] { "# Aktiviert das Suchen nach Updates für Brewery mit der curseforge api [true]",
				"# Wenn ein Update gefunden wurde, wird dies bei Serverstart im log angezeigt, sowie ops benachrichtigt",
				"updateCheck: true",
				"" };

		if (index == -1) {
			index = indexOfStart("autosave:");
			if (index == -1) {
				index = indexOfStart("# Sprachedatei");
				if (index == -1) {
					index = indexOfStart("language:");
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index, lines);
		}
	}

	// Update en from 1.3 to 1.3.1
	private void update13en() {
		updateVersion("1.3.1");

		int index = indexOfStart("# Autosave");
		String[] lines = new String[] { "# Enable checking for Updates, Checks the curseforge api for updates to Brewery [true]",
				"# If an Update is found a Message is logged on Server-start and displayed to ops joining the game",
				"updateCheck: true",
				"" };

		if (index == -1) {
			index = indexOfStart("autosave:");
			if (index == -1) {
				index = indexOfStart("# Languagefile");
				if (index == -1) {
					index = indexOfStart("language:");
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index, lines);
		}
	}

	// Update de from 1.3.1 to 1.4
	private void update131de() {
		updateVersion("1.4");

		int index = indexOfStart("# SamplePlugin = installiertes home plugin. Unterstützt: ManagerXL.");
		if (index != -1) {
			config.remove(index);
		}

		index = indexOfStart("# Ob der Spieler nach etwas kürzerem Ausloggen an einem zufälligen Ort \"aufwacht\" (diese müssen durch '/br Wakeup add");
		if (index != -1) {
			setLine(index, "# Ob der Spieler nach etwas kürzerem Ausloggen an einem zufälligen Ort \"aufwacht\" (diese müssen durch '/brew Wakeup add' von einem Admin festgelegt werden)");
		}

		index = indexOfStart("# Ob der Spieler sich bei großer Trunkenheit teilweise nicht einloggen kann und kurz warten muss, da sein Charakter nicht reagiert");
		if (index != -1) {
			setLine(index, "# Ob der Spieler bei großer Trunkenheit mehrmals probieren muss sich einzuloggen, da sein Charakter kurz nicht reagiert [true]");
		}

		index = indexOfStart("# Ob der Spieler sich übertrinken kann und dann in Ohnmacht fällt (gekickt wird)");
		if (index != -1) {
			setLine(index, "# Ob der Spieler kurz in Ohnmacht fällt (vom Server gekickt wird) wenn er die maximale Trunkenheit erreicht [false]");
		}

		index = indexOfStart("# Das Item kann nicht aufgesammelt werden und bleibt bis zum Despawnen liegen. (Achtung:");
		if (index != -1) {
			setLine(index, "# Das Item kann nicht aufgesammelt werden und bleibt bis zum Despawnen liegen.");
		}

		String[] lines = new String[] { "",
				"# Zeit in Sekunden bis die pukeitems despawnen, (mc standard wäre 300 = 5 min) [60]",
				"# Wurde die item Despawnzeit in der spigot.yml verändert, verändert sich auch die pukeDespawnzeit in Abhängigkeit.",
				"pukeDespawntime: 60" };

		index = indexOfStart("pukeItem:");
		if (index == -1) {
			index = indexOfStart("enablePuke:");
			if (index == -1) {
				index = indexOfStart("# Konsumierbares Item") - 1;
				if (index == -2) {
					index = indexOfStart("enableKickOnOverdrink:");
					if (index == -1) {
						index = indexOfStart("language:");
					}
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index + 1, lines);
		}

		index = indexOfStart("# Färben der Iteminformationen je nach Qualität während sie sich 1. im Fass und/oder 2. im Braustand befinden [true, false]");
		if (index != -1) {
			setLine(index, "# Färben der Iteminformationen je nach Qualität während sie sich 1. im Fass und/oder 2. im Braustand befinden [true, true]");
		}

		index = indexOfStart("# Wenn ein Update gefunden wurde, wird dies bei Serverstart im log angezeigt, sowie ops benachrichtigt");
		if (index != -1) {
			setLine(index, "# Wenn ein Update gefunden wurde, wird dies bei Serverstart im log angezeigt, sowie OPs benachrichtigt");
		}

		index = indexOfStart("#   Eine Liste von allen Materialien kann hier gefunden werden: http://jd.bukkit.org");
		if (index != -1) {
			setLine(index, "#   Eine Liste von allen Materialien kann hier gefunden werden: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
		}

		lines = new String[] { "#   Wenn Vault installiert ist können normale englische Item Namen verwendet werden, anstatt Material, ID und Data!",
				"#   Vault erkennt Namen wie \"Jungle Leaves\" anstatt \"LEAVES,3\". Dies macht es viel einfacher!" };

		index = indexOfStart("#   Es kann ein Data-Wert angegeben werden, weglassen");
		if (index != -1) {
			setLine(index, "#   Es kann ein Data-Wert (durability) angegeben werden, weglassen ignoriert diesen beim hinzufügen einer Zutat");
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("#   Eine Liste von allen Materialien kann hier");
			if (index == -1) {
				index = indexOfStart("# cookingtime: ") - 1;
				if (index == -2) {
					index = indexOfStart("# ingredients: Auflistung von");
					if (index == -1) {
						index = indexOfStart("# -- Rezepte für Getränke --") + 1;
						if (index == 0) {
							index = indexOfStart("# -- Verschiedene Einstellungen --");
						}
					}
				}
			}
			if (index == -1) {
				appendLines(lines);
			} else {
				addLines(index + 1, lines);
			}
		}

		lines = new String[] { "#   Effekte sind ab der 1.9 immer verborgen, wegen Änderungen an den Tränken." };
		index = indexOfStart("#   Mögliche Effekte: http://jd.bukkit.org");
		if (index != -1) {
			setLine(index, "#   Mögliche Effekte: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html");
			addLines(index, lines);
		} else {
			index = indexOfStart("#   Ein 'X' an den Namen anhängen, um");
			if (index == -1) {
				index = indexOfStart("# effects: ");
				if (index == -1) {
					index = indexOfStart("# -- Rezepte für Getränke --") + 1;
				}
			}
			if (index == 0) {
				appendLines(lines);
			} else {
				addLines(index + 1, lines);
			}
		}

		index = indexOfStart("# Text, der zwischen diesen Buchstaben");
		if (index != -1) {
			setLine(index, "# Im Chat geschriebener Text, der zwischen diesen Buchstaben steht, wird nicht verändert (\",\" als Trennung verwenden) (Liste) [- '[,]']");
		}
	}

	// Update en from 1.3.1 to 1.4
	private void update131en() {
		updateVersion("1.4");

		int index = indexOfStart("# SamplePlugin = installed home plugin. Supports: ManagerXL.");
		if (index != -1) {
			config.remove(index);
		}

		index = indexOfStart("# If the player \"wakes up\" at a random place when offline for some time while drinking (the places have to be defined with '/br Wakeup add'");
		if (index != -1) {
			setLine(index, "# If the player \"wakes up\" at a random place when offline for some time while drinking (the places have to be defined with '/brew Wakeup add' through an admin)");
		}

		index = indexOfStart("# If the Player may get some logins denied, when his character is drunk");
		if (index != -1) {
			setLine(index, "# If the Player may have to try multiple times when logging in while extremely drunk [true]");
		}

		index = indexOfStart("# If the Player faints (gets kicked) for some minutes if he overdrinks");
		if (index != -1) {
			setLine(index, "# If the Player faints shortly (gets kicked from the server) if he drinks the max amount of alcohol possible [false]");
		}

		index = indexOfStart("# The item can not be collected and stays on the ground until it despawns. (Warning:");
		if (index != -1) {
			setLine(index, "# The item can not be collected and stays on the ground until it despawns.");
		}

		String[] lines = new String[] { "",
				"# Time in seconds until the pukeitems despawn, (mc default is 300 = 5 min) [60]",
				"# If the item despawn time was changed in the spigot.yml, the pukeDespawntime changes as well.",
				"pukeDespawntime: 60" };

		index = indexOfStart("pukeItem:");
		if (index == -1) {
			index = indexOfStart("enablePuke:");
			if (index == -1) {
				index = indexOfStart("# Consumable Item") - 1;
				if (index == -2) {
					index = indexOfStart("enableKickOnOverdrink:");
					if (index == -1) {
						index = indexOfStart("language:");
					}
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index + 1, lines);
		}

		index = indexOfStart("# Color the Item information (lore) depending on quality while it is 1. in a barrel and/or 2. in a brewing stand [true, false]");
		if (index != -1) {
			setLine(index, "# Color the Item information (lore) depending on quality while it is 1. in a barrel and/or 2. in a brewing stand [true, true]");
		}

		index = indexOfStart("# If an Update is found a Message is logged on Server-start and displayed to ops joining the game");
		if (index != -1) {
			setLine(index, "# If an Update is found a Message is logged on Server-start and displayed to OPs joining the game");
		}

		index = indexOfStart("#   A list of materials can be found here: http://jd.bukkit.org");
		if (index != -1) {
			setLine(index, "#   A list of materials can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
		}

		lines = new String[] { "#   If Vault is installed normal names can be used instead of material or id, so using Vault is highly recommended.",
				"#   Vault will recognize things like \"Jungle Leaves\" instead of \"LEAVES,3\"" };

		index = indexOfStart("#   You can specify a data value, omitting");
		if (index != -1) {
			setLine(index, "#   You can specify a data (durability) value, omitting it will ignore the data value of the added ingredient");
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("#   A list of materials can be found");
			if (index == -1) {
				index = indexOfStart("# cookingtime: Time in real minutes") - 1;
				if (index == -2) {
					index = indexOfStart("# ingredients: ");
					if (index == -1) {
						index = indexOfStart("# -- Recipes for Potions --") + 1;
						if (index == 0) {
							index = indexOfStart("# -- Settings --");
						}
					}
				}
			}
			if (index == -1) {
				appendLines(lines);
			} else {
				addLines(index + 1, lines);
			}
		}

		lines = new String[] { "#   Effects are always hidden in 1.9 and newer, because of changes in the potion mechanics." };
		index = indexOfStart("#   Possible Effects: http://jd.bukkit.org");
		if (index != -1) {
			setLine(index, "#   Possible Effects: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html");
			addLines(index, lines);
		} else {
			index = indexOfStart("#   Suffix name with");
			if (index == -1) {
				index = indexOfStart("# effects: ");
				if (index == -1) {
					index = indexOfStart("# -- Recipes for Potions --") + 1;
				}
			}
			if (index == 0) {
				appendLines(lines);
			} else {
				addLines(index + 1, lines);
			}
		}

		index = indexOfStart("# Enclose a text with these Letters to bypass Chat Distortion");
		if (index != -1) {
			setLine(index, "# Enclose a Chat text with these Letters to bypass Chat Distortion (Use \",\" as Separator) (list) [- '[,]']");
		}

	}

	// Update de from 1.4 to 1.5
	private void update14de() {
		updateVersion("1.5");

		String[] lines = new String[] {"",
				"# Ob geschriebener Chat bei großer Trunkenheit abgefälscht werden soll,",
				"# so dass es etwas betrunken aussieht was geschrieben wird.",
				"# Wie stark der Chat verändert wird hängt davon ab wie betrunken der Spieler ist",
				"# Unten kann noch eingestellt werden wie und was verändert wird",
				"enableChatDistortion: true"};

		int index = indexOfStart("# -- Chat") + 2;
		if (index == 1) {
			index = indexOfStart("distortCommands:") - 1;
			if (index == -2) {
				index = indexOfStart("distortSignText:") - 1;
				if (index == -2) {
					index = indexOfStart("# words:");
					if (index == -1) {
						index = indexOfStart("words:");
					}
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index - 1, lines);
		}

		lines = new String[] {"# Also zum Beispiel im Chat: Hallo ich bin betrunken *Ich teste Brewery*"};

		index = indexOfStart("# Im Chat geschriebener Text, der zwischen");
		if (index != -1) {
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("distortBypass:");
			if (index != -1) {
				addLines(index, lines);
			}
		}

		lines = new String[] {"# distilltime: Wie lange (in sekunden) ein Destillations-Durchlauf braucht (0=Standard Zeit von 40 sek) MC Standard wäre 20 sek"};

		index = indexOfStart("# distillruns:");
		if (index == -1) {
			index = indexOfStart("# wood:") - 1;
			if (index == -2) {
				index = indexOfStart("# -- Rezepte") + 1;
				if (index == 0) {
					index = -1;
				}
			}
		}
		if (index != -1) {
			addLines(index + 1, lines);
		}

		index = indexOfStart("      name: Schlechtes Beispiel/Beispiel/Gutes Beispiel");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 60");
		}
		index = indexOfStart("      name: Bitterer Rum/Würziger Rum/&6Goldener Rum");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 30");
		}
		index = indexOfStart("      name: minderwertiger Absinth/Absinth/Starker Absinth");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 80");
		}
	}

	// Update de from 1.4 to 1.5
	private void update14en() {
		updateVersion("1.5");

		String[] lines = new String[] {"",
				"# If written Chat is distorted when the Player is Drunk,",
				"# so that it looks like drunk writing",
				"# How much the chat is distorted depends on how drunk the Player is",
				"# Below are settings for what and how changes in chat occur",
				"enableChatDistortion: true"};

		int index = indexOfStart("# -- Chat") + 2;
		if (index == 1) {
			index = indexOfStart("distortCommands:") - 1;
			if (index == -2) {
				index = indexOfStart("distortSignText:") - 1;
				if (index == -2) {
					index = indexOfStart("# words:");
					if (index == -1) {
						index = indexOfStart("words:");
					}
				}
			}
		}
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index - 1, lines);
		}

		lines = new String[] {"# Chat Example: Hello i am drunk *I am testing Brewery*"};

		index = indexOfStart("# Enclose a Chat text with these Letters");
		if (index != -1) {
			addLines(index + 1, lines);
		} else {
			index = indexOfStart("distortBypass:");
			if (index != -1) {
				addLines(index, lines);
			}
		}

		lines = new String[] {"# distilltime: How long (in seconds) one distill-run takes (0=Default time of 40 sec) MC Default would be 20 sec"};

		index = indexOfStart("# distillruns:");
		if (index == -1) {
			index = indexOfStart("# wood:") - 1;
			if (index == -2) {
				index = indexOfStart("# -- Recipes") + 1;
				if (index == 0) {
					index = -1;
				}
			}
		}
		if (index != -1) {
			addLines(index + 1, lines);
		}

		index = indexOfStart("      name: Bad Example/Example/Good Example");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 60");
		}
		index = indexOfStart("      name: Bitter Rum/Spicy Rum/&6Golden Rum");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 30");
		}
		index = indexOfStart("      name: Poor Absinthe/Absinthe/Strong Absinthe");
		if (index != -1) {
			addLines(index + 1, "      distilltime: 80");
		}
	}


	//update from 1.5 to 1.7/mc 1.13
	private void update15(boolean mc113, boolean langDE) {
		updateVersion("1.7");
		updateMaterials(mc113);

		if (langDE) {

			int index = indexOfStart("# ingredients: Auflistung von 'Material oder ID");
			if (index != -1) {
				setLine(index, "# ingredients: Auflistung von 'Material,Data/Anzahl'");
			}

			index = indexOfStart("#   (Item-ids anstatt Material");
			if (index != -1) {
				setLine(index, "#   (Item-ids anstatt Material können in Bukkit nicht mehr benutzt werden)");
			}

			index = indexOfStart("# [Beispiel] MATERIAL_oder_id: Name");
			if (index != -1) {
				setLine(index, "# [Beispiel] MATERIAL: Name nach Gähren");
			}

		} else {

			int index = indexOfStart("# ingredients: List of 'material or id");
			if (index != -1) {
				setLine(index, "# ingredients: List of 'material,data/amount'");
			}

			index = indexOfStart("#   (Item-ids instead of material are deprecated");
			if (index != -1) {
				setLine(index, "#   (Item-ids instead of material are not supported by bukkit anymore and will not work)");
			}

			index = indexOfStart("# [Example] MATERIAL_or_id: Name");
			if (index != -1) {
				setLine(index, "# [Example] MATERIAL: Name after cooking");
			}

		}
	}

	// Update de from 1.7 to 1.8
	private void update17de() {
		updateVersion("1.8");

		int index = indexOfStart("openLargeBarrelEverywhere");
		if (index == -1) {
			index = indexOfStart("colorInBrewer");
			if (index == -1) {
				index = indexOfStart("colorInBarrels");
				if (index == -1) {
					index = indexOfStart("hangoverDays");
					if (index == -1) {
						index = indexOfStart("language");
					}
				}
			}
		}
		String[] lines = {"",
			"# Wie viele Brewery Getränke in die Minecraft Fässer getan werden können [6]",
			"maxBrewsInMCBarrels: 6"};
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index + 1, lines);
		}

		index = indexOfStart("#   Benutzbare Farben");
		if (index == -1) {
			index = indexOfStart("# color:");
		}
		if (index != -1) {
			addLines(index + 1, "#   Oder RGB Farben (Hex: also zB '99FF33') (Ohne #) (mit '') (Einfach nach \"HTML color\" im Internet suchen)");
		}

		index = indexOfStart("# ingredients:");
		if (index == -1) {
			index = indexOfStart("#   Eine Liste von allen Materialien");
			if (index == -1) {
				index = indexOfStart("# -- Rezepte");
			}
		}
		if (index != -1) {
			addLines(index + 1, "#   Halte ein Item in der Hand und benutze /brew ItemName um dessen Material herauszufinden und für ein Rezept zu benutzen");
		}
		index = indexOfStart("# wood: Holz des Fasses");
		if (index != -1) {
			addLines(index + 1, "#   Das Minecraft Fass besteht aus Eiche");
		}
		if (P.use1_13) updateMaterialDescriptions(true);
	}

	// Update en from 1.7 to 1.8
	private void update17en() {
		updateVersion("1.8");

		int index = indexOfStart("openLargeBarrelEverywhere");
		if (index == -1) {
			index = indexOfStart("colorInBrewer");
			if (index == -1) {
				index = indexOfStart("colorInBarrels");
				if (index == -1) {
					index = indexOfStart("hangoverDays");
					if (index == -1) {
						index = indexOfStart("language");
					}
				}
			}
		}
		String[] lines = {"",
			"# How many Brewery drinks can be put into the Minecraft barrels [6]",
			"maxBrewsInMCBarrels: 6"};
		if (index == -1) {
			appendLines(lines);
		} else {
			addLines(index + 1, lines);
		}

		index = indexOfStart("#   Usable Colors");
		if (index == -1) {
			index = indexOfStart("# color:");
		}
		if (index != -1) {
			addLines(index + 1, "#   Or RGB colors (hex: for example '99FF33') (with '') (search for \"HTML color\" on the internet)");
		}

		index = indexOfStart("# ingredients:");
		if (index == -1) {
			index = indexOfStart("#   A list of materials");
			if (index == -1) {
				index = indexOfStart("# -- Recipes");
			}
		}
		if (index != -1) {
			addLines(index + 1, "#   With an item in your hand, use /brew ItemName to get its material for use in a recipe");
		}
		index = indexOfStart("# wood: Wood of the barrel");
		if (index != -1) {
			addLines(index + 1, "#   The Minecraft barrel is made of oak");
		}
		if (P.use1_13) updateMaterialDescriptions(false);
	}

	// Update all Materials to Minecraft 1.13
	private void updateMaterials(boolean toMC113) {
		int index;
		if (toMC113) {
			index = indexOfStart("oldMat:");
			if (index != -1) {
				config.remove(index);
			}
		} else {
			index = indexOfStart("version:");
			if (index != -1) {
				addLines(index + 1, "oldMat: true");
			}
		}

		index = indexOfStart("pukeItem: ");
		String line;
		if (index != -1) {
			line = config.get(index);
			if (line.length() > 10) {
				setLine(index, convertMaterial(line, "pukeItem: ", "", toMC113));
			}
		}

		index = indexOfStart("drainItems:");
		if (index != -1) {
			index++;
			while (config.get(index).startsWith("-")) {
				setLine(index, convertMaterial(config.get(index), "- ", "(,.*|)/.*", toMC113));
				index++;
			}
		}

		index = indexOfStart("recipes:");
		if (index != -1) {
			index++;
			int endIndex = indexOfStart("useWorldGuard:");
			if (endIndex < index) {
				endIndex = indexOfStart("enableChatDistortion:");
			}
			if (endIndex < index) {
				endIndex = indexOfStart("words:");
			}
			if (endIndex < index) {
				endIndex = config.size();
			}
			while (index < endIndex) {
				if (config.get(index).matches("^\\s+ingredients:.*")) {
					index++;
					while (config.get(index).matches("^\\s+- .+")) {
						line = config.get(index);
						setLine(index, convertMaterial(line, "^\\s+- ", "(,.*|)/.*", toMC113));
						index++;
					}
				} else if (config.get(index).startsWith("cooked:")) {
					index++;
					while (config.get(index).matches("^\\s\\s+.+")) {
						line = config.get(index);
						setLine(index, convertMaterial(line, "^\\s\\s+", ":.*", toMC113));
						index++;
					}
				}
				index++;
			}
		}
	}

	private String convertMaterial(String line, String regexPrefix, String regexPostfix, boolean toMC113) {
		if (!toMC113) {
			return convertIdtoMaterial(line, regexPrefix, regexPostfix);
		}
		String mat = line.replaceFirst(regexPrefix, "").replaceFirst(regexPostfix, "");
		Material material;
		if (mat.equalsIgnoreCase("LONG_GRASS")) {
			material = Material.GRASS;
		} else {
			material = Material.matchMaterial(mat, true);
		}

		if (material == null) {
			return line;
		}
		String matnew = material.name();
		if (!mat.equalsIgnoreCase(matnew)) {
			return line.replaceAll(mat, matnew);
		} else {
			return line;
		}
	}

	private String convertIdtoMaterial(String line, String regexPrefix, String regexPostfix) {
		String idString = line.replaceFirst(regexPrefix, "").replaceFirst(regexPostfix, "");
		int id = P.p.parseInt(idString);
		if (id > 0) {
			Material material = LegacyUtil.getMaterial(id);
			if (material == null) {
				P.p.errorLog("Could not find Material with id: " + line);
				return line;
			} else {
				return line.replaceAll(idString, material.name());
			}
		} else {
			return line;
		}
	}

	private void updateMaterialDescriptions(boolean de) {
		int index;
		if (de) {
			index = indexOfStart("# ingredients: Auflistung von 'Material,Data/Anzahl'");
			if (index != -1) {
				setLine(index, "# ingredients: Auflistung von 'Material/Anzahl'");
			}

			index = indexOfStart("#   Es kann ein Data-Wert (durability) angegeben werden");
			if (index != -1) {
				config.remove(index);
			}

			index = indexOfStart("#   Wenn Vault installiert ist");
			if (index != -1) {
				config.remove(index);
			}

			index = indexOfStart("#   Vault erkennt Namen wie");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#     - Jungle Leaves/64  # Nur mit Vault");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#     - Green Dye/6       # Nur mit Vault");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#   Ein 'X' an den Namen");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#   Effekte sind ab der 1.9 immer verborgen");
			if (index != -1) {
				config.remove(index);
			}
		} else {
			index = indexOfStart("# ingredients: List of 'material,data/amount'");
			if (index != -1) {
				setLine(index, "# ingredients: List of 'material/amount'");
			}

			index = indexOfStart("#   You can specify a data (durability) value");
			if (index != -1) {
				config.remove(index);
			}

			index = indexOfStart("#   If Vault is installed normal names can be used");
			if (index != -1) {
				config.remove(index);
			}

			index = indexOfStart("#   Vault will recognize things");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#     - Jungle Leaves/64  # Only with Vault");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#     - Green Dye/6       # Only with Vault");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#   Suffix name with 'X' to hide effect");
			if (index != -1) {
				config.remove(index);
			}
			index = indexOfStart("#   Effects are always hidden in 1.9 and newer");
			if (index != -1) {
				config.remove(index);
			}
		}
	}


}
