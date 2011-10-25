/* *********************************************************************** *
 * project: org.matsim.*
 * MateProposer.java
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;

/**
 * Class which selects possible mates, to choose from based on some model.
 *
 * @author thibautd
 */
public interface MateProposer {
	/**
	 * @return true if mate proposition is reflexive.
	 */
	public boolean isReflexive();

	/**
	 * @param trip the trip for which a mate is searched
	 * @param allPossibleMates a list of all requests corresponding to the possible
	 * mates, from the mate perspective.
	 *
	 * @return a list of mates proposals, from the mate perspective. This should
	 * be a sublist of allPossibleMates.
	 */
	public <T extends TripRequest> List<T> proposeMateList(
			TripRequest trip,
			List<T> allPossibleMates);

	/**
	 * Changes the attributes of a request to match charateristics of another agent.
	 *
	 * @param tripToConsider the trip to change (the parameter instance must not be modified)
	 * @param perspective the "perspective" under which the trip is to see. That is,
	 * the returned alternative must be a valid alternative for mode choice for this
	 * trip, for this decision maker.
	 *
	 * @return the new alternative
	 */
	public TripRequest changePerspective(
			TripRequest tripToConsider,
			TripRequest perspective);
}

