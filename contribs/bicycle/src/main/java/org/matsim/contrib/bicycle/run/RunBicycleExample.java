/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

public class RunBicycleExample {

	public static void main(String[] args) {

//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/boddin/config_bike_boddin.xml", new BikeConfigGroup()); // Boddinstrasse
		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/example/config_bicycle_berlin.xml", new BicycleConfigGroup()); // Berlin


		//		config.controler().setOutputDirectory("../../../runs-svn/berlin-bike/BerlinBike_0804_BVG_15000");
//		
//		config.plans().setInputFile("demand/bvg.run189.10pct.100.plans.selected_bikeonly_1percent_clean.xml.gz" );
//		
//		config.network().setInputFile("network/BerlinBikeNet_MATsim.xml");
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.global().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		
//		calculate customized bike speed per link? makes separate network unnecessary 
//		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.name() );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule());
		controler.run();
	}
}