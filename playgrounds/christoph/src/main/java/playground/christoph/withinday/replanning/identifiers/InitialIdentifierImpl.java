/* *********************************************************************** *
 * project: org.matsim.*
 * InitialIdentifierImpl.java
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
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QVehicle;

import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSim;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class InitialIdentifierImpl extends InitialIdentifier{

	protected KnowledgeWithinDayQSim simulation;
		
	public InitialIdentifierImpl(KnowledgeWithinDayQSim simulation)
	{
		this.simulation = simulation;
	}
		
	public List<DriverAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner)
	{
		List<DriverAgent> agentsToReplan = new ArrayList<DriverAgent>();
		
		for (QLink qLink : simulation.getQNetwork().getLinks().values())
		{
			for (QVehicle vehicle : qLink.getAllVehicles())
			{
				DriverAgent driverAgent = vehicle.getDriver();
				
				WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) driverAgent;
				if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner))
				{
					agentsToReplan.add(driverAgent);
				}
			}
		}
		
		return agentsToReplan;
	}

	public InitialIdentifierImpl clone()
	{
		InitialIdentifierImpl clone = new InitialIdentifierImpl(this.simulation);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
