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

package org.matsim.withinday.replanning.replanners.interfaces;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.replanning.identifiers.interfaces.Identifier;

/*
 *	Each WithinDayReplanner needs one or more AgentsToReplanIdentifier
 *	which identifies Agents that need a Replanning of their scheduled
 * 	Plans.
 */
public abstract class WithinDayReplanner<T extends Identifier> {
	
	protected final Id id;
	protected final Scenario scenario;
	protected final InternalInterface internalInterface;
	private final Random random;
	
	protected AbstractMultithreadedModule abstractMultithreadedModule;
	protected PlanAlgorithm routeAlgo;
	protected double time = Time.UNDEFINED_TIME;
	
	private double replanningProbability = 1.0;

	public WithinDayReplanner(Id id, Scenario scenario, InternalInterface internalInterface) {
		this.id = id;
		this.scenario = scenario;
		this.internalInterface = internalInterface;
		
		this.random = MatsimRandom.getLocalInstance();
	}
	
	public abstract boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent);

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
	
	public void reset() {
		this.time = Time.UNDEFINED_TIME;
	}
	
	@Override	
	public boolean equals(Object o) {
		if (o instanceof WithinDayReplanner) {
			WithinDayReplanner<?> replanner = (WithinDayReplanner<?>) o;
			return replanner.getId().equals(this.getId());
		}
		return false;
	}
}