/* *********************************************************************** *
 * project: org.matsim.*
 * SearchParkingAgentsIdentifier.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class SearchParkingAgentsIdentifier extends DuringLegIdentifier {
	
	/*
	 * TODO:
	 * Add a datastructure that logs when an agent has been replanned
	 */
	private final LinkReplanningMap linkReplanningMap;
	private final ParkingAgentsTracker parkingAgentsTracker;
	
	public SearchParkingAgentsIdentifier(LinkReplanningMap linkReplanningMap, ParkingAgentsTracker parkingAgentsTracker) {
		this.linkReplanningMap = linkReplanningMap;
		this.parkingAgentsTracker = parkingAgentsTracker;
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		Set<PlanBasedWithinDayAgent> agents = this.linkReplanningMap.getLegPerformingAgents();
		
		for (PlanBasedWithinDayAgent agent : agents) {
			
			/*
			 * - get current link Id
			 * - get current link
			 * - get destination facility (leg after the parking activity!!!)
			 * - calculate distance to facility
			 * - decide whether replanning should be enabled
			 */
//			agent.getCurrentLinkId()
		}
		
		return new TreeSet<PlanBasedWithinDayAgent>();
	}


}
