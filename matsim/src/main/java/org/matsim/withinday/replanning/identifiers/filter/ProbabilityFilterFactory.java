/* *********************************************************************** *
 * project: org.matsim.*
 * ProbabilityFilterFactory.java
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

import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

public class ProbabilityFilterFactory implements AgentFilterFactory {

	private final double replanningProbability;

	public ProbabilityFilterFactory(double replanningProbability) {
		this.replanningProbability = replanningProbability;
	}
	
	@Override
	public ProbabilityFilter createAgentFilter() {
		return new ProbabilityFilter(replanningProbability);
	}
}