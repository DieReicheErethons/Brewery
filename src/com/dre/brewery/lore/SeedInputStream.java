package com.dre.brewery.lore;

import java.io.InputStream;
import java.util.Arrays;

public class SeedInputStream extends InputStream {
	// From java.util.Random
	private static final long multiplier = 0x5DEECE66DL;
	private static final long addend = 0xBL;
	private static final long mask = (1L << 48) - 1;

	private long seed;
	private byte[] buf = new byte[4];
	private byte reader = 4;
	private long markSeed;
	private byte[] markbuf;

	public SeedInputStream(long seed) {
		this.seed = (seed ^ multiplier) & mask;
	}

	private void calcSeed() {
		seed = (seed * multiplier + addend) & mask;
	}

	private void genNext() {
		calcSeed();
		int next = (int)(seed >>> 16);
		buf[0] = (byte) (next >> 24);
		buf[1] = (byte) (next >> 16);
		buf[2] = (byte) (next >> 8);
		buf[3] = (byte) next;
		reader = 0;
	}

	@Override
	public int read(byte[] b, int off, int len) {
		for (int i = off; i < len; i++) {
			if (reader >= 4) {
				genNext();
			}
			b[i] = buf[reader++];
		}
		return len;
	}

	@Override
	public int read() {
		if (reader == 4) {
			genNext();
		}
		return buf[reader++];
	}

	@Override
	public long skip(long toSkip) {
		long n = toSkip;
		while (n > 0) {
			if (reader < 4) {
				reader++;
				n--;
			} else if (n >= 4) {
				calcSeed();
				n -= 4;
			} else {
				genNext();
			}
		}
		return toSkip;
	}

	@Override
	public void close() {
		buf = null;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		markbuf = new byte[] {buf[0], buf[1], buf[2], buf[3], reader};
		markSeed = seed;
	}

	@Override
	public synchronized void reset() {
		seed = markSeed;
		buf = Arrays.copyOfRange(markbuf, 0, 4);
		reader = markbuf[4];
	}
}
