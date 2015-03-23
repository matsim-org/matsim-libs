/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToPickupIdentifierFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.filter.ActivityStartingFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilterFactory;

public class AgentsToPickupIdentifierFactory extends DuringLegIdentifierFactory {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;
	private final InformedAgentsTracker informedAgentsTracker;
	private final DecisionDataProvider decisionDataProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final JointDepartureCoordinator jointDepartureCoordinator;
	
	public AgentsToPickupIdentifierFactory(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, 
			MobsimDataProvider mobsimDataProvider, EarliestLinkExitTimeProvider earliestLinkExitTimeProvider, 
			InformedAgentsTracker informedAgentsTracker, DecisionDataProvider decisionDataProvider,
			JointDepartureOrganizer jointDepartureOrganizer, JointDepartureCoordinator jointDepartureCoordinator, 
			AffectedAgentsFilterFactory affectedAgentsFilterFactory, TransportModeFilterFactory transportModeFilterFactory, 
			InformedAgentsFilterFactory informedAgentsFilterFactory, ActivityStartingFilterFactory activityStartingFilterFactory) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.earliestLinkExitTimeProvider = earliestLinkExitTimeProvider;
		this.informedAgentsTracker = informedAgentsTracker;
		this.decisionDataProvider = decisionDataProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.jointDepartureCoordinator = jointDepartureCoordinator;
		
		// remove agents which are not informed yet
		this.addAgentFilterFactory(informedAgentsFilterFactory);
		
		// remove agents which not walking
		this.addAgentFilterFactory(transportModeFilterFactory);
		
		// remove agents which are not traveling inside the affected area
		this.addAgentFilterFactory(affectedAgentsFilterFactory);
				
		// remove agents that are going to end their leg on their current link
		this.addAgentFilterFactory(activityStartingFilterFactory);
	}

	@Override
	public DuringLegAgentSelector createIdentifier() {
		DuringLegAgentSelector identifier = new AgentsToPickupIdentifier(this.scenario, this.coordAnalyzer.createInstance(), 
				this.vehiclesTracker, this.mobsimDataProvider, this.earliestLinkExitTimeProvider, this.informedAgentsTracker, 
				this.decisionDataProvider, this.jointDepartureOrganizer, this.jointDepartureCoordinator);
		identifier.setAgentSelectorFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
