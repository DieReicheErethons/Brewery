package com.dre.brewery;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BEffect {

	private PotionEffectType type;
	private short minlvl;
	private short maxlvl;
	private short minduration;
	private short maxduration;
	private boolean hidden = false;


	public BEffect(String effectString) {
		String[] effectSplit = effectString.split("/");
		String effect = effectSplit[0];
		if (effect.equalsIgnoreCase("WEAKNESS") ||
				effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
				effect.equalsIgnoreCase("SLOW") ||
				effect.equalsIgnoreCase("SPEED") ||
				effect.equalsIgnoreCase("REGENERATION")) {
			// hide these effects as they put crap into lore
			// Dont write Regeneration into Lore, its already there storing data!
			hidden = true;
		} else if (effect.endsWith("X")) {
			hidden = true;
			effect = effect.substring(0, effect.length() - 1);
		}
		type = PotionEffectType.getByName(effect);
		if (type == null) {
			P.p.errorLog("Effect: " + effect + " does not exist!");
			return;
		}

		if (effectSplit.length == 3) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setLvl(range);
				range = effectSplit[2].split("-");
				setDuration(range);
			}
		} else if (effectSplit.length == 2) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setDuration(range);
				maxlvl = 3;
				minlvl = 1;
			}
		} else {
			maxduration = 20;
			minduration = 10;
			maxlvl = 3;
			minlvl = 1;
		}
	}

	private void setLvl(String[] range) {
		if (range.length == 1) {
			maxlvl = (short) P.p.parseInt(range[0]);
			minlvl = 1;
		} else {
			maxlvl = (short) P.p.parseInt(range[1]);
			minlvl = (short) P.p.parseInt(range[0]);
		}
	}

	private void setDuration(String[] range) {
		if (range.length == 1) {
			maxduration = (short) P.p.parseInt(range[0]);
			minduration = (short) (maxduration / 8);
		} else {
			maxduration = (short) P.p.parseInt(range[1]);
			minduration = (short) P.p.parseInt(range[0]);
		}
	}

	public PotionEffect generateEffect(int quality) {
		int duration = calcDuration(quality);
		int lvl = calcLvl(quality);

		if (lvl < 1 || (duration < 1 && !type.isInstant())) {
			return null;
		}

		duration *= 20;
		duration /= type.getDurationModifier();
		return type.createEffect(duration, lvl - 1);
	}

	public void apply(int quality, Player player) {
		PotionEffect effect = generateEffect(quality);
		if (effect != null) {
			effect.apply(player);
		}
	}

	public int calcDuration(float quality) {
		return (int) Math.round(minduration + ((maxduration - minduration) * (quality / 10.0)));
	}

	public int calcLvl(float quality) {
		return (int) Math.round(minlvl + ((maxlvl - minlvl) * (quality / 10.0)));
	}

	public void writeInto(PotionMeta meta, int quality) {
		if ((calcDuration(quality) > 0 || type.isInstant()) && calcLvl(quality) > 0) {
			meta.addCustomEffect(type.createEffect(0, 0), true);
		} else {
			meta.removeCustomEffect(type);
		}
	}

	public boolean isValid() {
		return type != null && minlvl >= 0 && maxlvl >= 0 && minduration >= 0 && maxduration >= 0;
	}

	public boolean isHidden() {
		return hidden;
	}
}
