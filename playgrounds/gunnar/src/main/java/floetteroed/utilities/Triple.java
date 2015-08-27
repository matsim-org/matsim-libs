/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Triple<A, B, C> {

	// -------------------- MEMBERS --------------------

	private final Tuple<A, B> a_b;

	private final C c;

	private final int hashCode;

	// -------------------- CONSTRUCTION --------------------

	public Triple(final A a, final B b, final C c) {
		if (c == null) {
			throw new IllegalArgumentException("c is null");
		}
		this.a_b = new Tuple<A, B>(a, b);
		this.c = c;

		int hashCode = 1;
		hashCode = 31 * hashCode + this.a_b.hashCode();
		hashCode = 31 * hashCode + this.c.hashCode();
		this.hashCode = hashCode;
	}

	// -------------------- CONTENT ACCESS --------------------

	public A getA() {
		return a_b.getA();
	}

	public B getB() {
		return a_b.getB();
	}

	public C getC() {
		return c;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public boolean equals(final Object o) {
		try {
			if (o == null) {
				return false;
			}
			final Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
			return (this.a_b.equals(other.a_b) && this.c.equals(other.c));
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
		return this.getClass().getSimpleName() + "(" + this.a_b.getA() + ", "
				+ this.a_b.getB() + ", " + this.c + ")";
	}
}
