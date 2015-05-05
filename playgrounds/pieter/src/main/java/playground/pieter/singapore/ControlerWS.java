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

package playground.pieter.singapore;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;

import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.*;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.*;

/**
 * A run Controler for a transit router that depends on the travel times and
 * wait times
 * 
 * @author sergioo
 */

public class ControlerWS {

	public static void main(String[] args) {
		Controler controler = new Controler(
				ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		controler.setOverwriteFiles(true);
		// controler.addControlerListener(new
		// CalibrationStatsListener(controler.getEvents(), new String[]{args[1],
		// args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new
		// HashSet<Id>()));
        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(
                controler.getScenario().getPopulation(),
				controler.getScenario().getTransitSchedule(),
				controler.getConfig().travelTimeCalculator()
						.getTraveltimeBinSize(),
				(int) (controler.getConfig().qsim().getEndTime() - controler
						.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(
				controler.getScenario().getTransitSchedule(),
				controler.getConfig().travelTimeCalculator()
						.getTraveltimeBinSize(), (int) (controler.getConfig()
						.qsim().getEndTime() - controler
						.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(
				controler.getScenario().getConfig().planCalcScore(), controler
						.getScenario().getConfig().plansCalcRoute(), controler
						.getScenario().getConfig().transitRouter(), controler
						.getScenario().getConfig().vspExperimental());
		final TransitRouterWSImplFactory factory = new TransitRouterWSImplFactory(
				controler.getScenario(),
				waitTimeCalculator.getWaitTimes(),
				stopStopTimeCalculator.getStopStopTimes());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(factory);
			}
		});
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
