/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle;

/**
 * This is just an intermediate solution until 
 * <a href="https://matsim.atlassian.net/browse/MATSIM-700">https://matsim.atlassian.net/browse/MATSIM-700</a>
 * is fixed.
 * 
 * @author dziemke based on amit
 */
@Deprecated
class BicycleSpeedUtils {
	static double getSpeed( final String travelMode ){
		double speed;
		if (travelMode == "bicycle") {
			speed = 20.0/3.6;
		} else {
			throw new RuntimeException("No speed is set for travel mode " + travelMode + ".");
		}
		return speed;
	}
}
