/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideIncluder.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author thibautd
 */
public interface ParkAndRideIncluder {
	public boolean routeAndIncludePnrTrips(
			Activity accessOriginActivity,
			Activity accessDestinationActivity,
			Activity egressOriginActivity,
			Activity egressDestinationActivity,
			Plan plan);
}
