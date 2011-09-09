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
	private final double distance;
	private final double time;

	public AcceptabilityCondition(
			final double distance,
			final double time) {
		this.distance = distance;
		this.time = time;
	}

	public double getDistance() {
		return distance;
	}

	public double getTime() {
		return time;
	}

	public boolean isFullfilled(
			final List<JoinableTrips.Passage> passages) {
		boolean validPUFound = false;
		boolean validDOFound = false;

		for (JoinableTrips.Passage passage : passages) {
			if (passage.getDistance() <= distance) {
				switch ( passage.getType() ) {
					case pickUp:
						if (passage.getTimeDifference() >= -time) {
							validPUFound = true;
						}
						break;
					case dropOff:
						if (passage.getTimeDifference() <= time) {
							validDOFound = true;
						}
						break;
				}
				if ( validPUFound && validDOFound ) return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return "[distance = "+distance+"; time = "+time+"]";
	}

	@Override
	public boolean equals(final Object other) {
		AcceptabilityCondition condition = null;

		try {
			condition = (AcceptabilityCondition) other;
		} catch (ClassCastException e) {
			// not a condition
			return false;
		}

		return (this.distance == condition.distance) && (this.time == condition.time);
	}

	@Override
	public int hashCode() {
		return (int) (distance + (time * 100000));
	}
}

