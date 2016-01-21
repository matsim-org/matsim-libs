/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxi.usability.ConfigBasedTaxiLaunchUtils;
import playground.jbischoff.taxi.usability.TaxiConfigGroup;

/**
 * @author  jbischoff
 *
 */
public class AVLauncher {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/config.xml");
		
		TaxiConfigGroup tcg = new TaxiConfigGroup();
		tcg.addParam("vehiclesFile", "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/v48/taxi_vehicles_250000.xml.gz");
		tcg.addParam("ranksFile", "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/ranks.xml");
		tcg.addParam("outputDir", config.controler().getOutputDirectory()+"/taxi");
		tcg.addParam("algorithm", "dummy");
		tcg.addParam("nearestVehicleLimit", "20");
		tcg.addParam("nearestRequestLimit", "20");
		tcg.addParam("pickupDuration", "60");
		tcg.addParam("dropOffDuration", "60");
		config.addModule(tcg);
		config.global().setNumberOfThreads(16);
		config.qsim().setNumberOfThreads(16);
		
		
		
		
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
	
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new ConfigBasedTaxiLaunchUtils(controler).initiateTaxis();
		controler.run();
	}

}
