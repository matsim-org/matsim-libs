/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplanner.java
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

package playground.christoph.withinday.replanning;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;

/*
 *	Each WithinDayReplanner needs one or more AgentsToReplanIdentifier
 *	which identifies Agents that need a Replanning of their scheduled
 * 	Plans.
 */
public abstract class WithinDayReplanner implements Cloneable {
	
	private static final Logger log = Logger.getLogger(WithinDayReplanner.class);
	
	protected Scenario scenario;
	protected Id id;
	protected double time;
	protected PlanAlgorithm planAlgorithm;
	protected List<AgentsToReplanIdentifier> identifiers = new ArrayList<AgentsToReplanIdentifier>();
	
	public WithinDayReplanner(Id id, Scenario scenario)
	{
		this.id = id;
		this.scenario = scenario;
	}
	
	public abstract boolean doReplanning(PersonDriverAgent driverAgent);
	
	public Id getId()
	{
		return this.id;
	}
	
	public void setTime(double time)
	{
		this.time = time;
	}
	
	public void setReplanner(PlanAlgorithm planAlgorithm)
	{
		this.planAlgorithm = planAlgorithm;
	}
	
	public boolean addAgentsToReplanIdentifier(AgentsToReplanIdentifier identifier)
	{
		return this.identifiers.add(identifier);
	}
	
	public boolean removeAgentsToReplanIdentifier(AgentsToReplanIdentifier identifier)
	{
		return this.identifiers.remove(identifier);
	}
	
	public List<AgentsToReplanIdentifier> getAgentsToReplanIdentifers()
	{
		return Collections.unmodifiableList(identifiers);
	}
	
	@Override
	public abstract WithinDayReplanner clone();
	
	protected void cloneBasicData(WithinDayReplanner clone)
	{
		clone.setTime(this.time);
		
		if (this.planAlgorithm instanceof Cloneable)
		{
			try
			{
				Method method;
				method = planAlgorithm.getClass().getMethod("clone", new Class[]{});
				clone.setReplanner(planAlgorithm.getClass().cast(method.invoke(planAlgorithm, new Object[]{})));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		else
		{
			log.warn("Could not clone the PlanAlgorithm - use reference to existing one!");
			clone.setReplanner(planAlgorithm);
		}
		
		for (AgentsToReplanIdentifier identifier : this.identifiers)
		{
			if (identifier instanceof Cloneable)
			{
				try
				{
					Method method;
					method = identifier.getClass().getMethod("clone", new Class[]{});
					clone.addAgentsToReplanIdentifier(identifier.getClass().cast(method.invoke(identifier, new Object[]{})));
				}
				catch (Exception e)
				{
					Gbl.errorMsg(e);
				} 
			}
			else
			{
				log.warn("Could not clone the AgentsToReplanIdentifier - use reference to existing one!");
				clone.addAgentsToReplanIdentifier(identifier);
			}
		}
	}
}