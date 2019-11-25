/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */
package com.dre.brewery.utility;

public class Tuple<A, B> {

	/**
	 * The first value in the tuple
	 */
	private final A a;

	/**
	 * The second value in the tuple
	 */
	private final B b;

	public Tuple(A a, B b) {
		this.a = a;
		this.b = b;
	}

	/**
	 * Gets the first value in the tuple
	 */
	public A first() {
		return a;
	}

	/**
	 * Gets the second value in the tuple
	 */
	public B second() {
		return b;
	}

	/**
	 * Gets the first value in the tuple, Synonym for first()
	 */
	public A a() {
		return a;
	}

	/**
	 * Gets the second value in the tuple, Synonym for second()
	 */
	public B b() {
		return b;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Tuple)) {
			return false;
		}

		Tuple<?, ?> tuple = (Tuple<?, ?>) object;
		return tuple.a == a && tuple.b == b;
	}

	@Override
	public int hashCode() {
		return a.hashCode() ^ b.hashCode();
	}

}
