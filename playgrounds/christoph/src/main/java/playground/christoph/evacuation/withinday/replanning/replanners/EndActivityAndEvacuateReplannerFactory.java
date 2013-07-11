/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplannerFactory.java
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
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTime;

public class EndActivityAndEvacuateReplannerFactory extends WithinDayDuringActivityReplannerFactory {

	private Scenario scenario;
	private SwissPTTravelTime ptTravelTime;
	
	public EndActivityAndEvacuateReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			SwissPTTravelTime ptTravelTime) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.ptTravelTime = ptTravelTime;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new EndActivityAndEvacuateReplanner(super.getId(), 
				scenario, this.getWithinDayEngine().getInternalInterface(), ptTravelTime,
				this.getWithinDayEngine().getTripRouterFactory().instantiateAndConfigureTripRouter());
		return replanner;
	}

}