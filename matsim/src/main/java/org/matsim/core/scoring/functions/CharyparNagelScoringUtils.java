/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.scoring.functions;

/**
 * @author nagel
 *
 */
public class CharyparNagelScoringUtils {
	private CharyparNagelScoringUtils() {} // container for static methods, should not be instantiated

	/**
	 * This came about as the solution to <br>
	 * <code>
	 *    beta[perf] * t[typ] * ln( t[typ] / t0 ) = beta[perf] * 36000 , <br>
	 * </code>
	 * i.e. the all activities at their typical durations should have the same value of beta[perf] * 36000.     
	 * <p/>
	 * This looks a bit nicer when you put in the typical beta[perf] = 6/h = 6 / (3600 sec), since then the right-hand side becomes 60, i.e. all
	 * activities at their typical durations should have the same value of 60.
	 * <p/>
	 * Given our current understanding, this is not the best of all formulations, but their is no completely easy fix (see documentation) and so we leave it
	 * like this for backwards compatibility.
	 */
	public static double computeZeroUtilityDuration(final double priority,
			final double typicalDuration_s) {
		final double zeroUtilityDuration = typicalDuration_s * Math.exp( -10.0 / (typicalDuration_s / 3600.0) / priority );
		// ( the 3600s are in there because the original formulation was in "hours".  So the values in seconds are first
		// translated into hours.  kai, sep'12 )

		return zeroUtilityDuration;
	}

}
