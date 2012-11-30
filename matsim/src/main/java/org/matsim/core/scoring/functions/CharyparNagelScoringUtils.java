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

	public static double computeZeroUtilityDuration(final double priority,
			final double typicalDuration_s) {
		final double zeroUtilityDuration = typicalDuration_s * Math.exp( -10.0 / (typicalDuration_s / 3600.0) / priority );
		// ( the 3600s are in there because the original formulation was in "hours".  So the values in seconds are first
		// translated into hours.  kai, sep'12 )

		return zeroUtilityDuration;
	}

}
