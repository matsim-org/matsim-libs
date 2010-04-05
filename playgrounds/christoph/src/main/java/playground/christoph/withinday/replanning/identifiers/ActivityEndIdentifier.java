/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndIdentifier.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.core.mobsim.framework.PersonDriverAgent;

import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSim;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class ActivityEndIdentifier extends DuringActivityIdentifier{

	protected KnowledgeWithinDayQSim simulation;
	protected PriorityBlockingQueue<PersonDriverAgent> queue;
	
	public ActivityEndIdentifier(KnowledgeWithinDayQSim simulation)
	{
		this.simulation = simulation;
		this.queue = simulation.getActivityEndsList();
	}
		
	public List<PersonDriverAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner)
	{
		List<PersonDriverAgent> agentsToReplan = new ArrayList<PersonDriverAgent>(); 
				
		for (PersonDriverAgent driverAgent : queue)
		{	
			// If the Agent will depart
			if (driverAgent.getDepartureTime() <= time)
			{	
				WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) driverAgent;
				if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner))
				{
					agentsToReplan.add(driverAgent);
				}
			}
			
			// It's a priority Queue -> no further Agents will be found
			else break;
		}
		
		return agentsToReplan;
	}

	public ActivityEndIdentifier clone()
	{
		ActivityEndIdentifier clone = new ActivityEndIdentifier(this.simulation);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
