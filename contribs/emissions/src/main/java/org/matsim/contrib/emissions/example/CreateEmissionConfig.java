/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.emissions.example;

import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;

/**
 * 
 * Creates a config file 
 * with necessary emission input files for the {@link org.matsim.contrib.emissions.utils.EmissionsConfigGroup EmissionsConfigGroup}.
 * 
 * This config file is used by the {@link org.matsim.contrib.emissions.example.RunEmissionToolOfflineExampleV2 OfflineExample} and
 * the {@link org.matsim.contrib.emissions.example.RunEmissionToolOnlineExampleV2 OnlineExample}
 * 
 * @author benjamin, julia
 *
 */
public class CreateEmissionConfig {

		private static final String inputPath = "./test/input/org/matsim/contrib/emissions/";
		private static final String networkFile = //inputPath + 
				"sample_network.xml";
		private static final String plansFile = //inputPath + 
				"sample_population.xml";
		private static final String emissionVehicleFile = //inputPath + 
				"sample_emissionVehicles.xml";
		
		private static final String roadTypeMappingFile = //inputPath +
				"sample_roadTypeMapping.txt";
		
		private static final String averageFleetWarmEmissionFactorsFile = //inputPath +
				"sample_EFA_HOT_vehcat_2005average.txt";
		private static final String averageFleetColdEmissionFactorsFile = //inputPath +
				"sample_EFA_ColdStart_vehcat_2005average.txt";
		
		private static final boolean isUsingDetailedEmissionCalculation = true;
		private static final String detailedWarmEmissionFactorsFile = //inputPath +
				"sample_EFA_HOT_SubSegm_2005detailed.txt";
		private static final String detailedColdEmissionFactorsFile = //inputPath +
			 	"sample_EFA_ColdStart_SubSegm_2005detailed.txt";
		
		private static final String outputPath = "./test/output/";
		private static final String configFilePath = inputPath + "config_v2.xml";
		
		private static final int numberOfIterations = 6;
		
		
		public static void main(String[] args) {
			
			Config config = new Config();
			config.addCoreModules();
			MatsimServices controler = new Controler(config);
			
		// controlerConfigGroup
			ControlerConfigGroup ccg = controler.getConfig().controler();
			ccg.setOutputDirectory(outputPath);
			ccg.setFirstIteration(0);
			ccg.setLastIteration(numberOfIterations-1);
			
		// planCalcScoreConfigGroup
			PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
			ActivityParams homeP = new ActivityParams("home");
			homeP.setTypicalDuration(12 * 3600);
			pcs.addActivityParams(homeP);
			ActivityParams workP = new ActivityParams("work");
			workP.setTypicalDuration(8 * 3600);
			pcs.addActivityParams(workP);

		// strategy
			StrategyConfigGroup scg = controler.getConfig().strategy();
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			scg.addStrategySettings(strategySettings);
			
		// network
			NetworkConfigGroup ncg = controler.getConfig().network();
			ncg.setInputFile(networkFile);
			
		// plans
			PlansConfigGroup pcg = controler.getConfig().plans();
			pcg.setInputFile(plansFile);
			
		// define emission tool input files	
	        EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
	        controler.getConfig().addModule(ecg);
	        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
	        // emission vehicles are now set in the default vehicle container
	        config.vehicles().setVehiclesFile(emissionVehicleFile);

	        ecg.setUsingVehicleTypeIdAsVehicleDescription(false);

	        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
	        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
	        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
	        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
	        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);

	        ecg.setIgnoringEmissionsFromEventsFile(false);
	        ecg.setEmissionCostMultiplicationFactor(1.0);
	        ecg.setConsideringCO2Costs(true);
	        ecg.setEmissionEfficiencyFactor(1.0);
	        
	   // write config     
	        ConfigWriter cw = new ConfigWriter(config);
			cw.write(configFilePath);
			


	}

}
