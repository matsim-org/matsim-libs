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
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
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

	private static String runDirectory = "/Users/jakob/git/public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/policy/";
	private static String outputDirectory = "/Users/jakob/git/public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/policy/analysis/";
	private static String runId = "berlin-v6.3";

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.global().setCoordinateSystem("EPSG:25832");
		config.controller().setRunId(runId);
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.facilities().setInputFile(runDirectory + runId + ".output_facilities.xml.gz");
		config.controller().setOutputDirectory(runDirectory);

		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config,NoiseConfigGroup.class) ;
		noiseParameters.setReceiverPointGap(10000);
//		noiseParameters.setReceiverPointGap(12345789.);
		// ...

		Scenario scenario = ScenarioUtils.loadScenario(config);

		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
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
