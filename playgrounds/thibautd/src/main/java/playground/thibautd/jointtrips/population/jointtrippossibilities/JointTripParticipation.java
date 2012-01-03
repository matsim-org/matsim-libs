/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripParticipation.java
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
 * identifies a participant in a joint trip, and the place of the
 * joint trip in its plan.
 *
 * @author thibautd
 */
public interface JointTripParticipation {
	/**
	 * @return the id of the corresponding agent
	 */
	public Id getAgentId();
	/**
	 * @return the Id of the origin activity (this implies
	 * that identifiable activities must be used)
	 */
	public Id getOriginActivityId();
	/**
	 * @return the Id of the destination activity (this implies
	 * that identifiable activities must be used)
	 */
	public Id getDestinationActivityId();
}

