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

package playground.ikaddoura.internalizationCar;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.extCost.ExtCostEventHandler;
import playground.ikaddoura.analysis.extCost.TripInfoWriter;
import playground.ikaddoura.analysis.shapes.IKGISAnalyzer;

/**
 * (1) Computes marginal congestion events based on a standard events file.
 * (2) Computes agent money events based on these marginal congestion events.
 * (3) Does some spatial, person- and trip-based analysis
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionEventsWriter {
	private static final Logger log = Logger.getLogger(MarginalCongestionEventsWriter.class);
	
	static String runDirectory;
	
	static String shapeFileZones;
	static String homeActivity;
	static String workActivity;
	
	// the number of persons a single agent represents
	static int scalingFactor;
			
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			runDirectory = args[0];		
			log.info("runDirectory: " + runDirectory);
			
			shapeFileZones = args[1];		
			log.info("shapeFileZones: " + shapeFileZones);
			
			homeActivity = args[2];		
			log.info("homeActivity: " + homeActivity);
			
			workActivity = args[3];		
			log.info("workActivity: " + workActivity);
			
			scalingFactor = Integer.valueOf(args[4]);		
			log.info("scalingFactor: " + scalingFactor);
			
		} else {
			
			runDirectory = "../../runs-svn/berlin_internalizationCar2/output/baseCase_2/";
			shapeFileZones = "../../shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_1500/berlin_grid_1500.shp";
			homeActivity = "home";
			workActivity = "work";
			scalingFactor = 10;
		}
		
		MarginalCongestionEventsWriter congestionEventsWriter = new MarginalCongestionEventsWriter();
		congestionEventsWriter.run();
	}

	private void run() {
	
		log.info("Loading scenario...");
		Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml.gz");
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
//		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.plans().setInputFile(null);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputDirectory = runDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputDirectory);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
//		EventWriterXML eventWriter = new EventWriterXML(outputDirectory + config.controler().getLastIteration() + ".events_ExternalCongestionCost_Offline.xml.gz");
		MarginalCongestionHandlerImplV3 congestionHandler = new MarginalCongestionHandlerImplV3(events, scenario);
		MarginalCostPricingCarHandler marginalCostTollHandler = new MarginalCostPricingCarHandler(events, scenario);

		TollHandler tollHandler = new TollHandler(scenario);
		ExtCostEventHandler extCostHandler = new ExtCostEventHandler(scenario, false);
		
//		events.addHandler(eventWriter);
		events.addHandler(congestionHandler);
		events.addHandler(marginalCostTollHandler);
		
		events.addHandler(tollHandler);
		events.addHandler(extCostHandler);
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");

//		eventWriter.closeFile();
		
		congestionHandler.writeCongestionStats(outputDirectory + config.controler().getLastIteration() + ".congestionStats_Offline.csv");
		tollHandler.writeTollStats(outputDirectory + config.controler().getLastIteration() + ".tollStats_Offline.csv");

		TripInfoWriter writer = new TripInfoWriter(extCostHandler, outputDirectory);
		writer.writeDetailedResults(TransportMode.car);
		writer.writeAvgTollPerTimeBin(TransportMode.car);
		writer.writeAvgTollPerDistance(TransportMode.car);
		writer.writeAffectedAgentId2totalAmount();
		writer.writeCausingAgentId2totalAmount();
		
		// spatial analysis
		Map<Id, Double> causingAgentId2amountSum = extCostHandler.getCausingAgentId2amountSumAllAgents();
		Map<Id, Double> affectedAgentId2amountSum = extCostHandler.getAffectedAgentId2amountSumAllAgents();
		
		log.info("Analyzing zones...");
		IKGISAnalyzer gisAnalysis = new IKGISAnalyzer(shapeFileZones, scalingFactor, homeActivity, workActivity);
		gisAnalysis.analyzeZones(scenario, runDirectory, causingAgentId2amountSum, affectedAgentId2amountSum);
		log.info("Analyzing zones... Done.");
	}
			 
}
		

