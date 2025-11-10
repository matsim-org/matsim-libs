/**
 * se.vti.utils
 * 
 * Copyright (C) 2015-2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.utils.misc;

import java.io.Serializable;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <A>
 * @param <B>
 */
public class Tuple<A, B> implements Serializable {

	// -------------------- MEMBERS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final A a;

	private final B b;

	private final int hashCode;

	// -------------------- CONSTRUCTION --------------------

	public Tuple(final A a, final B b) {
		if (a == null) {
			throw new IllegalArgumentException("a is null");
		}
		if (b == null) {
			throw new IllegalArgumentException("b is null");
		}
		this.a = a;
		this.b = b;

		int hashCode = 1;
		hashCode = 31 * hashCode + a.hashCode();
		hashCode = 31 * hashCode + b.hashCode();
		this.hashCode = hashCode;
	}

	// -------------------- CONTENT ACCESS --------------------

	public A getA() {
		return this.a;
	}

	public B getB() {
		return this.b;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public boolean equals(final Object o) {
		try {
			if (o == null) {
				return false;
			}
			final Tuple<?, ?> other = (Tuple<?, ?>) o;
			return (this.a.equals(other.a) && this.b.equals(other.b));
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + a + ", " + b + ")";
	}
}
