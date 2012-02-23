/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegToMeetingPointReplannerFactory.java
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
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.evacuation.mobsim.HouseholdsTracker;

public class CurrentLegToMeetingPointReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	
	public CurrentLegToMeetingPointReplannerFactory(Scenario scenario,
			AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability, HouseholdsTracker householdsTracker) {
		super(abstractMultithreadedModule, replanningProbability);
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegToMeetingPointReplanner(super.getId(), scenario, householdsTracker);
		super.initNewInstance(replanner);
		return replanner;
	}
}