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

/**
 *  
 * @author cdobler
 */
public interface AgentFilter {

	/**
	 * Agents that do not match the filter criteria are removed from the set.
	 */
	public void applyAgentFilter(Set<Id> set, double time);
}
