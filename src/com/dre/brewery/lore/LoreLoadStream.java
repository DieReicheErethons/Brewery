package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class LoreLoadStream extends ByteArrayInputStream {

	public LoreLoadStream(ItemMeta meta) throws IllegalArgumentException {
		this(meta, -1);
	}

	public LoreLoadStream(ItemMeta meta, int line) throws IllegalArgumentException {
		super(loreToBytes(meta, line));
	}

	private static byte[] loreToBytes(ItemMeta meta, int lineNum) throws IllegalArgumentException {
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			if (lineNum >= 0) {
				String line = lore.get(lineNum);
				if (line.startsWith("§%")) {
					return loreLineToBytes(line);
				}
			}
			for (String line : lore) {
				if (line.startsWith("§%")) {
					return loreLineToBytes(line);
				}
			}
		}
		throw new IllegalArgumentException("Meta has no data in lore");
	}

	private static byte[] loreLineToBytes(String line) {
		StringBuilder build = new StringBuilder((int) (line.length() / 2F));
		byte skip = 2;
		for (char c : line.toCharArray()) {
			if (skip > 0) {
				skip--;
				continue;
			}
			if (c == '§') continue;
			build.append(c);
		}
		return build.toString().getBytes();
	}
}
