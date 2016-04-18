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


package playground.polettif.multiModalMap.workbench;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEmptyScenario {

	public static void main(String[] args) {

		String base = "C:/Users/polettif/Desktop/";

		// This creates a default matsim config:
		Config config = ConfigUtils.loadConfig(base+"data/test/config.xml");

		config.setParam("controler", "outputDirectory", base+"output/emptySimulation/");
		config.setParam("network", "inputNetworkFile", base+"output/mtsMapping_zh/network.xml");
		config.setParam("transit", "vehiclesFile", base+"data/test/vehicles.xml");

		config.setParam("transit", "transitScheduleFile", base+"output/mtsMapping_zh/schedule.xml");
		config.setParam("plans", "inputPlansFile", base+"data/test/population.xml");

		config.controler().setLastIteration(1);
//		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
//		config.controler().setOutputDirectory(base+"output/emptySimulation/");
//		config.network().setInputCRS("CH1903_LV03_Plus");
//		config.network().setInputFile(base+"output/mtsMapping_zh/network.xml");
//		config.transit().setTransitScheduleFile(base+"output/mtsMapping_zh/schedule.xml");
//		config.transit().setInputScheduleCRS("CH1903_LV03_Plus");
//		config.transit().setUseTransit(true);
//		config.vspExperimental().setWritingOutputEvents(true);
//		config.plans().setInputFile("data/test/population.xml");


		// This creates a default matsim scenario (which is empty):
		Scenario scenario = ScenarioUtils.createScenario(config) ;

		Controler controler = new Controler( scenario ) ;

		controler.run();

	}

}