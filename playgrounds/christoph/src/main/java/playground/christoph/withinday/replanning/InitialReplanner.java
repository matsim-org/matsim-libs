/* *********************************************************************** *
 * project: org.matsim.*
 * InitialReplanner.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.queuesim.DriverAgent;

import playground.christoph.router.util.KnowledgeTools;

/*
 * The InitialReplanner can be used when the Simulations is initialized but
 * has not started yet.
 */

public class InitialReplanner extends WithinDayInitialReplanner{
		
	private static final Logger log = Logger.getLogger(InitialReplanner.class);

	private KnowledgeTools knowledgeTools = new KnowledgeTools();
	private boolean removeKnowledge = true;
	
	public InitialReplanner(Id id)
	{
		super(id);
	}
	
	public void setRemoveKnowledge(boolean removeKnowledge)
	{
		this.removeKnowledge = removeKnowledge;
	}
	
	public boolean doReplanning(DriverAgent driverAgent)
	{	
		// If we don't have a valid Replanner.
		if (this.planAlgorithm == null) return false;
		
		// If we don't have a valid WithinDayPersonAgent
		if (driverAgent == null) return false;
		
		Person person = driverAgent.getPerson();
		
		planAlgorithm.run(person.getSelectedPlan());

		// If flag is set, remove Knowledge after doing the replanning.
		if (removeKnowledge)
		{
			knowledgeTools.removeKnowledge(person);
			knowledgeTools.removeSubNetwork(person);
		}
		
		return true;
	}
	
	public InitialReplanner clone()
	{
		InitialReplanner clone = new InitialReplanner(this.id);
		clone.setRemoveKnowledge(this.removeKnowledge);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
	
}