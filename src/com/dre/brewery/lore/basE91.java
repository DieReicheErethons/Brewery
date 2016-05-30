package com.dre.brewery.lore;

/*
 * basE91 encoding/decoding routines
 *
 * Copyright (c) 2000-2006 Joachim Henke
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of Joachim Henke nor the names of his contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

public class basE91
{
	private int ebq, en, dbq, dn, dv;
	private int[] marker = null;
	public final byte[] enctab;
	private final byte[] dectab;

	public int encode(byte[] ib, int n, byte[] ob)
	{
		int i, c = 0;

		for (i = 0; i < n; ++i) {
			ebq |= (ib[i] & 255) << en;
			en += 8;
			if (en > 13) {
				int ev = ebq & 8191;

				if (ev > 88) {
					ebq >>= 13;
					en -= 13;
				} else {
					ev = ebq & 16383;
					ebq >>= 14;
					en -= 14;
				}
				ob[c++] = enctab[ev % 91];
				ob[c++] = enctab[ev / 91];
			}
		}
		return c;
	}

	public int encEnd(byte[] ob)
	{
		int c = 0;

		if (en > 0) {
			ob[c++] = enctab[ebq % 91];
			if (en > 7 || ebq > 90)
				ob[c++] = enctab[ebq / 91];
		}
		encReset();
		return c;
	}

	public void encReset()
	{
		ebq = 0;
		en = 0;
	}

	public int decode(byte[] ib, int n, byte[] ob)
	{
		int i, c = 0;

		for (i = 0; i < n; ++i) {
			if (dectab[ib[i]] == -1)
				continue;
			if (dv == -1)
				dv = dectab[ib[i]];
			else {
				dv += dectab[ib[i]] * 91;
				dbq |= dv << dn;
				dn += (dv & 8191) > 88 ? 13 : 14;
				do {
					ob[c++] = (byte) dbq;
					dbq >>= 8;
					dn -= 8;
				} while (dn > 7);
				dv = -1;
			}
		}
		return c;
	}

	public int decEnd(byte[] ob)
	{
		int c = 0;

		if (dv != -1)
			ob[c++] = (byte) (dbq | dv << dn);
		decReset();
		return c;
	}

	public void decReset()
	{
		dbq = 0;
		dn = 0;
		dv = -1;
	}

	public void decMark() {
		marker = new int[] {dbq, dn, dv};
	}

	public void decUnmark() {
		if (marker == null) return;
		dbq = marker[0];
		dn = marker[1];
		dv = marker[2];
	}

	public basE91()
	{
		int i;
		String ts = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&()*+,-./:;<=>?@[]^_`{|}~\""; // Added '-' removed '#'

		enctab = ts.getBytes();
		dectab = new byte[256];
		for (i = 0; i < 256; ++i)
			dectab[i] = -1;
		for (i = 0; i < 91; ++i)
			dectab[enctab[i]] = (byte) i;
		encReset();
		decReset();
	}
}
