package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoreSaveStream extends ByteArrayOutputStream {

	public static final String IDENTIFIER = "§%";

	private ItemMeta meta;
	private int line;
	private boolean flushed = false;

	public LoreSaveStream(ItemMeta meta) {
		this(meta, -1);
	}

	public LoreSaveStream(ItemMeta meta, int line) {
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
		if (flushed || meta == null) {
			// Dont write twice
			return;
		}
		flushed = true;
		String s = toString();

		StringBuilder loreLineBuilder = new StringBuilder((s.length() * 2) + 6);
		loreLineBuilder.append(IDENTIFIER);
		for (char c : s.toCharArray()) {
			loreLineBuilder.append('§').append(c);
		}
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<>();
		}
		int prev = 0;
		for (Iterator<String> iterator = lore.iterator(); iterator.hasNext(); ) {
			if (iterator.next().startsWith(IDENTIFIER)) {
				iterator.remove();
				break;
			}
			prev++;
		}
		if (line < 0) {
			if (prev >= 0) {
				line = prev;
			} else {
				line = lore.size();
			}
		}
		while (lore.size() < line) {
			lore.add("");
		}
		lore.add(line, loreLineBuilder.toString());
		meta.setLore(lore);
	}

	@Override
	public void close() throws IOException {
		super.close();
		meta = null;
	}
}
