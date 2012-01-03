/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibility.java
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
package playground.thibautd.jointtrips.population.jointtrippossibilities;

/**
 * Describes a possible joint trips, by identifying driver and passenger.
 * If a driver drives several passengers from the same origin to the same
 * destination, there must be one possibility per passenger.
 * @author thibautd
 */
public interface JointTripPossibility {
	/**
	 * @return the {@link JointTripParticipation} describing the driver trip
	 */
	public JointTripParticipation getDriver();

	/**
	 * @return a {@link JointTripParticipation} describing the passenger trip
	 */
	public JointTripParticipation getPassenger();
}

