/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.crossings.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSmallScenario {

	public static void main(String[] args) {
		String base = "C:/Users/polettif/Desktop/crossings/";
		String scenarioName = "small";

		String inputBase = base + "input/" + scenarioName + "/";

		// run 1 iteration of scenario
		Config config = ConfigUtils.loadConfig(inputBase+"config.xml");
		config.setParam("controler", "outputDirectory", 		base+"output/"+scenarioName+"/");
		config.setParam("network", 	"inputNetworkFile", 		inputBase+"network.xml");
		config.setParam("plans", 	"inputPlansFile", 			inputBase+"population.xml");
		config.setParam("network", "inputChangeEventsFile", inputBase + "networkChangeEvents.xml");
//		config.network().setTimeVariantNetwork(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	}
}