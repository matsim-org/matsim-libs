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
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;

public class AgentsToPickupIdentifierFactory implements DuringLegIdentifierFactory {

	private final Scenario scenario;
	private final InsecureLegPerformingIdentifier insecureLegPerformingIdentifier;
	
	public AgentsToPickupIdentifierFactory(Scenario scenario, InsecureLegPerformingIdentifier insecureLegPerformingIdentifier) {
		this.scenario = scenario;
		this.insecureLegPerformingIdentifier = insecureLegPerformingIdentifier;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new AgentsToPickupIdentifier(scenario, insecureLegPerformingIdentifier);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
