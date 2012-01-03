/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesFactory.java
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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * Provides methods to create joint trips possibilities informationnal objects
 * @author thibautd
 */
public interface JointTripPossibilitiesFactory extends MatsimFactory {
	/**
	 * @param possibilities the list of possibilities
	 * @return an instance of a {@link JointTripPossibilities} implementation
	 * containing the argument possibilities.
	 */
	public JointTripPossibilities createJointTripPossibilities(
			List<JointTripPossibility> possibilities);

	/**
	 * Creates a new instance of a {@link JointTripPossibility} implementation.
	 * @param driver the driver information
	 * @param passenger the passenger information
	 * @return a new instance
	 */
	public JointTripPossibility createJointTripPossibility(
			JointTripParticipation driver,
			JointTripParticipation passenger);

	/**
	 * Creates a new instance of a {@link JointTripParticipation} implementation.
	 *
	 * @param agentId the Id of the agent
	 * @param originId the Id of the origin activity
	 * @param destinationId te Id of the destination activity
	 * @return a new instance
	 */
	public JointTripParticipation createJointTripParticipation(
			Id agentId, Id originId, Id destinationId);
}

