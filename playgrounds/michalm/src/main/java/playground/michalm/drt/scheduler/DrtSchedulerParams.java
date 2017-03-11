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

package playground.michalm.drt.scheduler;

import org.matsim.contrib.taxi.run.TaxiConfigGroup;

/**
 * @author michalm
 */
public class DrtSchedulerParams {
	public final boolean vehicleDiversion;
	public final double pickupDuration;// TODO per passenger??
	public final double dropoffDuration;// TODO per passenger??
	public final double AStarEuclideanOverdoFactor;

	public DrtSchedulerParams(TaxiConfigGroup taxiCfg) {
		this.vehicleDiversion = taxiCfg.isVehicleDiversion();
		this.pickupDuration = taxiCfg.getPickupDuration();
		this.dropoffDuration = taxiCfg.getDropoffDuration();
		this.AStarEuclideanOverdoFactor = taxiCfg.getAStarEuclideanOverdoFactor();
	}

	public DrtSchedulerParams(boolean destinationKnown, boolean vehicleDiversion, double pickupDuration,
			double dropoffDuration, double AStarEuclideanOverdoFactor) {
		this.vehicleDiversion = vehicleDiversion;
		this.pickupDuration = pickupDuration;
		this.dropoffDuration = dropoffDuration;
		this.AStarEuclideanOverdoFactor = AStarEuclideanOverdoFactor;
	}
}