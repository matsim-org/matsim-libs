/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.linkCandidateCreation;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.SortedSet;

/**
 * Interface for a class to create and store {@link LinkCandidate}s
 *
 * @author polettif
 */
public interface LinkCandidateCreator {
	
	void createLinkCandidates();

	/**
	 * @return A set of link candidates for the given stop facility and schedule transport mode.
	 * The set should be ordered descending by priority (based on distance, likelihood, etc.).
	 */
	SortedSet<LinkCandidate> getLinkCandidates(Id<TransitStopFacility> transitStopFacility, String scheduleTransportMode);

}