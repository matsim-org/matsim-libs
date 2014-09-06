/* *********************************************************************** *
 * project: org.matsim.*
 * NextTransportModeFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers.filter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

/**
 * Remove all agents from the set that...
 * <ul>
 * 	<li>do not perform an activity.</li>
 * 	<li>do not use one of the modes included in the given set of modes for their next trip.</li>
 * </ul>
 * 
 * @author cdobler
 */
public class NextTransportModeFilter implements AgentFilter {

	private final Map<Id<Person>, MobsimAgent> agents;
	private final Set<String> modes;
	
	// use the factory
	/*package*/ NextTransportModeFilter(Map<Id<Person>, MobsimAgent> agents, Set<String> modes) {
		this.agents = agents;
		this.modes = modes;
	}
	
	@Override
	public void applyAgentFilter(Set<Id<Person>> set, double time) {
		Iterator<Id<Person>> iter = set.iterator();
		
		while (iter.hasNext()) {
			Id<Person> id = iter.next();
			if (!this.applyAgentFilter(id, time)) iter.remove();
		}
	}

	@Override
	public boolean applyAgentFilter(Id<Person> id, double time) {
		MobsimAgent agent = this.agents.get(id);
		
		if (!(agent.getState() == MobsimAgent.State.ACTIVITY)) return false;
		
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		List<PlanElement> planElements = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements();
		
		List<PlanElement> subList = planElements.subList(planElementIndex, planElements.size());
		Iterator<PlanElement> iter = subList.iterator();
		while (iter.hasNext()) {
			PlanElement planElement = iter.next();
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (modes.contains(leg.getMode())) return true;
				else return false;	// mode not in set of valid modes
			}
		}
		// no next leg was found
		return false;
	}
}
