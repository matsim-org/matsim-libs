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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * (1) Computes noise emissions, immissions, person activities and damages based on a standard events file.
 * (2) Optionally throws noise immission damage events for the causing agent and the affected agent.
 * 
 * @author ikaddoura
 *
 */
public class NoiseOfflineCalculationBerlinDZ {
	private static final Logger log = Logger.getLogger(NoiseOfflineCalculationBerlinDZ.class);
	
	private static String runDirectory;
	private static String outputDirectory;
	private static int lastIteration;
	private static double receiverPointGap;
	private static double timeBinSize;
				
	private static String receiverPointsGridCSVFile;	

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
			
			receiverPointsGridCSVFile = args[5];		
			log.info("receiverPointGridCSVFile: " + receiverPointsGridCSVFile);
			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct/output/r_output_run0_bln_bc/";
			outputDirectory = runDirectory;
			receiverPointGap = 250.;
			lastIteration = 300;
			timeBinSize = 3600.;
//			receiverPointsGridCSVFile = "../../../shared-svn/studies/countries/de/berlin_noise/Fassadenpegel/FP_gesamt_Atom_repaired_heerstrasse.csv";
		}
		
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.vehicles().setVehiclesFile(null);
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);	
		config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
						
		// adjust the default noise parameters
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
		noiseParameters.setReceiverPointGap(receiverPointGap);
		
//		noiseParameters.setReceiverPointsCSVFile(receiverPointsGridCSVFile);
//		noiseParameters.setReceiverPointsCSVFileCoordinateSystem(TransformationFactory.DHDN_SoldnerBerlin);
		
		// Wilmersdorf with motorway: 4589486 , 5816193 : 4590778 , 5817029
//		double xMin = 4589486.;
//		double yMin = 5816193.;
//		double xMax = 4590778.;
//		double yMax = 5817029.;
		
		// somewhere in Schoeneberg: 4591842 5817767 : 4593134 5818603
//		double xMin = 4591842.;
//		double yMin = 5817767.;
//		double xMax = 4593134.;
//		double yMax = 5818603.;
		
		// Berlin Coordinates: Area around the city center of Berlin (Tiergarten)
//		double xMin = 4590855.;
//		double yMin = 5819679.;
//		double xMax = 4594202.;
//		double yMax = 5821736.;
		
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

      // Berlin Coordinates: Berlin area
		double xMin = 4575415.;
		double yMin = 5809450.;
		double xMax = 4615918.;
		double yMax = 5832532.;
		
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
		
		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMaxY(yMax);
		
//		 Berlin Activity Types
		String[] consideredActivitiesForDamages = {"home", "work", "other"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
		
//		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
//		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);
		
		// ################################
		
		noiseParameters.setScaleFactor(100.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(false);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);
		
		String[] hgvIdPrefixes = { "lkw" };
		noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);
		
//		String[] busIdPrefixes = { "-B-" };
//		noiseParameters.setBusIdPrefixesArray(busIdPrefixes);
		
//		 Berlin Tunnel Link IDs
		Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
		tunnelLinkIDs.add(Id.create("42341", Link.class));
		tunnelLinkIDs.add(Id.create("42340", Link.class));
		
		tunnelLinkIDs.add(Id.create("43771", Link.class));
		tunnelLinkIDs.add(Id.create("43770", Link.class));
		
		tunnelLinkIDs.add(Id.create("85818", Link.class));
		tunnelLinkIDs.add(Id.create("85831", Link.class));
		
		tunnelLinkIDs.add(Id.create("84478", Link.class));
		tunnelLinkIDs.add(Id.create("46127", Link.class));
		tunnelLinkIDs.add(Id.create("97243", Link.class));
		tunnelLinkIDs.add(Id.create("88927", Link.class));
		tunnelLinkIDs.add(Id.create("106000", Link.class));
		tunnelLinkIDs.add(Id.create("34744", Link.class));
		tunnelLinkIDs.add(Id.create("84490", Link.class));
		tunnelLinkIDs.add(Id.create("84484", Link.class));
		
		tunnelLinkIDs.add(Id.create("128294", Link.class));
		tunnelLinkIDs.add(Id.create("128314", Link.class));
		
		tunnelLinkIDs.add(Id.create("87838", Link.class));
		tunnelLinkIDs.add(Id.create("104345", Link.class));
		tunnelLinkIDs.add(Id.create("104366", Link.class));
		tunnelLinkIDs.add(Id.create("87837", Link.class));
		
		tunnelLinkIDs.add(Id.create("107405", Link.class));
		tunnelLinkIDs.add(Id.create("93049", Link.class));
		tunnelLinkIDs.add(Id.create("90684", Link.class));
		tunnelLinkIDs.add(Id.create("93050", Link.class));
		tunnelLinkIDs.add(Id.create("98819", Link.class));
		tunnelLinkIDs.add(Id.create("107382", Link.class));
		tunnelLinkIDs.add(Id.create("98825", Link.class));
		tunnelLinkIDs.add(Id.create("107402", Link.class));
		tunnelLinkIDs.add(Id.create("8029", Link.class));
		tunnelLinkIDs.add(Id.create("60859", Link.class));
		tunnelLinkIDs.add(Id.create("60858", Link.class));
		tunnelLinkIDs.add(Id.create("107401", Link.class));
		tunnelLinkIDs.add(Id.create("90687", Link.class));
		tunnelLinkIDs.add(Id.create("8029", Link.class));
		
		tunnelLinkIDs.add(Id.create("72810", Link.class));
		tunnelLinkIDs.add(Id.create("80104", Link.class));
		tunnelLinkIDs.add(Id.create("72804", Link.class));
		tunnelLinkIDs.add(Id.create("73609", Link.class));
		tunnelLinkIDs.add(Id.create("13366", Link.class));
		tunnelLinkIDs.add(Id.create("80101", Link.class));
		tunnelLinkIDs.add(Id.create("80103", Link.class));
		tunnelLinkIDs.add(Id.create("38562", Link.class));
		tunnelLinkIDs.add(Id.create("38563", Link.class));
		tunnelLinkIDs.add(Id.create("80100", Link.class));
		tunnelLinkIDs.add(Id.create("13367", Link.class));
		tunnelLinkIDs.add(Id.create("72801", Link.class));
	
		tunnelLinkIDs.add(Id.create("131154", Link.class));
		tunnelLinkIDs.add(Id.create("131159", Link.class));
		
		tunnelLinkIDs.add(Id.create("123820", Link.class));
		tunnelLinkIDs.add(Id.create("123818", Link.class));
		
		tunnelLinkIDs.add(Id.create("123818", Link.class));
		tunnelLinkIDs.add(Id.create("76986", Link.class));
		
		tunnelLinkIDs.add(Id.create("15488", Link.class));
		tunnelLinkIDs.add(Id.create("96568", Link.class));
		tunnelLinkIDs.add(Id.create("15490", Link.class));
		
		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
		
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();	
		
		// some processing of the output data
		String outputFilePath = outputDirectory + "noise-analysis_it." + scenario.getConfig().controler().getLastIteration() + "/";
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();
				
//		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
//		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/" , outputFilePath + "/damages_receiverPoint/" };

		final String[] labels = { "consideredAgentUnits" };
		final String[] workingDirectories = { outputFilePath + "consideredAgentUnits/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setOutputFormat(OutputFormat.xyt1t2t3etc);
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();
	}
}
		

