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

import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QVehicle;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class LeaveLinkIdentifier extends DuringLegIdentifier{

	protected QSim qSim;
	protected LinkReplanningMap linkReplanningMap;
	
	public LeaveLinkIdentifier(QSim qSim)
	{
		this.qSim = qSim;
		linkReplanningMap = new LinkReplanningMap(qSim);
	}
	
	// Only for Cloning.
	private LeaveLinkIdentifier(QSim qSim, LinkReplanningMap linkReplanningMap)
	{
		this.qSim = qSim;
		this.linkReplanningMap = linkReplanningMap;
	}
	
	public List<DriverAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner)
	{
		List<QVehicle> vehiclesToReplanLeaveLink = linkReplanningMap.getReplanningVehicles(time);
		List<DriverAgent> agentsToReplan = new ArrayList<DriverAgent>(); 

		for (QVehicle qVehicle : vehiclesToReplanLeaveLink)
		{
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) qVehicle.getDriver();
			if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner))
			{
				agentsToReplan.add(qVehicle.getDriver());
			}
		}
		
		return agentsToReplan;
	}

	public LeaveLinkIdentifier clone()
	{
		/*
		 *  We don't want to clone the linkReplanningMap. Instead we
		 *  reuse the existing one.
		 */
		LeaveLinkIdentifier clone = new LeaveLinkIdentifier(this.qSim, this.linkReplanningMap);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
