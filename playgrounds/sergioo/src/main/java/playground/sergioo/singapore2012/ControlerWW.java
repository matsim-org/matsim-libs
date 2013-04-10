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

package playground.sergioo.singapore2012;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.pt.router.TransitRouterConfig;

import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWWImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeCalculator;


/**
 * A run Controler for a transit router that depends on the travel times and wait times
 * 
 * @author sergioo
 */

public class ControlerWW {

	public static void main(String[] args) {
		Controler controler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		controler.setOverwriteFiles(true);
		//controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), new String[]{args[1], args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id>()));
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(controler.getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().getQSimConfigGroup().getEndTime()-controler.getConfig().getQSimConfigGroup().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(controler.getScenario().getConfig().planCalcScore(),
				controler.getScenario().getConfig().plansCalcRoute(), controler.getScenario().getConfig().transitRouter(),
				controler.getScenario().getConfig().vspExperimental());
		TransitRouterWWImplFactory factory = new TransitRouterWWImplFactory(transitRouterConfig, controler, waitTimeCalculator.getWaitTimes());
		controler.addControlerListener(factory);
		controler.setTransitRouterFactory(factory);
		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.run();
	}
	
}
