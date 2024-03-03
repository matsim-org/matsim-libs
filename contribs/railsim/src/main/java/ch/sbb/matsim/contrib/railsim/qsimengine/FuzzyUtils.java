/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

/**
 * Util class for fuzzy comparisons.
 */
final class FuzzyUtils {

	/**
	 * The allowed deviation for small numbers to be considered equal.
	 * Contrary to intuition, this value should not be too large.
	 * It might happen that trains are moved too earlier over links.
	 */
	private static final double EPSILON = 1E-6;

	private FuzzyUtils() {
	}

	/**
	 * Returns true if two doubles are approximately equal.
	 */
	public static boolean equals(double a, double b) {
		return a == b || Math.abs(a - b) < EPSILON;
	}

	/**
	 * Returns true if the first double is approximately greater than the second.
	 */
	public static boolean greaterEqualThan(double a, double b) {
		return equals(a, b) || a - b > EPSILON;
	}

	/**
	 * Returns true if the first double is certainly greater than the second.
	 */
	public static boolean greaterThan(double a, double b) {
		return a - b > EPSILON;
	}

	/**
	 * Returns true if the first double is approximately less than the second.
	 */
	public static boolean lessEqualThan(double a, double b) {
		return equals(a, b) || b - a > EPSILON;
	}

	/**
	 * Returns true if the first double is approximately less than the second.
	 */
	public static boolean lessThan(double a, double b) {
		return b - a > EPSILON;
	}

}
