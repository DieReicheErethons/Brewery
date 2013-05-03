package com.dre.brewery;

import java.util.ArrayList;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.dre.brewery.BPlayer;

public class Words {

	// represends Words and letters, that are replaced in drunk players messages

	public static ArrayList<Words> words = new ArrayList<Words>();
	public static FileConfiguration config;

	private String from;
	private String to;
	private String[] pre;
	private Boolean match = false;
	private int alcohol = 1;
	private int percentage = 100;

	public Words(Map<?, ?> part) {
		for (Map.Entry<?, ?> wordPart : part.entrySet()) {
			String key = (String) wordPart.getKey();

			if (wordPart.getValue() instanceof String) {

				if (key.equalsIgnoreCase("replace")) {
					this.from = (String) wordPart.getValue();
				} else if (key.equalsIgnoreCase("to")) {
					this.to = (String) wordPart.getValue();
				} else if (key.equalsIgnoreCase("pre")) {
					String fullPre = (String) wordPart.getValue();
					this.pre = fullPre.split(",");
				}

			} else if (wordPart.getValue() instanceof Boolean) {

				if (key.equalsIgnoreCase("match")) {
					this.match = (Boolean) wordPart.getValue();
				}

			} else if (wordPart.getValue() instanceof Integer) {

				if (key.equalsIgnoreCase("alcohol")) {
					this.alcohol = (Integer) wordPart.getValue();
				} else if (key.equalsIgnoreCase("percentage")) {
					this.percentage = (Integer) wordPart.getValue();
				}

			}
		}
		if (this.from != null && this.to != null) {
			words.add(this);
		}
	}

	// Distort players words when he talks
	public static void playerChat(AsyncPlayerChatEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer().getName());
		if (bPlayer != null) {
			if (words.isEmpty()) {
				// load when first drunk player talks
				load();
			}
			if (!words.isEmpty()) {
				String message = event.getMessage();
				for (Words w : words) {
					if (w.alcohol <= bPlayer.getDrunkeness()) {
						message = distort(message, w.from, w.to, w.pre, w.match, w.percentage);
					}
				}
				event.setMessage(message);
			}
		}
	}

	// replace "percent"% of "from" -> "to" in "words", when the string before
	// each "from" "match"es "pre"
	// Not yet ignoring case :(
	public static String distort(String words, String from, String to, String[] pre, boolean match, int percent) {
		if (from.equalsIgnoreCase("-end")) {
			from = words;
			to = words + to;
		} else if (from.equalsIgnoreCase("-start")) {
			from = words;
			to = to + words;
		} else if (from.equalsIgnoreCase("-space")) {
			from = " ";
		} else if (from.equalsIgnoreCase("-random")) {
			// inserts "to" on a random position in "words"
			int charIndex = (int) (Math.random() * (words.length() - 1));
			if (charIndex > words.length() / 2) {
				from = words.substring(charIndex);
				to = to + from;
			} else {
				from = words.substring(0, charIndex);
				to = from + to;
			}
		}

		if (words.contains(from)) {
			// some characters (*,?) disturb split() which then throws
			// PatternSyntaxException
			try {
				if (pre == null && percent == 100) {
					// All occurences of "from" need to be replaced
					return words.replaceAll(from, to);
				}
				String newWords = "";
				if (words.endsWith(from)) {
					// add space to end to recognize last occurence of "from"
					words = words + " ";
				}
				// remove all "from" and split "words" there
				String[] splitted = words.split(from);
				int index = 0;
				String part = null;

				// if there are occurences of "from"
				if (splitted.length > 1) {
					// - 1 because dont add "to" to the end of last part
					while (index < splitted.length - 1) {
						part = splitted[index];
						// add current part of "words" to the output
						newWords = newWords + part;
						// check if the part ends with correct string

						if (doesPreMatch(part, pre, match) && Math.random() * 100.0 <= percent) {
							// add replacement
							newWords = newWords + to;
						} else {
							// add original
							newWords = newWords + from;
						}
						index++;
					}
					// add the last part to finish the sentence
					part = splitted[index];
					if (part.equals(" ")) {
						// dont add the space to the end
						return newWords;
					} else {
						return newWords + part;
					}
				}
			} catch (java.util.regex.PatternSyntaxException e) {
				// e.printStackTrace();
				return words;
			}
		}
		return words;
	}

	public static boolean doesPreMatch(String part, String[] pre, boolean match) {
		boolean isBefore = !match;
		if (pre != null) {
			for (String pr : pre) {
				if (match == true) {
					// if one is correct, it is enough
					if (part.endsWith(pr) == match) {
						isBefore = true;
						break;
					}
				} else {
					// if one is wrong, its over
					if (part.endsWith(pr) != match) {
						isBefore = false;
						break;
					}
				}
			}
		} else {
			isBefore = true;
		}
		return isBefore;
	}

	// load from config file
	public static void load() {
		if (config != null) {
			for (Map<?, ?> map : config.getMapList("words")) {
				new Words(map);
			}
		}
	}

}