package com.dre.brewery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigUpdater {

	private ArrayList<String> config = new ArrayList<String>();
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
	public void addLines(int index, String... newLines) {
		config.addAll(index, Arrays.asList(newLines));
	}

	public void saveConfig() {
		StringBuilder stringBuilder = new StringBuilder("");
		for (String line : config) {
			stringBuilder.append(line + "\n");
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



	// ---- Updating to newer Versions ----

	// Update from a specified Config version and language to the newest version
	public void update(String fromVersion, String lang) {
		if (fromVersion.equals("0.5")) {
			// Version 0.5 was only released for de, but with en as setting, so default to de
			if (!lang.equals("de")) {
				lang = "de";
			}
		}
		if (fromVersion.equals("0.5") || fromVersion.equals("1.0")) {
			if (lang.equals("de")) {
				update05de();
			} else {
				update10en();
			}
		} else {
			P.p.log(P.p.languageReader.get("Error_ConfigUpdate", fromVersion));
			return;
		}
		saveConfig();
	}

	// Updates de from 0.5 to 1.1
	private void update05de() {
		// Update version String
		int index = indexOfStart("version");
		String line = "version: '1.1'";
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

		// Default language to de
		index = indexOfStart("language: en");
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
		line = "# -- Verschiedene Einstellungen --";
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
		int index = indexOfStart("version");
		String line = "version: '1.1'";
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
		index = indexOfStart("# words");
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
		line = "# -- Settings --";
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

}
