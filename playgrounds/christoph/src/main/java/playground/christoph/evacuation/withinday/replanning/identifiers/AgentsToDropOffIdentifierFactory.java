/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToDropOffIdentifierFactory.java
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
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

public class AgentsToDropOffIdentifierFactory extends DuringLegIdentifierFactory {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	public AgentsToDropOffIdentifierFactory(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker,
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new AgentsToDropOffIdentifier(scenario, coordAnalyzer.createInstance(), 
				vehiclesTracker, jointDepartureOrganizer);
		identifier.setIdentifierFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
