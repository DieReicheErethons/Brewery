package com.dre.brewery.lore;

import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Base91EncoderStream extends FilterOutputStream {

	private static final basE91 ENCODER = new basE91();

	private byte[] buf = new byte[16];
	private byte[] encBuf = new byte[24];
	private int writer = 0;
	private int encoded = 0;

	public Base91EncoderStream(OutputStream out) {
		super(out);
	}

	private void encFlush() throws IOException {
		encoded = ENCODER.encode(buf, writer, encBuf);
		out.write(encBuf, 0, encoded);
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
			out.write(encBuf, 0, encoded);
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
		if (writer > 0) {
			encFlush();
		}

		encoded = ENCODER.encEnd(encBuf);
		if (encoded > 0) {
			out.write(encBuf, 0, encoded);
		}
		super.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
		ENCODER.encReset();
		buf = null;
		encBuf = null;
	}
}
