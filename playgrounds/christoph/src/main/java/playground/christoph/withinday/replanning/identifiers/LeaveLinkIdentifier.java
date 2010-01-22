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

import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueVehicle;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class LeaveLinkIdentifier extends DuringLegIdentifier{

	protected LinkReplanningMap linkReplanningMap;
	
	public LeaveLinkIdentifier(LinkReplanningMap linkReplanningMap)
	{
		this.linkReplanningMap = linkReplanningMap;
	}
		
	public List<DriverAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner)
	{
		List<QueueVehicle> vehiclesToReplanLeaveLink = linkReplanningMap.getReplanningVehicles(time);
		List<DriverAgent> agentsToReplan = new ArrayList<DriverAgent>(); 

		for (QueueVehicle queueVehicle : vehiclesToReplanLeaveLink)
		{
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) queueVehicle.getDriver();
			if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner))
			{
				agentsToReplan.add(queueVehicle.getDriver());
			}
		}
		
		return agentsToReplan;
	}

	public LeaveLinkIdentifier clone()
	{
		LeaveLinkIdentifier clone = new LeaveLinkIdentifier(this.linkReplanningMap);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
