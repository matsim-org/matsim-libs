/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityPerformingIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning.identifiers;

import java.util.Iterator;
import java.util.List;

import org.matsim.core.mobsim.framework.PersonAgent;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class ActivityPerformingIdentifier extends DuringActivityIdentifier {
	
	protected ActivityReplanningMap activityReplanningMap;
	
	// use the Factory!
	/*package*/ ActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
		List<PersonAgent> agentsToReplan = activityReplanningMap.getActivityPerformingAgents();
		
		Iterator<PersonAgent> iter = agentsToReplan.iterator();
		while(iter.hasNext()) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) iter.next();
			
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner)) {
				iter.remove();
				continue;
			}
		}
		
		
		return agentsToReplan;
	}

}