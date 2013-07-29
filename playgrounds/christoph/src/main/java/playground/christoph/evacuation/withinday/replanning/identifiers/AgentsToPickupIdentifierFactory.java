/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureLegPerformingIdentifierFactory.java
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
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

public class AgentsToPickupIdentifierFactory extends DuringLegIdentifierFactory {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final TravelTime travelTime;
	private final InformedAgentsTracker informedAgentsTracker;
	private final DecisionDataProvider decisionDataProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	public AgentsToPickupIdentifierFactory(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, 
			TravelTime walkTravelTime, InformedAgentsTracker informedAgentsTracker, DecisionDataProvider decisionDataProvider,
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.travelTime = walkTravelTime;
		this.informedAgentsTracker = informedAgentsTracker;
		this.decisionDataProvider = decisionDataProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new AgentsToPickupIdentifier(scenario, coordAnalyzer.createInstance(), 
				vehiclesTracker, travelTime, informedAgentsTracker, decisionDataProvider, jointDepartureOrganizer);
		identifier.setIdentifierFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
