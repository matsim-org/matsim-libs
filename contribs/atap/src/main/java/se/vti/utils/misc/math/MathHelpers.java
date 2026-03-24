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
package se.vti.utils.misc.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class MathHelpers {

	private static final MathHelpers globalInstance = new MathHelpers();
	
	public MathHelpers() {		
	}
	
	public static MathHelpers globalInstance() {
		return globalInstance;
	}
	
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

	// TODO NEW
	public static synchronized <T> Set<T> drawWithoutReplacement(int n,
			final Collection<T> collection, final Random rnd) {
		final Set<T> result = new LinkedHashSet<T>();
		while ((result.size() < n) && (result.size() < collection.size())) {
			result.add(draw(collection, rnd));
		}
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

	public static double draw(final double lower, final double upper,
			final Random rnd) {
		return lower + rnd.nextDouble() * (upper - lower);
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

	public static <E> E draw(final Map<E, Double> event2weight,
			final double weightSum, final Random rnd) {
		final double x = weightSum * rnd.nextDouble();
		double cumulativeWeight = 0;
		final Iterator<Map.Entry<E, Double>> it = event2weight.entrySet()
				.iterator();
		E result = null;
		do {
			Map.Entry<E, Double> next = it.next();
			result = next.getKey();
			cumulativeWeight += next.getValue();
		} while (cumulativeWeight < x && it.hasNext());
		return result;
	}

	public static int drawIndex(double[] probas, Random rnd) {
		double u = rnd.nextDouble();
		double probaSum = 0.0;
		for (int i = 0; i < probas.length; i++) {
			probaSum += probas[i];
			if (u < probaSum) {
				return i;
			}
		}
		return (probas.length - 1);
	}
	
	// the order of the bounds does not matter
	public static double projectOnInterval(final double value,
			final double bound1, final double bound2) {
		double halfBounded = Math.max(value, Math.min(bound1, bound2));
		return Math.min(halfBounded, Math.max(bound1, bound2));
	}

	public static void main(String[] test) {

		Map<String, Double> m = new LinkedHashMap<>();
		m.put("A", 1.0);
		m.put("B", 2.0);
		m.put("C", 0.0);

		double aFreq = 0;
		double bFreq = 0;
		double cFreq = 0;
		for (int i = 0; i < 1000; i++) {
			final String draw = draw(m, 3, new Random());
			if ("A".equals(draw)) {
				aFreq++;
			} else if ("B".equals(draw)) {
				bFreq++;
			} else if ("C".equals(draw)) {
				cFreq++;
			}
		}

		System.out.println(aFreq + "\t" + bFreq + "\t" + cFreq);
	}
}
