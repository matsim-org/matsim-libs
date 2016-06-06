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

package playground.dziemke.other;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * (1) Computes noise emissions, immissions, person activities and damages based on a standard events file.
 * (2) Optionally throws noise immission damage events for the causing agent and the affected agent.
 * 
 * @author ikaddoura
 *
 */
public class NoiseCalculationOfflineMaryland {
	private static final Logger log = Logger.getLogger(NoiseCalculationOfflineMaryland.class);

	private static String runDirectory;
	private static String outputDirectory;
	private static int lastIteration;
	private static double receiverPointGap;

	public static void main(String[] args) {

		runDirectory = "../../../runs-svn/silo/maryland/run_09/matsim/year_2001/";
		outputDirectory = "../../../runs-svn/silo/maryland/run_09/matsim/year_2001/noise_dc/";
		receiverPointGap = 100.;
		lastIteration = 20;

		NoiseCalculationOfflineMaryland noiseCalculation = new NoiseCalculationOfflineMaryland();
		noiseCalculation.run();
	}

	private void run() {

		

		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "year_2001.output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "year_2001.output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);

		// ################################
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");

		noiseParameters.setReceiverPointGap(receiverPointGap);

		// Annapolis coordinates
//		double xMin = 361000.;
//		double yMin = 4312000.;
//		double xMax = 373000.;
//		double yMax = 4320000.;
		
		// DC coordinates
		double xMin = 305000.;
		double yMin = 4295000.;
		double xMax = 342000.;
		double yMax = 4323000.;

		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMaxY(yMax);

		//		 Activity Types
		String[] consideredActivitiesForDamages = {"home","work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);

		String[] consideredActivitiesForReceiverPointGrid = {"home","work"};
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);

		// ################################

		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(100.); // 100 for 1% sample
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false); // only makes sense for online runs
		noiseParameters.setComputeCausingAgents(false); // only needed when doing internalization
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);

		// trucks and buses; trucks are very relevant for noise; not using them leads to underestimation
		//		Set<String> hgvIdPrefixes = new HashSet<String>();
		//		//hgvIdPrefixes.add("lkw");
		//		noiseParameters.setHgvIdPrefixes(hgvIdPrefixes);

		//		Set<String> busIdPrefixes = new HashSet<String>();
		//		busIdPrefixes.add("-B-");
		//		noiseParameters.setBusIdPrefixes(busIdPrefixes);

//			Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
//			tunnelLinkIDs.add(Id.create("48114", Link.class));
//			noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);
		// or specify via csv file

		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);

		log.info("Loading scenario...");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();
		
		String outputFilePath = outputDirectory + "analysis_it." + scenario.getConfig().controler().getLastIteration() + "/";
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();
				
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/" , outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.setOutputFormat(OutputFormat.xyt);
		merger.setThreshold(1.); // Kai uses "1" here; before I had "-1" here
		merger.run();
	}
}