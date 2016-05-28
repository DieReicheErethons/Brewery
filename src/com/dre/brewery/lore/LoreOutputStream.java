package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoreOutputStream extends OutputStream {

	private static final basE91 ENCODER = new basE91();

	private byte[] buf = new byte[16];
	private byte[] encBuf = new byte[24];
	private int writer = 0;
	private int encoded = 0;

	private ItemMeta meta;
	private final int line;
	private ByteArrayOutputStream stream = new ByteArrayOutputStream(128);

	public LoreOutputStream(ItemMeta meta, int line) {
		this.meta = meta;
		this.line = line;
	}

	private void encFlush() {
		encoded = ENCODER.encode(buf, writer, encBuf);
		stream.write(encBuf, 0, encoded);
		writer = 0;
	}

	@Override
	public void write(int b) throws IOException {
		buf[writer++] = (byte) b;
		if (writer >= buf.length) {
			encFlush();
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len == 0) return;
		if (b == null) throw new NullPointerException();
		if (len < 0 || off < 0 || (off + len) > b.length || off > b.length || (off + len) < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (buf.length - writer >= len) {
			// Enough space in the buffer, copy it in
			System.arraycopy(b, off, buf, writer, len);
			writer += len;
			if (writer >= buf.length) {
				encFlush();
			}
			return;
		}

		if (off == 0 && buf.length >= len) {
			// Buffer is too full, so flush and encode data directly
			encFlush();
			encoded = ENCODER.encode(b, len, encBuf);
			stream.write(encBuf, 0, encoded);
			return;
		}

		// More data than space in the Buffer
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		while (true) {
			writer += in.read(buf, writer, buf.length - writer);
			if (writer >= buf.length) {
				encFlush();
			} else {
				break;
			}
		}
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		if (writer > 0) {
			encFlush();
		}

		encoded = ENCODER.encEnd(encBuf);
		if (encoded > 0) {
			stream.write(encBuf, 0, encoded);
		}
		if (stream.size() <= 0) return;

		stream.flush();
		String s = stream.toString();

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
		stream.close();
		ENCODER.encReset();
		buf = null;
		encBuf = null;
		meta = null;
		stream = null;
	}
}
