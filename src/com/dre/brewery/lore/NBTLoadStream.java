package com.dre.brewery.lore;

import com.dre.brewery.P;
import com.dre.brewery.utility.LegacyUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;

public class NBTLoadStream extends ByteArrayInputStream {
	private static final String TAG = "brewdata";

	public NBTLoadStream(ItemMeta meta) {
		super(getNBTBytes(meta));
	}

	private static byte[] getNBTBytes(ItemMeta meta) {
		byte[] bytes = LegacyUtil.readBytesItem(meta, new NamespacedKey(P.p, TAG));
		if (bytes == null) {
			return new byte[0];
		}
		return bytes;
	}

	public boolean hasData() {
		return count > 0;
	}
}
