package com.dre.brewery.utility;

import com.dre.brewery.*;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.BRecipe;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class Stats {

	public int brewsCreated;
	public int brewsCreatedCmd; // Created by command
	public int exc, good, norm, bad, terr; // Brews drunken with quality

	public void metricsForCreate(boolean byCmd) {
		if (brewsCreated == Integer.MAX_VALUE) return;
		brewsCreated++;
		if (byCmd) {
			if (brewsCreatedCmd == Integer.MAX_VALUE) return;
			brewsCreatedCmd++;
		}
	}

	public void forDrink(Brew brew) {
		if (brew.getQuality() >= 9) {
			exc++;
		} else if (brew.getQuality() >= 7) {
			good++;
		} else if (brew.getQuality() >= 5) {
			norm++;
		} else if (brew.getQuality() >= 3) {
			bad++;
		} else {
			terr++;
		}
	}

	public void setupBStats() {
		try {
			Metrics metrics = new Metrics(P.p, 3494);
			metrics.addCustomChart(new SingleLineChart("drunk_players", BPlayer::numDrunkPlayers));
			metrics.addCustomChart(new SingleLineChart("brews_in_existence", () -> brewsCreated));
			metrics.addCustomChart(new SingleLineChart("barrels_built", Barrel.barrels::size));
			metrics.addCustomChart(new SingleLineChart("cauldrons_boiling", BCauldron.bcauldrons::size));
			metrics.addCustomChart(new AdvancedPie("brew_quality", () -> {
				Map<String, Integer> map = new HashMap<>(8);
				map.put("excellent", exc);
				map.put("good", good);
				map.put("normal", norm);
				map.put("bad", bad);
				map.put("terrible", terr);
				return map;
			}));
			metrics.addCustomChart(new AdvancedPie("brews_created", () -> {
				Map<String, Integer> map = new HashMap<>(4);
				map.put("by command", brewsCreatedCmd);
				map.put("brewing", brewsCreated - brewsCreatedCmd);
				return map;
			}));

			metrics.addCustomChart(new SimplePie("number_of_recipes", () -> {
				int recipes = BRecipe.getAllRecipes().size();
				if (recipes < 7) {
					return "Less than 7";
				} else if (recipes < 11) {
					return "7-10";
				} else if (recipes == 11) {
					// There were 11 default recipes, so show this as its own slice
					return "11";
				} else if (recipes == 20) {
					// There are 20 default recipes, so show this as its own slice
					return "20";
				} else if (recipes <= 29) {
					if (recipes % 2 == 0) {
						return recipes + "-" + (recipes + 1);
					} else {
						return (recipes - 1) + "-" + recipes;
					}
				} else if (recipes < 35) {
					return "30-34";
				} else if (recipes < 40) {
					return "35-39";
				} else if (recipes < 45) {
					return "40-44";
				} else if (recipes <= 50) {
					return "45-50";
				} else {
					return "More than 50";
				}

			}));
			metrics.addCustomChart(new SimplePie("cauldron_particles", () -> {
				if (!BConfig.enableCauldronParticles) {
					return "disabled";
				}
				if (BConfig.minimalParticles) {
					return "minimal";
				}
				return "enabled";
			}));
			metrics.addCustomChart(new SimplePie("wakeups", () -> {
				if (!BConfig.enableWake) {
					return "disabled";
				}
				int wakeups = Wakeup.wakeups.size();
				if (wakeups == 0) {
					return "0";
				} else if (wakeups <= 5) {
					return "1-5";
				} else if (wakeups <= 10) {
					return "6-10";
				} else if (wakeups <= 20) {
					return "11-20";
				} else {
					return "More than 20";
				}
			}));
			metrics.addCustomChart(new SimplePie("v2_mc_version", () -> {
				String mcv = Bukkit.getBukkitVersion();
				mcv = mcv.substring(0, mcv.indexOf('.', 2));
				int index = mcv.indexOf('-');
				if (index > -1) {
					mcv = mcv.substring(0, index);
				}
				if (mcv.matches("^\\d\\.\\d{1,2}$")) {
					// Start, digit, dot, 1-2 digits, end
					return mcv;
				} else {
					return "undef";
				}
			}));
			metrics.addCustomChart(new DrilldownPie("plugin_mc_version", () -> {
				Map<String, Map<String, Integer>> map = new HashMap<>(3);
				String mcv = Bukkit.getBukkitVersion();
				mcv = mcv.substring(0, mcv.indexOf('.', 2));
				int index = mcv.indexOf('-');
				if (index > -1) {
					mcv = mcv.substring(0, index);
				}
				if (mcv.matches("^\\d\\.\\d{1,2}$")) {
					// Start, digit, dot, 1-2 digits, end
					mcv = "MC " + mcv;
				} else {
					mcv = "undef";
				}
				Map<String, Integer> innerMap = new HashMap<>(3);
				innerMap.put(mcv, 1);
				map.put(P.p.getDescription().getVersion(), innerMap);
				return map;
			}));
			metrics.addCustomChart(new SimplePie("language", () -> P.p.language));
			metrics.addCustomChart(new SimplePie("config_scramble", () -> BConfig.enableEncode ? "enabled" : "disabled"));
			metrics.addCustomChart(new SimplePie("config_lore_color", () -> {
				if (BConfig.colorInBarrels) {
					if (BConfig.colorInBrewer) {
						return "both";
					} else {
						return "in barrels";
					}
				} else {
					if (BConfig.colorInBrewer) {
						return "in distiller";
					} else {
						return "none";
					}
				}
			}));
			metrics.addCustomChart(new SimplePie("config_always_show", () -> {
				if (BConfig.alwaysShowQuality) {
					if (BConfig.alwaysShowAlc) {
						return "both";
					} else {
						return "quality stars";
					}
				} else {
					if (BConfig.alwaysShowAlc) {
						return "alc content";
					} else {
						return "none";
					}
				}
			}));
		} catch (Exception | LinkageError e) {
			e.printStackTrace();
		}
	}

}
