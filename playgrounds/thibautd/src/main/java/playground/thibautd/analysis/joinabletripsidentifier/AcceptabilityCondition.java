/* *********************************************************************** *
 * project: org.matsim.*
 * AccepatabilityCondition.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.List;

/**
 * Defines a condition on wether a trip should be accepted or not.
 * @author thibautd
 */
public class AcceptabilityCondition {
	private final int distance;
	private final int time;

	public AcceptabilityCondition(
			final int distance,
			final int time) {
		this.distance = distance;
		this.time = time;
	}

	public int getDistance() {
		return distance;
	}

	public int getTime() {
		return time;
	}

	public Fullfillment getFullfillment(
			final List<JoinableTrips.Passage> passages) {
		boolean validPUFound = false;
		boolean validDOFound = false;

		double minPuWalkDist = Double.POSITIVE_INFINITY;
		double minDoWalkDist = Double.POSITIVE_INFINITY;

		for (JoinableTrips.Passage passage : passages) {
			double passageDist = passage.getDistance();
			if (passageDist <= distance) {
				switch ( passage.getType() ) {
					case pickUp:
						if (passage.getTimeDifference() >= -time) {
							validPUFound = true;

							if (passageDist < minPuWalkDist) {
								minPuWalkDist = passageDist;
							}
						}
						break;
					case dropOff:
						if (passage.getTimeDifference() <= time) {
							validDOFound = true;

							if (passageDist < minDoWalkDist) {
								minDoWalkDist = passageDist;
							}
						}
						break;
				}
				//if ( validPUFound && validDOFound ) return true;
			}
		}

		return new Fullfillment( validPUFound && validDOFound , minPuWalkDist , minDoWalkDist );
	}

	@Override
	public String toString() {
		return "[distance = "+distance+"; time = "+time+"]";
	}

	@Override
	public boolean equals(final Object other) {
		if ( other == null ) return false;
		AcceptabilityCondition condition = null;

		try {
			condition = (AcceptabilityCondition) other;
		} catch (ClassCastException e) {
			// not a condition
			return false;
		}

		return condition.distance == distance && condition.time == time;
	}

	@Override
	public int hashCode() {
		return distance + time * 1000;
	}

	public static class Fullfillment {
		private final boolean isFullfilled;
		private final double minPuWalkDist;
		private final double minDoWalkDist;

		private Fullfillment(
				final boolean isFullfilled,
				final double minPuWalkDist,
				final double minDoWalkDist) {
			this.isFullfilled = isFullfilled;
			this.minPuWalkDist = minPuWalkDist;
			this.minDoWalkDist = minDoWalkDist;
		}

		public boolean isFullfilled() {
			return this.isFullfilled;
		}

		public double getMinPuWalkDist() {
			return this.minPuWalkDist;
		}

		public double getMinDoWalkDist() {
			return this.minDoWalkDist;
		}
	}
}

