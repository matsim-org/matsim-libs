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

package playground.christoph.evacuation.withinday.replanning.identifiers.filters;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilter.FilterType;

public class AffectedAgentsFilterFactory implements AgentFilterFactory {

	private final Scenario scenario;
	private final AgentsTracker agentsTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final CoordAnalyzer coordAnalyzer;
	private final FilterType filterType;
	
	public AffectedAgentsFilterFactory(Scenario scenario, AgentsTracker agentsTracker, MobsimDataProvider mobsimDataProvider, 
			CoordAnalyzer coordAnalyzer, FilterType filterType) {
		this.scenario = scenario;
		this.agentsTracker = agentsTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.coordAnalyzer = coordAnalyzer;
		this.filterType = filterType;
	}
	
	@Override
	public AffectedAgentsFilter createAgentFilter() {
		return new AffectedAgentsFilter(scenario, agentsTracker, mobsimDataProvider, coordAnalyzer, filterType);
	}

}
