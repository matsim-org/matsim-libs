/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegToSecureFacilityReplannerFactory.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;

import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class CurrentLegToSecureFacilityReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private Scenario scenario;
	
	public CurrentLegToSecureFacilityReplannerFactory(Scenario scenario, AgentCounterI agentCounter, AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability) {
		super(agentCounter, abstractMultithreadedModule, replanningProbability);
		this.scenario = scenario;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegToSecureFacilityReplanner(super.getId(), scenario);
		super.initNewInstance(replanner);
		return replanner;
	}
}