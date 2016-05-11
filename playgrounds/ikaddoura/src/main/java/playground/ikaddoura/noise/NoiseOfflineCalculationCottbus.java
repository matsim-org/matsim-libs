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

package playground.ikaddoura.noise;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
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
public class NoiseOfflineCalculationCottbus {
	
	private static String runDirectory = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/";
	private static String outputDirectory = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/noise-analysis/";
	private static int lastIteration = 100;
				
	public static void main(String[] args) {
	
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);		
						
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setReceiverPointGap(100.);
		noiseParameters.setTimeBinSizeNoiseComputation(3600.);
		
		String[] consideredActivitiesForDamages = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);

		noiseParameters.setReceiverPointsGridMinX(448292.);
		noiseParameters.setReceiverPointsGridMinY(5729203.);
		noiseParameters.setReceiverPointsGridMaxX(460235.);
		noiseParameters.setReceiverPointsGridMaxY(5738117.);
		
		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(4.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);
		
		Set<String> busIdPrefixes = new HashSet<>();
		busIdPrefixes.add("");
		noiseParameters.setBusIdIdentifierSet(busIdPrefixes);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();
		
		// some processing of the output data
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
		merger.run();
		
		final String[] labels2 = {"consideredAgentUnits"};
		final String[] workingDirectories2 = {outputFilePath + "/consideredAgentUnits/"};

		MergeNoiseCSVFile merger2 = new MergeNoiseCSVFile();
		merger2.setOutputFormat(OutputFormat.xyt1t2t3etc);
		merger2.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger2.setOutputDirectory(outputFilePath);
		merger2.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger2.setWorkingDirectory(workingDirectories2);
		merger2.setLabel(labels2);
		merger2.run();
		
		final String[] labels3 = {"damages_receiverPoint" };
		final String[] workingDirectories3 = {outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger3 = new MergeNoiseCSVFile();
		merger3.setOutputFormat(OutputFormat.xyt1t2t3etc);
		merger3.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger3.setOutputDirectory(outputFilePath);
		merger3.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger3.setWorkingDirectory(workingDirectories3);
		merger3.setLabel(labels3);
		merger3.run();
	}
}