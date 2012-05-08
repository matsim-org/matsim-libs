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
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeKTIFactory;

public class EndActivityAndEvacuateReplannerFactory extends WithinDayDuringActivityReplannerFactory {

	private Scenario scenario;
	private PTTravelTimeKTIFactory ptTravelTimeFactory;
	
	public EndActivityAndEvacuateReplannerFactory(Scenario scenario, ReplanningManager replanningManager,
			AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability,
			PTTravelTimeKTIFactory ptTravelTimeFactory) {
		super(replanningManager, abstractMultithreadedModule, replanningProbability);
		this.scenario = scenario;
		this.ptTravelTimeFactory = ptTravelTimeFactory;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new EndActivityAndEvacuateReplanner(super.getId(), 
				scenario, this.getReplanningManager().getInternalInterface(), ptTravelTimeFactory.createTravelTime());
		super.initNewInstance(replanner);
		return replanner;
	}

}