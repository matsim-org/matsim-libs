/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.integrationCNE;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripCongestionNoiseAnalysisRun;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.DecongestionApproach;
import playground.ikaddoura.integrationCNE.CNEIntegration.CongestionTollingApproach;
import playground.ikaddoura.moneyTravelDisutility.data.BerlinAgentFilter;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * 
 * @author ikaddoura
 *
 */

public class CNEBerlin {
	private static final Logger log = Logger.getLogger(CNEBerlin.class);
	
	private final double xMin = 4565039.;
	private final double xMax = 4632739.;
	private final double yMin = 5801108.;
	private final double yMax = 5845708.;
	
	private final Double timeBinSize = 3600.;
	private final int noOfTimeBins = 30;

	private static String outputDirectory;
	private static String configFile;

	private static boolean congestionPricing;
	private static boolean noisePricing;
	private static boolean airPollutionPricing;
	
	private static double sigma;
	
	private static CongestionTollingApproach congestionTollingApproach;
	private static double kP;
		
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			outputDirectory = args[0];
			log.info("Output directory: " + outputDirectory);
			
			configFile = args[1];
			log.info("Config file: " + configFile);
			
			congestionPricing = Boolean.parseBoolean(args[2]);
			log.info("Congestion Pricing: " + congestionPricing);
			
			noisePricing = Boolean.parseBoolean(args[3]);
			log.info("Noise Pricing: " + noisePricing);
			
			airPollutionPricing = Boolean.parseBoolean(args[4]);
			log.info("Air poullution Pricing: " + airPollutionPricing);
			
			sigma = Double.parseDouble(args[5]);
			log.info("Sigma: " + sigma);
			
			String congestionTollingApproachString = args[6];
			
			if (congestionTollingApproachString.equals(CongestionTollingApproach.QBPV3.toString())) {
				congestionTollingApproach = CongestionTollingApproach.QBPV3;
			} else if (congestionTollingApproachString.equals(CongestionTollingApproach.QBPV9.toString())) {
				congestionTollingApproach = CongestionTollingApproach.QBPV9;
			} else if (congestionTollingApproachString.equals(CongestionTollingApproach.DecongestionPID.toString())) {
				congestionTollingApproach = CongestionTollingApproach.DecongestionPID;
			} else {
				throw new RuntimeException("Unknown congestion pricing approach. Aborting...");
			}
			log.info("Congestion Tolling Approach: " + congestionTollingApproach);
			
			kP = Double.parseDouble(args[7]);
			log.info("kP: " + kP);
			
		} else {
			
			outputDirectory = null;
			configFile = "../../../runs-svn/berlin-an/input/config.xml";
			
			congestionPricing = true;
			noisePricing = true;
			airPollutionPricing = true;
			
			sigma = 0.;
			
			congestionTollingApproach = CongestionTollingApproach.DecongestionPID;
			kP = 2 * ( 10 / 3600. );			
		}
				
		CNEBerlin cnControler = new CNEBerlin();
		cnControler.run();
	}

	public void run() {
						
		Config config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup(), new NoiseConfigGroup(), new DecongestionConfigGroup());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		if (outputDirectory != null) {
			controler.getScenario().getConfig().controler().setOutputDirectory(outputDirectory);
		}
		
		// air pollution Berlin settings
		
		int noOfXCells = 677;
		int noOfYCells = 446;
		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
	
		EmissionsConfigGroup emissionsConfigGroup =  (EmissionsConfigGroup) controler.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
		emissionsConfigGroup.setConsideringCO2Costs(true);
	
		// noise Berlin settings
		
		NoiseConfigGroup noiseParameters =  (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);
		
		noiseParameters.setReceiverPointGap(100.);
		
		String[] consideredActivitiesForReceiverPointGrid = {""};
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);
		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxY(yMax);
			
		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga", "leisure", "other", "shop_daily", "shop_other"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
		
		String[] hgvIdPrefixes = { "lkw" };
		noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);
						
		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
				
		noiseParameters.setScaleFactor(10.);
		noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(false);
		
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
		noiseParameters.setTunnelLinkIDsSet(tunnelLinkIDs);
		
		// decongestion pricing Berlin settings
		
		final DecongestionConfigGroup decongestionSettings = (DecongestionConfigGroup) controler.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);
		
		if (congestionTollingApproach.toString().equals(CongestionTollingApproach.DecongestionPID.toString())) {
			
			decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
			decongestionSettings.setKp(kP);
			decongestionSettings.setKi(0.);
			decongestionSettings.setKd(0.);
			
			decongestionSettings.setMsa(true);
			
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setTOLERATED_AVERAGE_DELAY_SEC(30.);

		} else if (congestionTollingApproach.toString().equals(CongestionTollingApproach.DecongestionBangBang.toString())) {

			decongestionSettings.setDecongestionApproach(DecongestionApproach.BangBang);
			decongestionSettings.setINITIAL_TOLL(0.01);
			decongestionSettings.setTOLL_ADJUSTMENT(1.0);
			
			decongestionSettings.setMsa(false);
			
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setTOLERATED_AVERAGE_DELAY_SEC(30.);
			
		} else {
			// for V3, V9 and V10: no additional settings
		}
		
		// CNE Integration
		
		CNEIntegration cne = new CNEIntegration(controler, gt, rgt);
		cne.setCongestionPricing(congestionPricing);
		cne.setNoisePricing(noisePricing);
		cne.setAirPollutionPricing(airPollutionPricing);
		cne.setSigma(sigma);
		cne.setCongestionTollingApproach(congestionTollingApproach);
		cne.setAgentFilter(new BerlinAgentFilter());

		controler = cne.prepareControler();
				
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		
		PersonTripCongestionNoiseAnalysisRun analysis = new PersonTripCongestionNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.run();
		
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(receiverPointsFile);
		merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();
		
		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = controler.getConfig().controler().getOutputDirectory();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			log.info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}
	}
	
}
