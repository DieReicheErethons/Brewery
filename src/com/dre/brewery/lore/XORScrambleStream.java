package com.dre.brewery.lore;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class XORScrambleStream extends FilterOutputStream {

	private final long seed;
	private SeedInputStream xorStream;
	private boolean running;

	public XORScrambleStream(OutputStream out, long seed) {
		super(out);
		this.seed = seed;
	}

	public void start() throws IOException {
		running = true;
		if (xorStream == null) {
			short id = 0;
			while (id == 0) {
				id = (short) new Random().nextInt();
			}
			xorStream = new SeedInputStream(seed ^ id);
			out.write((byte) (id >> 8));
			out.write((byte) id);
			write(209); // parity/sanity
		}
	}

	public void stop() {
		running = false;
	}

	@Override
	public void write(int b) throws IOException {
		if (!running) {
			out.write(b);
			return;
		}
		out.write(b ^ xorStream.read());
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (!running) {
			out.write(b, off, len);
			return;
		}
		byte[] xored = new byte[len];
		xorStream.read(xored);
		int j = off;
		for (int i = 0; i < len; i++) {
			xored[i] ^= b[j++];
		}
		out.write(xored);
	}

	@Override
	public void close() throws IOException {
		running = false;
		xorStream = null;
		super.close();
	}
}
