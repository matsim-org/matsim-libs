/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning.replanners.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;

import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.replanners.tools.ReplanningIdGenerator;

public abstract class WithinDayReplannerFactory {

	private Id id;
	private AgentCounterI agentCounter;
	private AbstractMultithreadedModule abstractMultithreadedModule;
	private double replanningProbability = 1.0;
	
	public WithinDayReplannerFactory(AgentCounterI agentCounter, AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability) {
		this.agentCounter = agentCounter;
		this.abstractMultithreadedModule = abstractMultithreadedModule;
		this.replanningProbability = replanningProbability;
		this.id = ReplanningIdGenerator.getNextId();
	}
	
	public abstract WithinDayReplanner<? extends AgentsToReplanIdentifier> createReplanner();
	
	/*
	 * This method should be called after a new Replanner instance
	 * has been created. Is there any way to force this???
	 */
	public final void initNewInstance(WithinDayReplanner<? extends AgentsToReplanIdentifier> replanner) {
		replanner.setReplannerFactory(this);
		replanner.setAgentCounter(agentCounter);
		replanner.setReplanningProbability(this.replanningProbability);
		replanner.setAbstractMultithreadedModule(this.abstractMultithreadedModule);
		additionalParametersForNewInstance(replanner);
	}

	/*
	 * Override this method if you want to set additional parameters in 
	 * a new created WithinDayReplanner instance. 
	 */
	public void additionalParametersForNewInstance(WithinDayReplanner<? extends AgentsToReplanIdentifier> replanner) {
	}
	
	public final Id getId() {
		return this.id;
	}
}
