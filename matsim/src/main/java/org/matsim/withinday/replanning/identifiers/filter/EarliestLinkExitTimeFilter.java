/* *********************************************************************** *
 * project: org.matsim.*
 * EarliestLinkExitTimeFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;

/**
 * Remove all agents from the set that spent more time on a link that
 * their minimal link travel time. Such agents are already in the QLinks'
 * buffer and cannot stop at that link anymore.
 * 
 * @author cdobler
 */
public class EarliestLinkExitTimeFilter implements AgentFilter {

	private final EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;
	
	// use the factory
	/*package*/ EarliestLinkExitTimeFilter(EarliestLinkExitTimeProvider earliestLinkExitTimeProvider) {
		this.earliestLinkExitTimeProvider = earliestLinkExitTimeProvider;
	}
	
	@Override
	public void applyAgentFilter(Set<Id<Person>> set, double time) {
		set.removeIf(id -> !this.applyAgentFilter(id, time));
	}
	
	@Override
	public boolean applyAgentFilter(Id<Person> id, double time) {
		OptionalTime earliestLinkExitTime = this.earliestLinkExitTimeProvider.getEarliestLinkExitTime(id);
		return earliestLinkExitTime.isDefined() && earliestLinkExitTime.seconds() > time;
	}
}
