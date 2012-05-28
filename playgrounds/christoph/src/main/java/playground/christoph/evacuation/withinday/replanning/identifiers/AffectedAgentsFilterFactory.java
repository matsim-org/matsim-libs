/* *********************************************************************** *
 * project: org.matsim.*
 * AffectedAgentsFilterFactory.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.withinday.replanning.identifiers.AffectedAgentsFilter.FilterType;

public class AffectedAgentsFilterFactory implements AgentFilterFactory {

	private final Scenario scenario;
	private final AgentsTracker agentsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final FilterType filterType;
	
	public AffectedAgentsFilterFactory(Scenario scenario, AgentsTracker agentsTracker, VehiclesTracker vehiclesTracker, 
			CoordAnalyzer coordAnalyzer, FilterType filterType) {
		this.scenario = scenario;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		this.filterType = filterType;
	}
	
	@Override
	public AgentFilter createAgentFilter() {
		return new AffectedAgentsFilter(scenario, agentsTracker, vehiclesTracker, coordAnalyzer, filterType);
	}

}
