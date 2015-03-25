/* *********************************************************************** *
 * project: org.matsim.*
 * PickupAgentReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifier;

public class PickupAgentReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final AgentsToPickupIdentifier agentsToPickupIdentifier;
	
	public PickupAgentReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine, AgentsToPickupIdentifier agentsToPickupIdentifier) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.agentsToPickupIdentifier = agentsToPickupIdentifier;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new PickupAgentReplanner(super.getId(), scenario,
				this.getWithinDayEngine().getActivityRescheduler(), this.agentsToPickupIdentifier);
		return replanner;
	}
}