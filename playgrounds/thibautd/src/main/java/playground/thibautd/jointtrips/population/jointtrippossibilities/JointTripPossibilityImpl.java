/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilityImpl.java
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
 * Default immutable implementation of a JointTripPossibility object.
 * It provides no public constructor: instances are obtained from factories
 * or builders.
 * 
 * @author thibautd
 */
public class JointTripPossibilityImpl implements JointTripPossibility {
	private final JointTripParticipation driver;
	private final JointTripParticipation passenger;

	/* package */ JointTripPossibilityImpl(
			final JointTripParticipation driver,
			final JointTripParticipation passenger) {
		this.driver = driver;
		this.passenger = passenger;
	}

	@Override
	public JointTripParticipation getDriver() {
		return driver;
	}

	@Override
	public JointTripParticipation getPassenger() {
		return passenger;
	}
}

