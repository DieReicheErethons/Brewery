package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoreWriter extends ByteArrayOutputStream {

	private ItemMeta meta;
	private int line;
	private boolean flushed = false;

	public LoreWriter(ItemMeta meta, int line) {
		super(128);
		this.meta = meta;
		this.line = line;
	}

	// Writes to the Lore
	// Without calling this, the ItemMeta remains unchanged
	@Override
	public void flush() throws IOException {
		super.flush();
		if (size() <= 0) return;
		if (flushed) {
			// Dont write twice
			return;
		}
		flushed = true;
		String s = toString();

		StringBuilder loreLineBuilder = new StringBuilder((s.length() * 2) + 6);
		loreLineBuilder.append("§%");
		for (char c : s.toCharArray()) {
			loreLineBuilder.append('§').append(c);
		}
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<>();
		}
		while (lore.size() < line) {
			lore.add("");
		}
		//TODO when existing data string in lore
		lore.add(line, loreLineBuilder.toString());
		meta.setLore(lore);
	}

	@Override
	public void close() throws IOException {
		super.close();
		meta = null;
	}
}
