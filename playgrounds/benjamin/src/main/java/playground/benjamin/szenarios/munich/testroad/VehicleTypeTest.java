/* *********************************************************************** *
 * project: org.matsim.*
 * BkGo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.HouseholdsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.Controler;

/**
 * @author benjamin
 *
 */
public class VehicleTypeTest {

	public static void main(String[] args) {
		String baseDirectory = "../../detailedEval/teststrecke/sim/";
		
		String configFile = baseDirectory + "inputVehicles/config_vehiclesTest.xml";
		
		String outputDirectory = baseDirectory + "outputVehicles";
		
		String networkFile = baseDirectory + "input/network.xml";
		
		String changeEventsInputFile = baseDirectory + "input/capacityChanges.xml";
		
		String plansFile = baseDirectory + "input/20090708_plans.xml.gz";
		
		String householdsFile = baseDirectory + "";
		
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);
		Controler kontrolle = new Controler(config);
		Scenario scenario = kontrolle.getScenario();
		
	// controler settings	
		kontrolle.setOverwriteFiles(true);
		kontrolle.setCreateGraphs(false);
		
	// controlerConfigGroup
		ControlerConfigGroup ccg = kontrolle.getConfig().controler();
		ccg.setOutputDirectory(outputDirectory);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(0);

	// network
		NetworkConfigGroup ncg = kontrolle.getConfig().network();
		ncg.setInputFile(networkFile);
		ncg.setTimeVariantNetwork(true);
		ncg.setChangeEventInputFile(changeEventsInputFile);
		
	// plansCalcScore
		
		
	// plans
		PlansConfigGroup pcg = kontrolle.getConfig().plans();
		pcg.setInputFile(plansFile);
		
	// plansCalcRoute
		
		
	// scenario
		ScenarioConfigGroup scg = kontrolle.getConfig().scenario();
		HouseholdsConfigGroup hcg = kontrolle.getConfig().households();
		scg.setUseVehicles(true);
		scg.setUseHouseholds(false);
		hcg.setInputFile(householdsFile);
		
		kontrolle.addControlerListener(new VehicleControlerListener(scenario));
		
		kontrolle.run();
	}
}
