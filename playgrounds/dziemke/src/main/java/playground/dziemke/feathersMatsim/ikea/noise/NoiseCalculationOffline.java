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

package playground.dziemke.feathersMatsim.ikea.noise;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.NoiseParameters;
import org.matsim.contrib.noise.NoiseWriter;
import org.matsim.contrib.noise.data.GridParameters;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;

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

	public static void main(String[] args) {

		runDirectory = "C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case1b_fcf_3/";
		outputDirectory = "C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/noiseAnalysis/Case1b_fcf_3_detail";
		receiverPointGap = 5.;
		lastIteration = 20;

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
		config.network().setInputFile(runDirectory + "run0.output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "run0.output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);

		// ################################

		GridParameters gridParameters = new GridParameters();
		gridParameters.setReceiverPointGap(receiverPointGap);

		// Hasselt Coordinates:
//		double xMin = 662600.;
//		double yMin = 5642604.;
//		double xMax = 663569.;
//		double yMax = 5643410.;
		double xMin = 663026.;
		double yMin = 5643274.;
		double xMax = 663481.;
		double yMax = 5643561.;

		//double xMin = 0.;
		//double yMin = 0.;
		//double xMax = 0.;
		//double yMax = 0.;

		gridParameters.setReceiverPointsGridMinX(xMin);
		gridParameters.setReceiverPointsGridMinY(yMin);
		gridParameters.setReceiverPointsGridMaxX(xMax);
		gridParameters.setReceiverPointsGridMaxY(yMax);

		//		 Activity Types
		String[] consideredActivitiesForDamages = {"home","work","n.a.","bringGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"};

		gridParameters.setConsideredActivitiesForSpatialFunctionality(consideredActivitiesForDamages);

		String[] consideredActivitiesForReceiverPointGrid = {"home","work","n.a.","bringGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"};

		gridParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);

		// ################################

		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(2.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(true);
		noiseParameters.setThrowNoiseEventsAffected(true);
		noiseParameters.setThrowNoiseEventsCaused(true);

		//		Set<String> hgvIdPrefixes = new HashSet<String>();
		//		//hgvIdPrefixes.add("lkw");
		//		noiseParameters.setHgvIdPrefixes(hgvIdPrefixes);

		//		Set<String> busIdPrefixes = new HashSet<String>();
		//		busIdPrefixes.add("-B-");
		//		noiseParameters.setBusIdPrefixes(busIdPrefixes);

//			Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
//			tunnelLinkIDs.add(Id.create("48114", Link.class));
//			tunnelLinkIDs.add(Id.create("78107", Link.class));
//			tunnelLinkIDs.add(Id.create("78108", Link.class));
//			tunnelLinkIDs.add(Id.create("81129", Link.class));
//			tunnelLinkIDs.add(Id.create("81130", Link.class));
//			tunnelLinkIDs.add(Id.create("78155", Link.class));
//			noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);


		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);

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
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/run0." + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");

		timeTracker.computeFinalTimeIntervals();

		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter.closeFile();
		}
		log.info("Noise calculation completed.");

		log.info("Processing the noise immissions...");
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", receiverPointGap);
		process.run();

	}
}




