/* *********************************************************************** *
 * project: org.matsim.*
 * TripInfo.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

/**
 * Gives access to information on a particular trip.
 * @author thibautd
 */
public class TripInfo {
	private final double minPuWalkDistance;
	private final double minDoWalkDistance;

	public TripInfo(
			final double minPuWalkDistance,
			final double minDoWalkDistance) {
		this.minPuWalkDistance = minPuWalkDistance;
		this.minDoWalkDistance = minDoWalkDistance;
	}

	public TripInfo(
			final AcceptabilityCondition.Fullfillment fullfillment) {
		this( fullfillment.getMinPuWalkDist() , fullfillment.getMinDoWalkDist() );
	}

	public double getMinPuWalkDistance() {
		return minPuWalkDistance;
	}

	public double getMinDoWalkDistance() {
		return minDoWalkDistance;
	}

	@Override
	public boolean equals(final Object other) {
		if ( !(other instanceof TripInfo) ) return false;
		double eps = 1E-7;
		double puDiff = minPuWalkDistance - ((TripInfo) other).minPuWalkDistance;
		double doDiff = minDoWalkDistance - ((TripInfo) other).minDoWalkDistance;
		return Math.abs( puDiff ) < eps && Math.abs( doDiff ) < eps;
	}

	@Override
	public int hashCode() {
		return (int) (minPuWalkDistance + minDoWalkDistance);
	}
}

