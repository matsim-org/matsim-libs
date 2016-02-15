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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.noise2.data.GridParameters;
import playground.ikaddoura.noise2.data.NoiseAllocationApproach;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.handler.LinkSpeedCalculation;
import playground.ikaddoura.noise2.handler.NoiseTimeTracker;
import playground.ikaddoura.noise2.handler.PersonActivityTracker;
import playground.ikaddoura.noise2.utils.MergeNoiseCSVFile;
import playground.ikaddoura.noise2.utils.ProcessNoiseImmissions;
import playground.ikaddoura.noise2.utils.MergeNoiseCSVFile.OutputFormat;

/**
 * (1) Computes noise emissions, immissions, person activities and damages based on a standard events file.
 * (2) Optionally throws noise immission damage events for the causing agent and the affected agent.
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
	private static double timeBinSize;
				
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
			
			timeBinSize = Double.valueOf(args[4]);		
			log.info("Time bin size: " + timeBinSize);

			throw new RuntimeException("Not yet implemented. Aborting...");

			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/";
			receiverPointGap = 25.;
			lastIteration = 100;
			timeBinSize = 900.;
		}
		
		NoiseCalculationOffline noiseCalculation = new NoiseCalculationOffline();
		noiseCalculation.run();
	}

	private void run() {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);
		
		// ################################
		
		GridParameters gridParameters = new GridParameters();
		gridParameters.setReceiverPointGap(receiverPointGap);
		
		// Berlin Coordinates: Area around the city center of Berlin (Tiergarten)
		double xMin = 4590855.;
		double yMin = 5819679.;
		double xMax = 4594202.;
		double yMax = 5821736.;
		
//		// Berlin Coordinates: Area around the Tempelhofer Feld 4591900,5813265 : 4600279,5818768
//		double xMin = 4591900.;
//		double yMin = 5813265.;
//		double xMax = 4600279.;
//		double yMax = 5818768.;
				
//      // Berlin Coordinates: Greater Berlin area
//		double xMin = 4573258.;
//		double yMin = 5801225.;
//		double xMax = 4620323.;
//		double yMax = 5839639.;

//      // Berlin Coordinates: Berlin area
//		double xMin = 4575415.;
//		double yMin = 5809450.;
//		double xMax = 4615918.;
//		double yMax = 5832532.;
		
//      // Berlin Coordinates: Hundekopf
//		double xMin = 4583187.;
//		double yMin = 5813643.;
//		double xMax = 4605520.;
//		double yMax = 5827098.;

//		// Berlin Coordinates: Manteuffelstrasse
//		double xMin = 4595288.82;
//		double yMin = 5817859.97;
//		double xMax = 4598267.52;
//		double yMax = 5820953.98;	
		
		gridParameters.setReceiverPointsGridMinX(xMin);
		gridParameters.setReceiverPointsGridMinY(yMin);
		gridParameters.setReceiverPointsGridMaxX(xMax);
		gridParameters.setReceiverPointsGridMaxY(yMax);
		
//		 Berlin Activity Types
		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
//		String[] consideredActivitiesForDamages = {"home"};
//		String[] consideredActivitiesForDamages = {"work"};
//		String[] consideredActivitiesForDamages = {"educ_primary", "educ_secondary", "educ_higher", "kiga"};
//		String[] consideredActivitiesForDamages = {"leisure"};
//		String[] consideredActivitiesForDamages = {"home", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForSpatialFunctionality(consideredActivitiesForDamages);
		
//		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
//		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga", "leisure"};
//		gridParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);
		
		// ################################
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(10.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);
		
		Set<String> hgvIdPrefixes = new HashSet<String>();
		hgvIdPrefixes.add("lkw");
		noiseParameters.setHgvIdPrefixes(hgvIdPrefixes);
		
//		Set<String> busIdPrefixes = new HashSet<String>();
//		busIdPrefixes.add("-B-");
//		noiseParameters.setBusIdPrefixes(busIdPrefixes);
		
//		 Berlin Tunnel Link IDs
		Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
		tunnelLinkIDs.add(Id.create("108041", Link.class));
		tunnelLinkIDs.add(Id.create("108142", Link.class));
		tunnelLinkIDs.add(Id.create("108970", Link.class));
		tunnelLinkIDs.add(Id.create("109085", Link.class));
		tunnelLinkIDs.add(Id.create("109757", Link.class));
		tunnelLinkIDs.add(Id.create("109919", Link.class));
		tunnelLinkIDs.add(Id.create("110060", Link.class));
		tunnelLinkIDs.add(Id.create("110226", Link.class));
		tunnelLinkIDs.add(Id.create("110164", Link.class));
		tunnelLinkIDs.add(Id.create("110399", Link.class));
		tunnelLinkIDs.add(Id.create("96503", Link.class));
		tunnelLinkIDs.add(Id.create("110389", Link.class));
		tunnelLinkIDs.add(Id.create("110116", Link.class));
		tunnelLinkIDs.add(Id.create("110355", Link.class));
		tunnelLinkIDs.add(Id.create("92604", Link.class));
		tunnelLinkIDs.add(Id.create("92603", Link.class));
		tunnelLinkIDs.add(Id.create("25651", Link.class));
		tunnelLinkIDs.add(Id.create("25654", Link.class));
		tunnelLinkIDs.add(Id.create("112540", Link.class));
		tunnelLinkIDs.add(Id.create("112556", Link.class));
		tunnelLinkIDs.add(Id.create("5052", Link.class));
		tunnelLinkIDs.add(Id.create("5053", Link.class));
		tunnelLinkIDs.add(Id.create("5380", Link.class));
		tunnelLinkIDs.add(Id.create("5381", Link.class));
		tunnelLinkIDs.add(Id.create("106309", Link.class));
		tunnelLinkIDs.add(Id.create("106308", Link.class));
		tunnelLinkIDs.add(Id.create("26103", Link.class));
		tunnelLinkIDs.add(Id.create("26102", Link.class));
		tunnelLinkIDs.add(Id.create("4376", Link.class));
		tunnelLinkIDs.add(Id.create("4377", Link.class));
		tunnelLinkIDs.add(Id.create("106353", Link.class));
		tunnelLinkIDs.add(Id.create("106352", Link.class));
		tunnelLinkIDs.add(Id.create("103793", Link.class));
		tunnelLinkIDs.add(Id.create("103792", Link.class));
		tunnelLinkIDs.add(Id.create("26106", Link.class));
		tunnelLinkIDs.add(Id.create("26107", Link.class));
		tunnelLinkIDs.add(Id.create("4580", Link.class));
		tunnelLinkIDs.add(Id.create("4581", Link.class));
		tunnelLinkIDs.add(Id.create("4988", Link.class));
		tunnelLinkIDs.add(Id.create("4989", Link.class));
		tunnelLinkIDs.add(Id.create("73496", Link.class));
		tunnelLinkIDs.add(Id.create("73497", Link.class));
		noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);
		
		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
		
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);
				
		log.info("Loading scenario...");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
					
		NoiseContext noiseContext = new NoiseContext(scenario, gridParameters, noiseParameters);
		noiseContext.initialize();
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/", false);
				
		EventsManager events = EventsUtils.createEventsManager();

		NoiseTimeTracker timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
		events.addHandler(timeTracker);
		
		if (noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
			LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation(noiseContext);
			events.addHandler(linkSpeedCalculator);	
		}
		
		EventWriterXML eventWriter = null;
		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter = new EventWriterXML(outputFilePath + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
			events.addHandler(eventWriter);	
		}
		
		if (noiseContext.getNoiseParams().isComputePopulationUnits()) {
			PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
			events.addHandler(actTracker);
		}
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		timeTracker.computeFinalTimeIntervals();

		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter.closeFile();
		}
		log.info("Noise calculation completed.");
		
		log.info("Processing the noise immissions...");
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", receiverPointGap);
		process.run();
		
		log.info("Merging other information to one file...");
		
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/" , outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(timeBinSize);
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.setOutputFormat(OutputFormat.xyt);
		merger.setThreshold(-1.);
		merger.run();
	}
}
		

