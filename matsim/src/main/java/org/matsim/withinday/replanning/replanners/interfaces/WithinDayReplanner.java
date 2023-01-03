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
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;

/*
 *	Each WithinDayReplanner needs one or more AgentsToReplanIdentifier
 *	which identifies Agents that need a Replanning of their scheduled
 * 	Plans.
 */
/**
 * @param <T>  
 */
public abstract class WithinDayReplanner<T extends AgentSelector> {
	
	protected final Id<WithinDayReplanner> id;
	protected final Scenario scenario;
	protected final ActivityEndRescheduler internalInterface;

	protected OptionalTime time = OptionalTime.undefined();

	public WithinDayReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler activityEndRescheduler) {
		this.id = id;
		this.scenario = scenario;
		this.internalInterface = activityEndRescheduler;
	}
	
	public abstract boolean doReplanning(MobsimAgent withinDayAgent);
	
	public final Id<WithinDayReplanner> getId() {
		return this.id;
	}
	
	public final OptionalTime getTime() {
		return this.time;
	}
	
	public final void setTime(double time) {
		this.time = OptionalTime.defined(time);
	}
	
	public void reset() {
		this.time = OptionalTime.undefined();
	}
	
	@Override	
	public boolean equals(Object o) {
		if (o instanceof WithinDayReplanner) {
			WithinDayReplanner<?> replanner = (WithinDayReplanner<?>) o;
			return replanner.getId().equals(this.getId());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}