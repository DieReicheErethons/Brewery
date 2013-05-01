package com.dre.brewery;

import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.dre.brewery.BPlayer;

public class Words {


	private static ArrayList<Words> words = new ArrayList<Words>();//material and amount
	public static ConfigurationSection config;

	private String from;
	private String to;
	private String[] pre;
	private Boolean match;
	private int alcohol;
	private int percentage;

	public Words(ConfigurationSection section){
		if(section.getString("to",null) != null){
			this.from = section.getName();
			this.to = section.getString("to",null);
			String pre = section.getString("pre",null);
			if(pre != null){
				this.pre = pre.split(",");
			}
			this.match = section.getBoolean("match",false);
			this.alcohol = section.getInt("alcohol",10);
			this.percentage = section.getInt("percentage",100);
			words.add(this);
		}
	}

	//Distort players words when he talks
	public static void playerChat(AsyncPlayerChatEvent event){
		BPlayer bPlayer = BPlayer.get(event.getPlayer());
		if(bPlayer != null){
			if(words.isEmpty()){
				load();
			}
			if(!words.isEmpty()){
				String message = event.getMessage();
				for(Words w:words){
					if(w.alcohol <= bPlayer.getDrunkeness()){
						message = distort(message, w.from, w.to, w.pre, w.match, w.percentage);
					}
				}
				event.setMessage(message);
			}
		}
	}

	//replace "percent"% of "from" -> "to" in "words", when the string before each "from" "match"es "pre"
	//Not yet ignoring case :(
	public static String distort(String words, String from, String to, String[] pre, boolean match, int percent){
		if(words.contains(from)){
			if(pre == null && percent == 100){
				//All occurences of "from" need to be replaced
				return words.replaceAll(from,to);
			}
			String newWords = "";
			if(words.endsWith(from)){
				//add space to end to recognize last occurence of "from"
				words = words+" ";
			}
			//remove all "from" and split "words" there
			String[] splitted = words.split(from);
			int index = 0;
			String part = null;
			boolean isBefore = !match;

			//if there are occurences of "from"
			if(splitted.length > 1){
				//- 1 because dont add "to" to the end of last part
				while(index < splitted.length - 1){
					part = splitted[index];
					//add current part of "words" to the output
					newWords = newWords+part;
					//check if the part ends with correct string
					if(pre != null){
						for(String pr:pre){
							if(match == true){
								//if one is correct, it is enough
								if(part.endsWith(pr) == match){
									isBefore = true;
									break;
								}
							} else {
								//if one is wrong, its over
								if(part.endsWith(pr) != match){
									isBefore = false;
									break;
								}
							}
						}
					} else {
						isBefore = true;
					}
					if(isBefore && Math.random() * 100.0 <= percent){
						//add replacement
						newWords = newWords+to;
					} else {
						//add original
						newWords = newWords+from;
					}
					index++;
				}
				//add the last part to finish the sentence
				part = splitted[index];
				if(part.equals(" ")){
					//dont add the space to the end
					return newWords;
				} else {
					return newWords + part;
				}
			}
		}
		return words;
	}

	//loaded when first drunken player speaks
	public static void load(){
		if(config != null){
			for(String word:config.getKeys(false)){
				new Words(config.getConfigurationSection(word));
			}
		}
	}


}