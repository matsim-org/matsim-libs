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

package playground.ikaddoura.noise2;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * (1) Computes noise emissions and immissions based on a standard events file.
 * (2) Optionally throws noise immission events.
 * (2) Writes out some analysis.
 * 
 * @author ikaddoura
 *
 */
public class NoiseCalculationOffline {
	private static final Logger log = Logger.getLogger(NoiseCalculationOffline.class);
	
	static String runDirectory;
	static String outputDirectory;
	static int lastIteration;
				
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			lastIteration = Integer.valueOf(args[1]);
			log.info("last iteration: " + lastIteration);
			outputDirectory = args[2];		
			log.info("output directory: " + outputDirectory);
			
		} else {
//			runDirectory = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/";
//			lastIteration = 100;
			runDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
			lastIteration = 5;
			outputDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
		}
		
		NoiseCalculationOffline noiseCalculation = new NoiseCalculationOffline();
		noiseCalculation.run();
	}

	private void run() {
	
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);
		// Hopefully this is all information required from the config file... Alternatively, read in the output config.
		
		log.info("Loading scenario...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
//		EventWriterXML eventWriter = new EventWriterXML(outputDirectory + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
//		events.addHandler(eventWriter);
		
		NoiseSpatialInfo spatialInfo = new NoiseSpatialInfo(scenario);
		spatialInfo.setActivityCoords();
		spatialInfo.setReceiverPoints();
		spatialInfo.setActivityCoord2NearestReceiverPointId();
		spatialInfo.setRelevantLinkIds();
				
		NoiseEmissionHandler noiseEmissionHandler = new NoiseEmissionHandler(scenario);
		noiseEmissionHandler.setHdvVehicles(null);
		events.addHandler(noiseEmissionHandler);

		PersonActivityHandler personActivityTracker = new PersonActivityHandler(scenario, spatialInfo);
		events.addHandler(personActivityTracker);
				
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		log.info("Calculating noise emission...");
		noiseEmissionHandler.calculateNoiseEmission();
		noiseEmissionHandler.writeNoiseEmissionStats(outputFilePath + config.controler().getLastIteration() + ".emissionStats.csv");
		noiseEmissionHandler.writeNoiseEmissionStatsPerHour(outputFilePath + config.controler().getLastIteration() + ".emissionStatsPerHour.csv");
		log.info("Calculating noise emission... Done.");
		
		log.info("Calculating each agent's activity durations...");
		personActivityTracker.calculateDurationOfStay();
		log.info("Calculating each agent's activity durations... Done.");
		
		log.info("Calculating noise immission...");
		NoiseImmission noiseImmission = new NoiseImmission(scenario, events, spatialInfo, NoiseConfigParameters.getAnnualCostRate(), noiseEmissionHandler, personActivityTracker);
		noiseImmission.setTunnelLinks(null);
		noiseImmission.setNoiseBarrierLinks(null);
		noiseImmission.calculateNoiseImmission();
		noiseImmission.writeNoiseImmissionStats(outputFilePath + config.controler().getLastIteration() + ".immissionStats.csv");
		noiseImmission.writeNoiseImmissionStatsPerHour(outputFilePath + config.controler().getLastIteration() + ".immissionStatsPerHour.csv");
		log.info("Calculating noise immission... Done.");
				
//		eventWriter.closeFile();
	}
}
		

