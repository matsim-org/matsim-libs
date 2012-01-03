/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesFactoryImpl.java
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

/**
 * Default implementation of a {@link JointTripPossibilitiesFactory}.
 *
 * @author thibautd
 */
class JointTripPossibilitiesFactoryImpl implements JointTripPossibilitiesFactory {

	@Override
	public JointTripPossibilities createJointTripPossibilities(
			final List<JointTripPossibility> possibilities) {
		return new JointTripPossibilitiesImpl( possibilities );
	}

	@Override
	public JointTripPossibility createJointTripPossibility(
			final JointTripParticipation driver,
			final JointTripParticipation passenger) {
		return new JointTripPossibilityImpl( driver , passenger );
	}

	@Override
	public JointTripParticipation createJointTripParticipation(
			final Id agentId,
			final Id originId,
			final Id destinationId) {
		return new JointTripParticipationImpl( agentId , originId , destinationId );
	}
}

