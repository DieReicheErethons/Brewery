package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LoreInputStream extends InputStream {

	private static final basE91 DECODER = new basE91();

	private byte[] decbuf = new byte[18];
	private byte[] buf = new byte[16];
	private int reader = 0;
	private int count = 0;
	private ByteArrayInputStream readStream;

	public LoreInputStream(ItemMeta meta) throws IOException {
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
					readStream = new ByteArrayInputStream(build.toString().getBytes());
					break;
				}
			}
		}
		if (readStream == null) throw new IOException("Meta has no data in lore");
	}

	private void decode() throws IOException {
		reader = 0;
		count = readStream.read(decbuf);
		if (count < 1) {
			count = DECODER.decEnd(buf);
			if (count < 1) {
				count = -1;
			}
			return;
		}
		count = DECODER.decode(decbuf, count, buf);
	}

	@Override
	public int read() throws IOException {
		if (count == -1) return -1;
		if (count == 0 || reader == count) {
			decode();
			return read();
		}
		return buf[reader++] & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (b == null) throw new NullPointerException();
		if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
		if (len == 0) return 0;

		if (count == -1) return -1;
		if (count == 0 || reader == count) {
			decode();
			if (count == -1) return -1;
		}

		if (count > 0 && count - reader >= len) {
			// enough data in buffer, copy it out directly
			System.arraycopy(buf, reader, b, off, len);
			reader += len;
			return len;
		}

		int out = 0;
		int writeSize;
		while (count > 0) {
			writeSize = Math.min(len, count - reader);
			System.arraycopy(buf, reader, b, off + out, writeSize);
			out += writeSize;
			len -= writeSize;
			if (len > 0) {
				decode();
			} else {
				reader += writeSize;
				break;
			}
		}
		return out;
	}

	@Override
	public long skip(long n) throws IOException {
		return super.skip(n);
	}

	@Override
	public int available() throws IOException {
		return Math.round(readStream.available() * 0.813F); // Ratio encoded to decoded with random data
	}

	@Override
	public void close() throws IOException {
		count = -1;
		DECODER.decReset();
		buf = null;
		decbuf = null;
		readStream = null;
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (!markSupported()) return;
		readStream.mark(readlimit);
		DECODER.decMark();
	}

	@Override
	public synchronized void reset() throws IOException {
		if (!markSupported()) super.reset();
		readStream.reset();
		DECODER.decUnmark();
	}

	@Override
	public boolean markSupported() {
		return readStream.markSupported();
	}
}
