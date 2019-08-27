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

package org.matsim.contrib.noise.examples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculationWithInjection;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * An example how to compute noise levels, damages etc. for a single iteration (= offline noise computation). 
 * 
 * @author ikaddoura
 *
 */
public class NoiseOfflineCalculationExample {
	
	private static String runDirectory = "pathTo/RunDirectory/";
	private static String outputDirectory = "pathTo/analysis-output-directory/";
	private static String runId = "runXYZ";
				
	public static void main(String[] args) {
	
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.controler().setRunId(runId);
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
						
		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config,NoiseConfigGroup.class) ;
		noiseParameters.setReceiverPointGap(12345789.);
		// ...
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculationWithInjection noiseCalculation = new NoiseOfflineCalculationWithInjection(scenario, outputDirectory);
		noiseCalculation.run();
		
		// some processing of the output data
		if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory + "/";
		
		String outputFilePath = outputDirectory + "noise-analysis/";
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
		merger.run();
	}
}
