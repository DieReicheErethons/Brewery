package com.dre.brewery.lore;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.LegacyUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTSaveStream extends ByteArrayOutputStream {
	private static final String TAG = "brewdata";
	private static final NamespacedKey KEY = new NamespacedKey(BreweryPlugin.getInstance(), TAG);

	private final ItemMeta meta;

	public NBTSaveStream(ItemMeta meta) {
		super(128);
		this.meta = meta;
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		if (size() <= 0) return;
		LegacyUtil.writeBytesItem(toByteArray(), meta, KEY);
	}
}
