/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionModule;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;

/**
* @author ikaddoura
*/

public class RunBerlinOptAV {

	private static final Logger log = Logger.getLogger(RunBerlinOptAV.class);

	private static String configFile;
	private static String outputDirectory;
	private static boolean internalizeNoise;
	private static boolean internalizeCongestion;
	private static boolean minExtCost;
		
	private static boolean otfvis;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			internalizeCongestion = Boolean.parseBoolean(args[2]);
			log.info("internalizeCongestion: "+ internalizeCongestion);
			
			internalizeNoise = Boolean.parseBoolean(args[3]);
			log.info("internalizeNoise: "+ internalizeNoise);
			
			minExtCost = Boolean.parseBoolean(args[4]);
			log.info("minExtCost: " + minExtCost);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/optAV_test_2taxiTrips/";
			internalizeNoise = false;
			internalizeCongestion = false;
			minExtCost = false;
			otfvis = false;
		}
		
		RunBerlinOptAV runBerlinOptAV = new RunBerlinOptAV();
		runBerlinOptAV.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(
				configFile,
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup(),
				new DecongestionConfigGroup());
		
		config.controler().setOutputDirectory(outputDirectory);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// #############################
		// noise pricing
		// #############################
		
		NoiseConfigGroup noiseParams = (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
		if (internalizeNoise) {
			noiseParams.setInternalizeNoiseDamages(true);
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
		}
		controler.addControlerListener(new NoiseCalculationOnline(new NoiseContext(controler.getScenario())));

		// #############################
		// congestion pricing
		// #############################
					
		if (internalizeCongestion) controler.addOverridingModule(new DecongestionModule(scenario));
		
		// #############################
		// dvrp / taxi
		// #############################

		DvrpConfigGroup.get(controler.getConfig()).setMode(TaxiModule.TAXI_MODE);
        controler.getConfig().addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
        controler.getConfig().checkConsistency();
        
		controler.addOverridingModule(new TaxiOutputModule());
        controler.addOverridingModule(new TaxiModule());
		
        // #############################
        // travel disutility
        // #############################

        final MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory;
        
        if (minExtCost) {
        	dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(null);        	
        
        } else {
        	dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, controler.getConfig().planCalcScore()));
        }
		
        controler.addOverridingModule(new MoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, dvrpTravelDisutilityFactory));
		
		// #############################
		// welfare analysis
		// #############################

		controler.addOverridingModule(new PersonTripAnalysisModule());

		// #############################
		// run
		// #############################
				
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// #############################
		// post processing
		// #############################
			
		String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
			
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParams.getReceiverPointGap());
		processNoiseImmissions.run();
			
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
	
		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(receiverPointsFile);
		merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
		merger.setTimeBinSize(noiseParams.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();	
	}
}

