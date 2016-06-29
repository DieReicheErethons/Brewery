package com.dre.brewery.lore;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XORUnscrambleStream extends FilterInputStream {

	private final long seed;
	private SeedInputStream xorStream;
	private boolean running;
	private boolean markRunning;
	private boolean markxor;

	public XORUnscrambleStream(InputStream in, long seed) {
		super(in);
		this.seed = seed;
	}

	public void start() throws IOException {
		running = true;
		if (xorStream == null) {
			short id = (short) (in.read() << 8 | in.read());
			if (id == 0) {
				running = false;
				return;
			}
			xorStream = new SeedInputStream(seed ^ id);
		}
	}

	public void stop() {
		running = false;
	}

	@Override
	public int read() throws IOException {
		if (!running) {
			return in.read();
		}
		return (in.read() ^ xorStream.read()) & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (!running) {
			return in.read(b, off, len);
		}
		len = in.read(b, off, len);
		for (int i = off; i < len + off; i++) {
			b[i] ^= xorStream.read();
		}
		return len;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = in.skip(n);
		if (running && skipped > 0) {
			xorStream.skip(skipped);
		}
		return skipped;
	}

	@Override
	public void close() throws IOException {
		if (xorStream != null) {
			xorStream.close();
			xorStream = null;
		}
		running = false;
		super.close();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		if (markxor) {
			xorStream.reset();
		} else {
			xorStream = null;
		}
		running = markRunning;
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
		if (xorStream != null) {
			xorStream.mark(readlimit);
			markxor = true;
		}
		markRunning = running;
	}
}
