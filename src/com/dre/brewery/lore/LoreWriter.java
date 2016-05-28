package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.io.OutputStream;

public class LoreWriter extends OutputStream {

	ItemMeta meta;

	public LoreWriter(ItemMeta meta) {
		this.meta = meta;
	}

	@Override
	public void write(int b) throws IOException {

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

	}
}
