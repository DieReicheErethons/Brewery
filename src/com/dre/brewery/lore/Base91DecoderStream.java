package com.dre.brewery.lore;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Base91DecoderStream extends FilterInputStream {

	private final basE91 decoder = new basE91();
	private byte[] decbuf = new byte[32];
	private byte[] buf = new byte[32];
	private int reader = 0;
	private int count = 0;
	private byte[] markBuf = null;

	public Base91DecoderStream(InputStream in) {
		super(in);
	}

	private void decode() throws IOException {
		reader = 0;
		count = in.read(decbuf);
		if (count < 1) {
			count = decoder.decEnd(buf);
			if (count < 1) {
				count = -1;
			}
			return;
		}
		count = decoder.decode(decbuf, count, buf);
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
			// Not enough data in buffer, write all out, decode and repeat
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
		if (count == -1) return 0;
		if (count > 0 && count - reader >= n) {
			reader += n;
			return n;
		}
		long skipped = count - reader;
		decode();

		while (count > 0) {
			if (count > n - skipped) {
				reader = (int) (n - skipped);
				return n;
			}
			skipped += count;
			decode();
		}
		return skipped;
	}

	@Override
	public int available() throws IOException {
		if (count == -1) return 0;
		return (int) (in.available() * 0.813F) + count - reader; // Ratio encoded to decoded with random data
	}

	@Override
	public void close() throws IOException {
		in.close();
		count = -1;
		decoder.decReset();
		buf = null;
		decbuf = null;
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (!markSupported()) return;
		if (count == -1) return;
		in.mark(readlimit);
		decoder.decMark();
		if (count > 0 && reader < count) {
			markBuf = new byte[count - reader];
			System.arraycopy(buf, reader, markBuf, 0, markBuf.length);
		} else {
			markBuf = null;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		if (!markSupported()) throw new IOException("mark and reset not supported by underlying Stream");
		in.reset();
		decoder.decUnmark();
		reader = 0;
		count = 0;
		if (markBuf != null) {
			System.arraycopy(markBuf, 0, buf, 0, markBuf.length);
			count = markBuf.length;
		}
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
}
