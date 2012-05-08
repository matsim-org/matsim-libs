/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentActivityToMeetingPointReplanner.java
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
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeKTIFactory;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;

public class CurrentActivityToMeetingPointReplannerFactory extends WithinDayDuringActivityReplannerFactory {

	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker; 
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final PTTravelTimeKTIFactory ptTravelTimeFactory;
	
	public CurrentActivityToMeetingPointReplannerFactory(Scenario scenario, 
			ReplanningManager replanningManager, AbstractMultithreadedModule abstractMultithreadedModule,
			double replanningProbability, HouseholdsTracker householdsTracker, ModeAvailabilityChecker modeAvailabilityChecker,
			PTTravelTimeKTIFactory ptTravelTimeFactory) {
		super(replanningManager, abstractMultithreadedModule, replanningProbability);
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.ptTravelTimeFactory = ptTravelTimeFactory;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new CurrentActivityToMeetingPointReplanner(super.getId(), scenario,
				this.getReplanningManager().getInternalInterface(), householdsTracker, 
				modeAvailabilityChecker.createInstance(), ptTravelTimeFactory.createTravelTime());
		super.initNewInstance(replanner);
		return replanner;
	}

}