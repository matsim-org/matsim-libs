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

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
public abstract class WithinDayReplanner<T extends AgentsToReplanIdentifier> {
	
	protected Id id;
	protected Scenario scenario;
	protected AgentCounterI agentCounter;
	protected AbstractMultithreadedModule abstractMultithreadedModule;
	protected PlanAlgorithm routeAlgo;
	protected double time;
	private Set<T> identifiers = new HashSet<T>();
	private double replanningProbability = 1.0;
	private Random random;
	
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
	public final boolean replanAgent() {
		double rand = random.nextDouble();
		if (rand <= replanningProbability) return true;
		else return false;
	}
	
	public final Id getId() {
		return this.id;
	}
	
	public final double getReplanningProbability() {
		return this.replanningProbability;
	}
	
	public final void setReplanningProbability(double probability) {
		this.replanningProbability = probability;
	}
	
	public final double getTime() {
		return this.time;
	}
	
	public final void setTime(double time) {
		this.time = time;
	}
	
	public final void setAbstractMultithreadedModule(AbstractMultithreadedModule module) {
		this.abstractMultithreadedModule = module;
		this.routeAlgo = module.getPlanAlgoInstance();
	}
	
	public final void setAgentCounter(AgentCounterI agentCounter) {
		this.agentCounter = agentCounter;
	}
	
	public final boolean addAgentsToReplanIdentifier(T identifier) {
		return this.identifiers.add(identifier);
	}
	
	public final boolean removeAgentsToReplanIdentifier(T identifier) {
		return this.identifiers.remove(identifier);
	}
	
	public final Set<T> getAgentsToReplanIdentifers() {
		return Collections.unmodifiableSet(identifiers);
	}
		
	public final void setReplannerFactory(WithinDayReplannerFactory factory) {
		this.replannerFactory = factory;
	}
	
	public final WithinDayReplannerFactory getReplannerFactory() {
		return replannerFactory;
	}
}