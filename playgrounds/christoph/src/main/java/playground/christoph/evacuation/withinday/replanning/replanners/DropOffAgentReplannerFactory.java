/* *********************************************************************** *
 * project: org.matsim.*
 * DropOffAgentReplannerFactory.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class DropOffAgentReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	
	public DropOffAgentReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine) {
		super(withinDayEngine);
		this.scenario = scenario;

	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new DropOffAgentReplanner(super.getId(), scenario,
				this.getWithinDayEngine().getInternalInterface(),
				this.getWithinDayEngine().getTripRouterFactory().instantiateAndConfigureTripRouter());
		return replanner;
	}
}