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

package playground.christoph.mobsim;

import java.util.ArrayList;

import org.matsim.core.mobsim.queuesim.QueueVehicle;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;

/*
 * Uses a LinkReplanningMap Object to determine which
 * Agents within a Simulation need a replanning of their
 * plans.
 */
public class LeaveLinkReplanningModule {

	public static int replanningCounter = 0;
	
	protected ParallelLeaveLinkReplanner parallelLeaveLinkReplanner;
	protected LinkReplanningMap linkReplanningMap;
	
	public LeaveLinkReplanningModule(LinkReplanningMap linkReplanningMap)
	{
		this.linkReplanningMap = linkReplanningMap;
		parallelLeaveLinkReplanner = new ParallelLeaveLinkReplanner();
	}
	
	public void doLeaveLinkReplanning(double time)
	{
		ArrayList<QueueVehicle> vehiclesToReplanLeaveLink = (ArrayList<QueueVehicle>)linkReplanningMap.getReplanningVehicles(time);
		if (vehiclesToReplanLeaveLink.size() > 0)
		{
			replanningCounter = replanningCounter + vehiclesToReplanLeaveLink.size();
			parallelLeaveLinkReplanner.run(vehiclesToReplanLeaveLink, time);
		}
	}
}
