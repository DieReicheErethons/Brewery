package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class LoreReader extends ByteArrayInputStream {

	public LoreReader(ItemMeta meta) throws IOException {
		super(loreToBytes(meta));
	}

	private static byte[] loreToBytes(ItemMeta meta) throws IOException {
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			for (String line : lore) {
				if (line.startsWith("§%")) {
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
		}
		throw new IOException("Meta has no data in lore");
	}
}
