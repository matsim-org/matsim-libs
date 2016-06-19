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

package playground.polettif.publicTransitMapping.mapping.v2;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.mapping.pseudoPTRouter.LinkCandidate;

import java.util.List;
import java.util.SortedSet;

public interface LinkCandidateCreator {
	
	void createLinkCandidates();

	/**
	 * Returns a list of link candidates for the given stop facility and schedule transport mode.
	 * The list is ordered ascending by priority (distance, likelihood, etc.).
	 */
	SortedSet<LinkCandidate> getLinkCandidates(TransitStopFacility transitStopFacility, String scheduleTransportMode);

	boolean stopFacilityOnlyHasLoopLink(TransitStopFacility stopFacility, String transportMode);

}