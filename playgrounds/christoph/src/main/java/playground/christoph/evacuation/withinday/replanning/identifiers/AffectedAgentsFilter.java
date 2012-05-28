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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
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
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final FilterType filterType;
	
	public AffectedAgentsFilter(Scenario scenario, AgentsTracker agentsTracker, VehiclesTracker vehiclesTracker, 
			CoordAnalyzer coordAnalyzer, FilterType filterType) {
		this.scenario = scenario;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		this.filterType = filterType;
	}
		
	@Override
	public void applyAgentFilter(Set<Id> set, double time) {
		
		Iterator<Id> iter = set.iterator();
		while (iter.hasNext()) {
			Id agentId = iter.next();
			AgentPosition agentPosition = this.agentsTracker.getAgentPosition(agentId);
			Position positionType = agentPosition.getPositionType();

			boolean affected = false;
			if (positionType == Position.LINK) {
				Id linkId = agentPosition.getPositionId();
				Link link = scenario.getNetwork().getLinks().get(linkId);
				affected = coordAnalyzer.isLinkAffected(link);
			} else if (positionType == Position.FACILITY) {
				Id facilityId = agentPosition.getPositionId();
				Facility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
				affected = coordAnalyzer.isFacilityAffected(facility);
			} else if (positionType == Position.VEHICLE) {
				Id linkId = vehiclesTracker.getVehicleLinkId(agentPosition.getPositionId());
				Link link = scenario.getNetwork().getLinks().get(linkId);
				affected = coordAnalyzer.isLinkAffected(link);
			} else {
				log.warn("Agent's position is undefined! Id: " + agentId);
			}
			
			// apply filter
			if (filterType == FilterType.Affected && !affected) iter.remove();
			else if (filterType == FilterType.NotAffected && affected) iter.remove();
		}
	}
}