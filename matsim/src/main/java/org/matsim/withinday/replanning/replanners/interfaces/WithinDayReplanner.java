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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
	
	protected AbstractMultithreadedModule abstractMultithreadedModule;
	protected PlanAlgorithm routeAlgo;
	protected double time = Time.UNDEFINED_TIME;

	public WithinDayReplanner(Id id, Scenario scenario, InternalInterface internalInterface) {
		this.id = id;
		this.scenario = scenario;
		this.internalInterface = internalInterface;
	}
	
	public abstract boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent);

	
	public final Id getId() {
		return this.id;
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