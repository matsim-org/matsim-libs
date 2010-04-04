/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToReplanIdentifier.java
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

package playground.christoph.withinday.replanning.identifiers.interfaces;

import java.util.List;

import org.matsim.core.mobsim.framework.DriverAgent;

import playground.christoph.withinday.replanning.WithinDayReplanner;

/*
 * Identify Agents that need a Replanning of their scheduled
 * plan.
 */
public abstract class AgentsToReplanIdentifier implements Cloneable{
	
	protected boolean checkAllAgents = true;
	
	
	public abstract List<DriverAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner);
	
	
	public boolean checkAllAgents()
	{
		return this.checkAllAgents;
	}
	
	public void checkAllAgents(boolean checkAllAgents)
	{
		this.checkAllAgents = checkAllAgents;
	}
	
	@Override
	public abstract AgentsToReplanIdentifier clone();
	
	protected void cloneBasicData(AgentsToReplanIdentifier clone)
	{
		clone.checkAllAgents = this.checkAllAgents;
	}
}
