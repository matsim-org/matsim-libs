/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import playground.wrashid.PSF.ParametersPSF;

public class TestConfig4 extends MatsimTestCase {

	public void testConfig(){
		String basePathOfTestData=getPackageInputDirectory();
		String networkFile = "test/scenarios/berlin/network.xml.gz";

		String configFilePath=basePathOfTestData + "config4.xml";
		Config config = loadConfig(configFilePath);
		ConfigUtils.addOrGetModule(config, ParametersPSF.PSF_MODULE, ParametersPSF.class);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(basePathOfTestData + "plans2.xml");

		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);

		// start simulation run
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        new OptimizedChargerTestGeneral(controler).optimizedChargerTest();
	}

}
