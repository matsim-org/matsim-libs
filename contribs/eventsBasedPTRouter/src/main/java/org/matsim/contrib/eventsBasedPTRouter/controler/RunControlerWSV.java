/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package org.matsim.contrib.eventsBasedPTRouter.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSVFactory;
import org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy.VehicleOccupancyCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;


/**
 * A run Controler for a transit router that depends on the travel times and wait times
 * 
 * @author sergioo
 */

public class RunControlerWSV {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
		final Controler controler = new Controler(scenario);
		final WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		final StopStopTimeCalculatorImpl stopStopTimeCalculator = new StopStopTimeCalculatorImpl(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		final VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(controler.getScenario().getTransitSchedule(), ((MutableScenario)controler.getScenario()).getTransitVehicles(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(vehicleOccupancyCalculator);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding("pt").toProvider(new TransitRouterEventsWSVFactory(controler.getScenario(), waitTimeCalculator.get(), stopStopTimeCalculator.get(), vehicleOccupancyCalculator.getVehicleOccupancy()));
			}
		});
		
		// yyyyyy note that in the above script only the router is modified, but not the scoring.  With standard matsim, a slower bu
		// less crowded pt route will only be accepted by the agent when the faster but more crowded option was never presented 
		// to the agent.  (Alternatively, e.g. with the Singapore scenario, there may be boarding denials, in which case 
		// routes that avoid crowded sections may also be beneficial.)  kai, jul'15
		
		controler.run();
	}
	
}
