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

package playground.christoph.withinday.replanning.replanners.interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;

import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

/*
 *	Each WithinDayReplanner needs one or more AgentsToReplanIdentifier
 *	which identifies Agents that need a Replanning of their scheduled
 * 	Plans.
 */
public abstract class WithinDayReplanner {
	
	protected Scenario scenario;
	protected Id id;
	protected double time;
	protected AbstractMultithreadedModule abstractMultithreadedModule;
	protected PlanAlgorithm routeAlgo;
	protected List<AgentsToReplanIdentifier> identifiers = new ArrayList<AgentsToReplanIdentifier>();
	protected double replanningProbability = 1.0;
	protected Random random;
	protected AgentCounterI agentCounter;
	
	private WithinDayReplannerFactory replannerFactory;
	
	public WithinDayReplanner(Id id, Scenario scenario) {
		this.id = id;
		this.scenario = scenario;
		
		this.random = MatsimRandom.getLocalInstance();
	}
	
	public abstract boolean doReplanning(PersonAgent personAgent);

	/*
	 * Based on a random number it is decided whether an agent should
	 * do a replanning or not.
	 * number <= replanningProbability: do replanning 
	 * else: no replanning
	 */
	public boolean replanAgent() {
		double rand = random.nextDouble();
		if (rand <= replanningProbability) return true;
		else return false;
	}
	
	public Id getId() {
		return this.id;
	}
	
	public double getReplanningProbability() {
		return this.replanningProbability;
	}
	
	public void setReplanningProbability(double probability) {
		this.replanningProbability = probability;
	}
	
	public double getTime() {
		return this.time;
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public void setAbstractMultithreadedModule(AbstractMultithreadedModule module) {
		this.abstractMultithreadedModule = module;
		this.routeAlgo = module.getPlanAlgoInstance();
	}
	
	public void setAgentCounter(AgentCounterI agentCounter) {
		this.agentCounter = agentCounter;
	}
	
	public boolean addAgentsToReplanIdentifier(AgentsToReplanIdentifier identifier) {
		return this.identifiers.add(identifier);
	}
	
	public boolean removeAgentsToReplanIdentifier(AgentsToReplanIdentifier identifier) {
		return this.identifiers.remove(identifier);
	}
	
	public List<AgentsToReplanIdentifier> getAgentsToReplanIdentifers() {
		return Collections.unmodifiableList(identifiers);
	}
		
	public final void setReplannerFactory(WithinDayReplannerFactory factory) {
		this.replannerFactory = factory;
	}
	
	public final WithinDayReplannerFactory getReplannerFactory() {
		return replannerFactory;
	}
}