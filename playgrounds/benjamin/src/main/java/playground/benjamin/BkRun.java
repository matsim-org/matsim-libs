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
package playground.benjamin;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author benjamin
 *
 */
public class BkRun {
	
//	static String baseDirectory = "../../detailedEval/teststrecke/sim/";
//	static String configFile = baseDirectory + "inputVehicles/config_vehiclesTest.xml";
//	static String networkFile = baseDirectory + "input/network.xml";
//	static String plansFile = baseDirectory + "input/20090708_plans.xml.gz";

//	static String changeEventsInputFile = baseDirectory + "input/capacityChanges.xml";
//	static String householdsFile = baseDirectory + "";
	
	static String baseDirectory = "../../runs-svn/run981/";
	static String configFile = baseDirectory + "981.output_config.xml.gz";
	static String networkFile = baseDirectory + "981.output_network.xml.gz";
	static String plansFile = baseDirectory + "981.output_plans.xml.gz";
	static String countsFile = baseDirectory + "input/counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml";
	
	static String outputDirectory = baseDirectory + "output/";
	

	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		ConfigReader confReader = new ConfigReader(config);
		confReader.readFile(configFile);
		Controler kontrolle = new Controler(config);
		
	// controler settings	
		kontrolle.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		kontrolle.getConfig().controler().setCreateGraphs(false);

        // controlerConfigGroup
		ControlerConfigGroup ccg = kontrolle.getConfig().controler();
		ccg.setOutputDirectory(outputDirectory);
		ccg.setFirstIteration(1500);
		ccg.setLastIteration(1500);

		CountsConfigGroup countsCg = kontrolle.getConfig().counts();
		countsCg.setInputFile(countsFile);
		
	// network
		NetworkConfigGroup ncg = kontrolle.getConfig().network();
		ncg.setInputFile(networkFile);
	//	ncg.setTimeVariantNetwork(true);
	//	ncg.setChangeEventInputFile(changeEventsInputFile);
		
	// plans
		PlansConfigGroup pcg = kontrolle.getConfig().plans();
		pcg.setInputFile(plansFile);
		
	// scenario
	//	ScenarioConfigGroup scg = kontrolle.getConfig().scenario();
	//	HouseholdsConfigGroup hcg = kontrolle.getConfig().households();
	//	scg.setUseVehicles(false);
	//	scg.setUseHouseholds(false);
	//	hcg.setInputFile(householdsFile);
		
		kontrolle.run();
	}
}