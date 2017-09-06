/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package taxibus.algorithm.scheduler;

/**
 * @author jbischoff
 *
 */
public class TaxibusSchedulerParams  {
	public final boolean destinationKnown = true;
	public final boolean vehicleDiversion = false;
	public final double pickupDuration;
	public final double dropoffDuration;
	public final double AStarEuclideanOverdoFactor = 1.;
	public TaxibusSchedulerParams(double pickupDuration, double dropoffDuration) {
		// We assume we a) know where we are heading to and b) do not allow diversions once a bus is running
		this.pickupDuration = pickupDuration;
		this.dropoffDuration = dropoffDuration;
	}
}
