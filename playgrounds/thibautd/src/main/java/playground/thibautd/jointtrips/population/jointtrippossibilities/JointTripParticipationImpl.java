/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripParticipationImpl.java
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

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class JointTripParticipationImpl implements JointTripParticipation {
	private final Id agentId;
	private final Id originId;
	private final Id destinationId;

	/*package*/ JointTripParticipationImpl(
			final Id agentId,
			final Id originId,
			final Id destinationId) {
		this.agentId = agentId;
		this.originId = originId;
		this.destinationId = destinationId;
	}

	@Override
	public Id getAgentId() {
		return agentId;
	}

	@Override
	public Id getOriginActivityId() {
		return originId;
	}

	@Override
	public Id getDestinationActivityId() {
		return destinationId;
	}
}

