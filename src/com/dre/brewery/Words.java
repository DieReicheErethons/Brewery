package com.dre.brewery;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.Character;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.block.SignChangeEvent;

public class Words {

	// represends Words and letters, that are replaced in drunk players messages

	public static ArrayList<Words> words = new ArrayList<Words>();
	public static List<String> commands;
	public static List<String[]> ignoreText = new ArrayList<String[]>();
	public static FileConfiguration config;
	public static Boolean doSigns;
	public static Boolean log;
	private static Map<String, Long> waitPlayers = new HashMap<String, Long>();

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

	private static boolean loadWords() {
		if (words.isEmpty()) {
			// load when first drunk player talks
			load();
		}
		return !words.isEmpty();
	}

	// Distort players words when he uses a command
	public static void playerCommand(PlayerCommandPreprocessEvent event) {
		String name = event.getPlayer().getName();
		BPlayer bPlayer = BPlayer.get(name);
		if (bPlayer != null) {
			if (!commands.isEmpty() && loadWords()) {
				if (!waitPlayers.containsKey(name) || waitPlayers.get(name) + 500 < System.currentTimeMillis()) {
					String chat = event.getMessage();
					for (String command : commands) {
						if (command.length() + 1 < chat.length()) {
							if (Character.isSpaceChar(chat.charAt(command.length()))) {
								if (chat.toLowerCase().startsWith(command.toLowerCase())) {
									if (log) {
										P.p.log(P.p.languageReader.get("Player_TriedToSay", name, chat));
									}
									String message = chat.substring(command.length() + 1);
									message = distortMessage(message, bPlayer.getDrunkeness());

									event.setMessage(chat.substring(0, command.length() + 1) + message);
									waitPlayers.put(name, System.currentTimeMillis());
									return;
								}
							}
						}
					}
				}
			}
		}
	}

	// Distort players words when he uses a command
	public static void signWrite(SignChangeEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer().getName());
		if (bPlayer != null) {
			if (loadWords()) {
				int index = 0;
				for (String message : event.getLines()) {
					if (message.length() > 1) {
						message = distortMessage(message, bPlayer.getDrunkeness());

						if (message.length() > 15) {
							message = message.substring(0, 14);
						}
						event.setLine(index, message);
					}
					index++;
				}
			}
		}
	}

	// Distort players words when he talks
	public static void playerChat(AsyncPlayerChatEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer().getName());
		if (bPlayer != null) {
			if (loadWords()) {
				String message = event.getMessage();
				if (log) {
					P.p.log(P.p.languageReader.get("Player_TriedToSay", event.getPlayer().getName(), message));
				}
				event.setMessage(distortMessage(message, bPlayer.getDrunkeness()));
			}
		}
	}

	// distorts a message, ignoring text enclosed in ignoreText letters
	public static String distortMessage(String message, int drunkeness) {
		if (!ignoreText.isEmpty()) {
			for (String[] bypass : ignoreText) {
				int indexStart = 0;
				if (!bypass[0].equals("")) {
					indexStart = message.indexOf(bypass[0]);
				}
				int indexEnd = message.length() - 1;
				if (!bypass[1].equals("")) {
					indexEnd = message.indexOf(bypass[1], indexStart + 2);
				}
				if (indexStart != -1 && indexEnd != -1) {
					if (indexEnd > indexStart + 1) {
						String ignoredMessage = message.substring(indexStart, indexEnd);
						String msg0 = message.substring(0, indexStart);
						String msg1 = message.substring(indexEnd);

						if (msg0.length() > 1) {
							msg0 = distortMessage(msg0, drunkeness);
						}
						if (msg1.length() > 1) {
							msg1 = distortMessage(msg1, drunkeness);
						}

						return msg0 + ignoredMessage + msg1;
					}
				}
			}
		}
		return distortString(message, drunkeness);
	}

	// distorts a message without checking ignoreText letters
	private static String distortString(String message, int drunkeness) {
		if (message.length() > 1) {
			for (Words word : words) {
				if (word.alcohol <= drunkeness) {
					message = word.distort(message);
				}
			}
		}
		return message;
	}

	// replace "percent"% of "from" -> "to" in "words", when the string before
	// each "from" "match"es "pre"
	// Not yet ignoring case :(
	public String distort(String words) {
		String from = this.from;
		String to = this.to;

		if (from.equalsIgnoreCase("-end")) {
			from = words;
			to = words + to;
		} else if (from.equalsIgnoreCase("-start")) {
			from = words;
			to = to + words;
		} else if (from.equalsIgnoreCase("-all")) {
			from = words;
		} else if (from.equalsIgnoreCase("-space")) {
			from = " ";
		} else if (from.equalsIgnoreCase("-random")) {
			// inserts "to" on a random position in "words"
			int charIndex = (int) (Math.random() * (words.length() - 1));
			if (charIndex < words.length() / 2) {
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
				if (pre == null && percentage == 100) {
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
				String part;

				// if there are occurences of "from"
				if (splitted.length > 1) {
					// - 1 because dont add "to" to the end of last part
					for (int i = 0; i < splitted.length - 1; i++) {
						part = splitted[i];
						// add current part of "words" to the output
						newWords = newWords + part;
						// check if the part ends with correct string

						if (doesPreMatch(part) && Math.random() * 100.0 <= percentage) {
							// add replacement
							newWords = newWords + to;
						} else {
							// add original
							newWords = newWords + from;
						}
					}

					// add the last part to finish the sentence
					part = splitted[splitted.length - 1];
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

	public boolean doesPreMatch(String part) {
		boolean isBefore = !match;
		if (pre != null) {
			for (String pr : pre) {
				if (match) {
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