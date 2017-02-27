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
import org.matsim.contrib.av.robotaxi.scoring.*;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.*;
import org.matsim.contrib.noise.utils.*;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivityScheduling;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysisRun;

/**
* @author ikaddoura
*/

public class RunBerlinAV {
	
	private static final Logger log = Logger.getLogger(RunBerlinAV.class);

	private static String configFile;
	private static String outputDirectory;
	
	private static boolean analyzeNoise;
	
	private static boolean otfvis;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			analyzeNoise = Boolean.getBoolean(args[2]);
			log.info("analyzeNoise: "+ analyzeNoise);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/baseCase_berlinArea_av-trip-share-0.1_av-20000/";
			analyzeNoise = false;
			otfvis = false;
		}
		
		RunBerlinAV runBerlinAV = new RunBerlinAV();
		runBerlinAV.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config.controler().setOutputDirectory(outputDirectory);
		DvrpConfigGroup.get(config).setMode(TaxiOptimizerModules.TAXI_MODE);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
						
		AgentSpecificActivityScheduling aa = new AgentSpecificActivityScheduling(controler);
		controler = aa.prepareControler(false);
		
		// #############################
		// taxi
		// #############################

		FleetImpl fleet = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleet).readFile(taxiCfg.getTaxisFileUrl(config.getContext()).getFile());
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		controler.addOverridingModule(new TaxiOutputModule());
		controler.addOverridingModule(TaxiOptimizerModules.createDefaultModule(fleet));
        
        final RandomizingTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory = 
        		new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, controler.getConfig().planCalcScore());
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(dvrpTravelDisutilityFactory);
			}
		});

		// #############################
		// run
		// #############################

		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// #############################
		// analysis
		// #############################
		
		String outputDirectory = config.controler().getOutputDirectory();
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		if (analyzeNoise) {
			NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
			noiseParameters.setInternalizeNoiseDamages(false);
			
			NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
			noiseCalculation.run();	
			
			String outputFilePath = outputDirectory + "noise-analysis_it." + scenario.getConfig().controler().getLastIteration() + "/";
			ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
			process.run();
			
			if (noiseParameters.isComputeNoiseDamages()) {
				final String[] labels = { "damages_receiverPoint" };
				final String[] workingDirectories = { outputFilePath + "damages_receiverPoint/" };

				MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
				merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
				merger.setOutputDirectory(outputFilePath);
				merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
				merger.setWorkingDirectory(workingDirectories);
				merger.setLabel(labels);
				merger.run();
			}
			
			PersonTripNoiseAnalysisRun analysis1 = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory(), outputFilePath + controler.getConfig().controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
			analysis1.run();
			
		} else {
			PersonTripNoiseAnalysisRun analysis1 = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory());
			analysis1.run();
		}
	}
}

