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
package floetteroed.utilities.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MathHelpers {

	// -------------------- PRIVATE CONSTRUCTOR --------------------

	private MathHelpers() {
	}

	// -------------------- STATIC IMPLEMENTATION --------------------

	public static Double parseDouble(final String s) {
		if (s == null) {
			return null;
		} else {
			return Double.parseDouble(s);
		}
	}

	public static Integer parseInteger(final String s) {
		if (s == null) {
			return null;
		} else {
			return Integer.parseInt(s);
		}
	}

	public static Long parseLong(final String s) {
		if (s == null) {
			return null;
		} else {
			return Long.parseLong(s);
		}
	}

	public static Boolean parseBoolean(final String s) {
		if (s == null) {
			return null;
		} else {
			return Boolean.parseBoolean(s);
		}
	}

	public static double length(final double x1, final double y1,
			final double x2, final double y2) {
		final double dx = x2 - x1;
		final double dy = y2 - y1;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static double overlap(final double start1, final double end1,
			final double start2, final double end2) {
		return Math.max(0, (Math.min(end1, end2) - Math.max(start1, start2)));
	}

	public static double round(final double x, final int digits) {
		final double fact = Math.pow(10.0, digits);
		return Math.round(x * fact) / fact;
	}

	public static int round(final double x) {
		return (int) round(x, 0);
	}

	public static int draw(final Vector probs, final Random rnd) {
		final double x = rnd.nextDouble();
		int result = -1;
		double pSum = 0;
		do {
			result++;
			pSum += probs.get(result);
		} while (pSum < x && result < probs.size() - 1);
		return result;
	}

	// TODO NEW
	public static <T> T draw(final Collection<T> collection, final Random rnd) {
		final int index = rnd.nextInt(collection.size());
		Iterator<T> it = collection.iterator();
		for (int i = 0; i < index; i++) {
			it.next();
		}
		return it.next();
	}

	// TODO NEW
	public static <T> T drawAndRemove(final Collection<T> collection,
			final Random rnd) {
		final T result = draw(collection, rnd);
		collection.remove(result);
		return result;
	}

	public static double[] override(final double[] dest, final double[] source,
			final boolean overrideWithZeros) {
		if (source == null) {
			if (overrideWithZeros) {
				return null;
			} else {
				return dest;
			}
		} else { // source != null
			if (dest == null) {
				final double[] result = new double[source.length];
				System.arraycopy(source, 0, result, 0, source.length);
				return result;
			} else { // dest != null
				for (int i = 0; i < source.length; i++) {
					if (overrideWithZeros || source[i] != 0.0) {
						dest[i] = source[i];
					}
				}
				return dest;
			}
		}
	}

	// TODO move this into Vector (also the corresponding unit test)
	public static boolean equal(final Vector v1, final Vector v2,
			final double tol) {
		if (v1 == null || v2 == null || v1.size() != v2.size()) {
			return false;
		} else {
			for (int i = 0; i < v1.size(); i++) {
				if (Math.abs(v1.get(i) - v2.get(i)) > tol) {
					return false;
				}
			}
		}
		return true;
	}

	// TODO move this into Matrix (also the corresponding unit test)
	public static boolean equal(final Matrix m1, final Matrix m2,
			final double tol) {
		if (m1 == null || m2 == null || m1.rowSize() != m2.rowSize()) {
			return false;
		} else {
			for (int i = 0; i < m1.rowSize(); i++) {
				if (!equal(m1.getRow(i), m2.getRow(i), tol)) {
					return false;
				}
			}
		}
		return true;
	}

	// TODO NEW
	public static <E> E draw(final Map<E, Double> event2proba, final Random rnd) {
		final double x = rnd.nextDouble();
		double pSum = 0;
		final Iterator<Map.Entry<E, Double>> it = event2proba.entrySet()
				.iterator();
		E result = null;
		do {
			Map.Entry<E, Double> next = it.next();
			result = next.getKey();
			pSum += next.getValue();
		} while (pSum < x && it.hasNext());
		return result;
	}

	// TODO NEW
	// the order of the bounds does not matter
	public static double projectOnInterval(final double value,
			final double bound1, final double bound2) {
		double halfBounded = Math.max(value, Math.min(bound1, bound2));
		return Math.min(halfBounded, Math.max(bound1, bound2));
	}

	// TODO NEW, move this into Vector
	public static Vector hadamardProduct(final Vector x, final Vector y) {
		final Vector result = x.copy();
		for (int i = 0; i < y.size(); i++) {
			result.mult(i, y.get(i));
		}
		return result;
	}

}
