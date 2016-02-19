/* *********************************************************************** *
 * project: org.matsim.*
 * CenteredTimeProportionalPenalty.java
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
package playground.thibautd.parknride.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Computes the parking penalty using a cost per time unit, which decreases
 * linearly with distance to a reference point.
 *
 * @author thibautd
 */
public class CenteredTimeProportionalPenalty implements ParkingPenalty {
	private static final double UNDEFINED_COST_PER_SEC = Double.NaN;

	// /////////////////////////////////////////////////////////////////////////
	// parameters
	// /////////////////////////////////////////////////////////////////////////
	private final Coord centerPoint;
	private final double zoneRadius;
	private final double maxCostPerSecond;

	// /////////////////////////////////////////////////////////////////////////
	// state variables
	// /////////////////////////////////////////////////////////////////////////
	private double costPerSecond;
	private double parkingTime;
	private double accumulatedParkingCost;

	@Override
	public void reset() {
		accumulatedParkingCost = 0;
		resetLocationDependantState();
	}

	private void resetLocationDependantState() {
		costPerSecond = UNDEFINED_COST_PER_SEC;
		parkingTime = Time.UNDEFINED_TIME;
	}

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Initialises an instance
	 * @param zoneCenter the point where parking cost is higher
	 * @param zoneRadius the distance to the zone center from which parking cost is 0
	 * @param maxCostPerSecond the cost per second at the zone center, in utility units
	 */
	public CenteredTimeProportionalPenalty(
			final Coord zoneCenter,
			final double zoneRadius,
			final double maxCostPerSecond) {
		this.centerPoint = zoneCenter;
		this.zoneRadius = zoneRadius;
		this.maxCostPerSecond = maxCostPerSecond;
		reset();
	}

	// /////////////////////////////////////////////////////////////////////////
	// scoring
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void park(final double time, final Coord coord) {
		if (parkingTime != Time.UNDEFINED_TIME) {
			throw new IllegalStateException( "trying to park while not unparked" );
		}
		if (time == Time.UNDEFINED_TIME) {
			throw new IllegalArgumentException( "got invalid time "+time );
		}
		parkingTime = time;
		costPerSecond = calcCostPerSecond( coord );
	}

	private double calcCostPerSecond(final Coord coord) {
		double distToCenter = CoordUtils.calcEuclideanDistance( coord , centerPoint );
		double cost = maxCostPerSecond - (distToCenter / zoneRadius) * maxCostPerSecond;
		return cost > 0d ? cost : 0d;
	}

	@Override
	public void unPark(final double time) {
		if (time < parkingTime) {
			throw new IllegalArgumentException( "cannot unpark at "+time+": before parking time "+parkingTime );
		}

		accumulatedParkingCost += (time - parkingTime) * costPerSecond;
		resetLocationDependantState();
	}

	@Override
	public void finish() {
		// nothing to do
	}

	@Override
	public double getPenalty() {
		return -accumulatedParkingCost;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters for tests
	// /////////////////////////////////////////////////////////////////////////
	double getCostPerSecond() {
		return costPerSecond;
	}

	double getParkingTime() {
		return parkingTime;
	}
}

