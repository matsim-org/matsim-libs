/* *********************************************************************** *
 * project: org.matsim.*
 * WriteTransitRouterNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

public class WriteTransitRouterNetwork {

	/**
	 * Expect two input strings:
	 * <li>
	 * 	<ul>config file</ul>
	 * 	<ul>output file</ul>
	 * </li>
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) return;
		
		Config config = ConfigUtils.loadConfig(args[0]);
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.network().setInputFile(null);
		config.scenario().setUseTransit(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new WriteTransitRouterNetwork().writeTransitRouterNetwork(scenario, args[1]);
	}
	
	public void writeTransitRouterNetwork(Scenario scenario, String outputFile) {
		
		Config config = scenario.getConfig();
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		TransitRouterNetwork network = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), 
				transitRouterConfig.getBeelineWalkConnectionDistance());
		
		new TransitRouterNetworkWriter(network).write(outputFile);
	}
}
