/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.utils;

import java.util.function.ToDoubleFunction;

/**
 * @author thibautd
 */
public class ArrayUtils {
	/**
	 * Given a sorted array, returns the index of the first object greater than the given value
	 * @param array an array sorted according to the result of function
	 * @param function a function that maps objects to a scalar value
	 * @param maxValue the maximum value the function should take
	 * @param minRange do not search below this index
	 * @param maxRange do not search above this index
	 * @param <T> the type of objects
	 * @return the first index of an object greater than the given value
	 */
	public static <T> int searchLowest(
			final T[] array ,
			final ToDoubleFunction<T> function ,
			final double maxValue,
			final int minRange,
			final int maxRange ) {
		int min = minRange;
		int max = maxRange;

		if ( function.applyAsDouble( array[ min ] ) > maxValue ) return min;

		while ( min < max - 1 ) {
			final int mid = (max + min) / 2;

			// we want the rightmost element: "push" min even if in the right value,
			// as long as possible
			if ( function.applyAsDouble( array[ mid ] ) <= maxValue ) min = mid;
			else max = mid;
		}

		return max;
	}
}

