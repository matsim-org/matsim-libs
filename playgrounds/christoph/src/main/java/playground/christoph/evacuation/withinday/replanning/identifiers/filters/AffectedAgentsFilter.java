/* *********************************************************************** *
 * project: org.matsim.*
 * AffectedAgentsFilter.java
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

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.Facility;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;

public class AffectedAgentsFilter implements AgentFilter {

	private static final Logger log = Logger.getLogger(AffectedAgentsFilter.class);
	
	/*
	 * Affected ... only agents which are affected are kept
	 * NotAffected ... only agents which are not affected are kept
	 */
	public static enum FilterType {
		Affected, NotAffected
	}
	
	private final Scenario scenario;
	private final AgentsTracker agentsTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final CoordAnalyzer coordAnalyzer;
	private final FilterType filterType;
	
	public AffectedAgentsFilter(Scenario scenario, AgentsTracker agentsTracker, MobsimDataProvider mobsimDataProvider, 
			CoordAnalyzer coordAnalyzer, FilterType filterType) {
		this.scenario = scenario;
		this.agentsTracker = agentsTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.coordAnalyzer = coordAnalyzer;
		this.filterType = filterType;
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
		AgentPosition agentPosition = this.agentsTracker.getAgentPosition(id);
		Position positionType = agentPosition.getPositionType();

		boolean affected = false;
		if (positionType == Position.LINK) {
			Id linkId = agentPosition.getPositionId();
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			affected = this.coordAnalyzer.isLinkAffected(link);
		} else if (positionType == Position.FACILITY) {
			Id facilityId = agentPosition.getPositionId();
			Facility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId);
			affected = this.coordAnalyzer.isFacilityAffected(facility);
		} else if (positionType == Position.VEHICLE) {
			Id linkId = this.mobsimDataProvider.getVehicle(agentPosition.getPositionId()).getCurrentLink().getId();
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			affected = this.coordAnalyzer.isLinkAffected(link);
		} else {
			log.warn("Agent's position is undefined! Id: " + id);
		}
		
		// apply filter
		if (filterType == FilterType.Affected && !affected) return false;
		else if (filterType == FilterType.NotAffected && affected) return false;
		return true;
	}
}