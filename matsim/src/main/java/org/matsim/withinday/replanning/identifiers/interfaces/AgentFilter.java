/* *********************************************************************** *
 * project: org.matsim.*
 * AgentFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers.interfaces;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * AgentFilters are often applied to several agents stored in unordered data
 * structures. Therefore please ensure that the filter result does NOT depend
 * on the order in which agents are processed. If e.g. random numbers are drawn,
 * set a deterministic seed (e.g. agentId.hashCode() + (long) time).
 *  
 * @author cdobler
 */
public interface AgentFilter {

	/**
	 * Agents that do not match the filter criteria are removed from the set.
	 */
	public void applyAgentFilter(Set<Id<Person>> set, double time);

	/**
	 * Returns true if the agent matches the filter criteria, otherwise returns false.
	 */
	public boolean applyAgentFilter(Id<Person> id, double time);
}
