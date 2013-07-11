/* *********************************************************************** *
 * project: org.matsim.*
 * CreateEvacuationPlanReplannerFactory.java
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

package playground.christoph.oldenburg;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.PlanRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

public class CreateEvacuationPlanReplannerFactory extends WithinDayInitialReplannerFactory {

	private Scenario scenario;
	
	public CreateEvacuationPlanReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine) {
		super(withinDayEngine);
		this.scenario = scenario;
	}

	@Override
	public WithinDayInitialReplanner createReplanner() {
		WithinDayInitialReplanner replanner = new CreateEvacuationPlanReplanner(super.getId(), scenario,
				this.getWithinDayEngine().getInternalInterface(),
				new PlanRouter(this.getWithinDayEngine().getTripRouterFactory().instantiateAndConfigureTripRouter()));
		return replanner;
	}

}