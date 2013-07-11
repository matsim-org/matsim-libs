/* *********************************************************************** *
 * project: org.matsim.*
 *CurrentLegInitialReplannerFactory.java
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

import playground.christoph.evacuation.analysis.CoordAnalyzer;

public class CurrentLegInitialReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	
	public CurrentLegInitialReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			CoordAnalyzer coordAnalyzer) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegInitialReplanner(super.getId(), 
				scenario, this.getWithinDayEngine().getInternalInterface(), coordAnalyzer,
				this.getWithinDayEngine().getTripRouterFactory().instantiateAndConfigureTripRouter());
		return replanner;
	}
}