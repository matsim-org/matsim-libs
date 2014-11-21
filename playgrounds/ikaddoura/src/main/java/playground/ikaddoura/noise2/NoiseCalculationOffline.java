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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
 * (2) Optionally throws noise immission damage events.
 * (2) Writes out some analysis.
 * 
 * @author ikaddoura
 *
 */
public class NoiseCalculationOffline {
	private static final Logger log = Logger.getLogger(NoiseCalculationOffline.class);
	
	private static String runDirectory;
	private static String outputDirectory;
	private static int lastIteration;
	private static double receiverPointGap;
	private static double scaleFactor;
				
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			
			lastIteration = Integer.valueOf(args[1]);
			log.info("last iteration: " + lastIteration);
			
			outputDirectory = args[2];		
			log.info("output directory: " + outputDirectory);
			
			receiverPointGap = Double.valueOf(args[3]);		
			log.info("Receiver point gap: " + receiverPointGap);
			
			scaleFactor = Double.valueOf(args[4]);		
			log.info("Population scale factor: " + scaleFactor);
			
		} else {
			
//			runDirectory = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/";
//			lastIteration = 100;
//			outputDirectory = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/analysis_localRun/";
//			receiverPointGap = 100.;
//			scaleFactor = 10.;
			
			runDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
			lastIteration = 5;
			outputDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
			receiverPointGap = 250.;
			scaleFactor = 1.;
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
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setScaleFactor(scaleFactor);
		
		log.info("Loading scenario...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		EventWriterXML eventWriter = new EventWriterXML(outputDirectory + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
		events.addHandler(eventWriter);
		
		Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints = new HashMap<Id<ReceiverPoint>, ReceiverPoint>();
		
		NoiseInitialization initialization = new NoiseInitialization(scenario, noiseParameters, receiverPoints);
		initialization.initialize();
		NoiseWriter.writeReceiverPoints(receiverPoints, outputFilePath + "/receiverPoints/");
				
		NoiseEmissionHandler noiseEmissionHandler = new NoiseEmissionHandler(scenario, noiseParameters);
		events.addHandler(noiseEmissionHandler);

		PersonActivityHandler personActivityHandler = new PersonActivityHandler(scenario, noiseParameters, initialization, receiverPoints);
		events.addHandler(personActivityHandler);
				
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		log.info("Calculating noise emissions...");
		noiseEmissionHandler.calculateNoiseEmission();
		NoiseWriter.writeNoiseEmissionsStats(noiseEmissionHandler, noiseParameters, outputFilePath + config.controler().getLastIteration() + ".emissionStats.csv");
		NoiseWriter.writeNoiseEmissionStatsPerHour(noiseEmissionHandler, noiseParameters, outputFilePath + config.controler().getLastIteration() + ".emissionStatsPerHour.csv");
		log.info("Calculating noise emissions... Done.");
		
		log.info("Calculating noise immissions...");
		NoiseImmissionCalculation noiseImmission = new NoiseImmissionCalculation(initialization, noiseEmissionHandler, noiseParameters, receiverPoints);
		noiseImmission.setTunnelLinks(null);
		noiseImmission.setNoiseBarrierLinks(null);
		noiseImmission.calculateNoiseImmission();
		NoiseWriter.writeNoiseImmissionStats(receiverPoints, noiseParameters, outputFilePath + config.controler().getLastIteration() + ".immissionStats.csv");
		NoiseWriter.writeNoiseImmissionStatsPerHour(receiverPoints, noiseParameters, outputFilePath + config.controler().getLastIteration() + ".immissionStatsPerHour.csv");
		NoiseWriter.writeNoiseImmissionStatsPerHourShapeFile(receiverPoints, outputFilePath + config.controler().getLastIteration() + ".immissionStatsPerHour.shp");
		log.info("Calculating noise immissions... Done.");
		
		log.info("Calculating each agent's activity durations...");
		personActivityHandler.calculateDurationsOfStay();
		NoiseWriter.writePersonActivityInfoPerHour(receiverPoints, noiseParameters, outputFilePath + config.controler().getLastIteration() + ".personActivityInfoPerHour.csv");
		log.info("Calculating each agent's activity durations... Done.");
		
		log.info("Calculating noise damage costs and throwing noise events...");
		NoiseDamageCalculation noiseDamageCosts = new NoiseDamageCalculation(scenario, events, noiseParameters, noiseEmissionHandler, noiseImmission, receiverPoints);
		noiseDamageCosts.setCollectNoiseEvents(false);
		noiseDamageCosts.calculateNoiseDamageCosts();
		log.info("Calculating noise damage costs and throwing noise events... Done.");

		eventWriter.closeFile();
	}
}
		

