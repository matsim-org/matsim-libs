/* *********************************************************************** *
 * project: org.matsim.*
 * LeaveLinkReplanningModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import java.util.List;

import org.matsim.core.mobsim.queuesim.QueueVehicle;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.withinday.replanning.parallel.ParallelLeaveLinkReplanner;

/*
 * Uses a LinkReplanningMap Object to determine which
 * Agents within a Simulation need a replanning of their
 * plans.
 */
public class DuringLegReplanningModule implements WithinDayReplanningModule{

	public static int replanningCounter = 0;
	
	protected ParallelLeaveLinkReplanner parallelLeaveLinkReplanner;
	protected LinkReplanningMap linkReplanningMap;
	
	public DuringLegReplanningModule(ParallelLeaveLinkReplanner parallelLeaveLinkReplanner, LinkReplanningMap linkReplanningMap)
	{
		this.parallelLeaveLinkReplanner = parallelLeaveLinkReplanner;
		this.linkReplanningMap = linkReplanningMap;
	}
	
	public void doReplanning(double time)
	{
		List<QueueVehicle> vehiclesToReplanLeaveLink = linkReplanningMap.getReplanningVehicles(time);
		for (QueueVehicle queueVehicle : vehiclesToReplanLeaveLink)
		{
			parallelLeaveLinkReplanner.addAgentToReplan(queueVehicle.getDriver());
		}
		if (vehiclesToReplanLeaveLink.size() > 0)
		{
			replanningCounter = replanningCounter + vehiclesToReplanLeaveLink.size();
			parallelLeaveLinkReplanner.run(time);
		}
	}

}