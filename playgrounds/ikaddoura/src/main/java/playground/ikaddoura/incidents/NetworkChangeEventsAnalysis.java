/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Analyzes network change event files and provides some statistics,
 * e.g. average flow capacity per link and time bin; average freespeed per link and time bin.
 * 
 * @author jbischoff, ikaddoura
 *
 */
public class NetworkChangeEventsAnalysis {
	
	private final String nceDirectory = "../../../shared-svn/studies/ihab/incidents/analysis/berlin_2016-02-11_2016-05-18/networkChangeEventFiles_weekends_holidays/";
	private final String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";
	
	private final int TIMESTEP = 3600 * 3;
	private final int morningTimeInterval_from = TIMESTEP * 2;
	private final int nightTimeInterval_from = TIMESTEP * 0;
	
	private final boolean writeDetailedOutput = true;
	private final boolean analyzeFreespeed = false;
	private final boolean analyzeCapacity = true;
	private final boolean analyzeLanes = false;

	private Map<Id<Link>, double[]> capacities = new HashMap<>();
	private Map<Id<Link>, double[]> averageCapacity = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationCapacity = new HashMap<>();
	
	private Map<Id<Link>, double[]> freespeeds = new HashMap<>();
	private Map<Id<Link>, double[]> averageFreespeed = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationFreespeed = new HashMap<>();
	
	private Map<Id<Link>, double[]> lanes = new HashMap<>();
	private Map<Id<Link>, double[]> averageLanes = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationLanes = new HashMap<>();
	
	private int days;
	private int timebins;
	private int binsPerDay;
	
	Logger log = Logger.getLogger(getClass());

	public static void main(String[] args) {
		NetworkChangeEventsAnalysis cmtt = new NetworkChangeEventsAnalysis();
		cmtt.run();
	}

	public NetworkChangeEventsAnalysis() {
		binsPerDay = (24*3600)/TIMESTEP;
	}

	private void run() {

		File[] fileList = new File(nceDirectory).listFiles();
		
		int fileCounter = 0;
		for (File f : fileList) {
			if (f.getName().endsWith(".xml.gz") && f.getName().startsWith("networkChangeEvents_")) {
				fileCounter++;
			}
		}
				
		this.days = fileCounter;
		timebins = (days * 24 * 3600) / TIMESTEP;
		log.info(timebins + " timebins created for " + days + " days and " + TIMESTEP + "sec time bins.");
		
		Config config0 = ConfigUtils.createConfig();
		config0.network().setInputFile(networkFile);
		Scenario scenario0 = ScenarioUtils.loadScenario(config0);
		
		for (Id<Link> linkId : scenario0.getNetwork().getLinks().keySet()) {
			
			if (analyzeCapacity) {
				capacities.put(linkId, new double[timebins]);
				averageCapacity.put(linkId, new double[binsPerDay]);
				standardDeviationCapacity.put(linkId, new double[binsPerDay]);
			}
			
			if (analyzeFreespeed) {
				freespeeds.put(linkId, new double[timebins]);
				averageFreespeed.put(linkId, new double[binsPerDay]);
				standardDeviationFreespeed.put(linkId, new double[binsPerDay]);
			}
			
			if (analyzeLanes) {
				lanes.put(linkId, new double[timebins]);
				averageLanes.put(linkId, new double[binsPerDay]);
				standardDeviationLanes.put(linkId, new double[binsPerDay]);
			}
		}
		
		int dayCounter = 0;
		for (File f : fileList) {

			if (f.getName().endsWith(".xml.gz") && f.getName().startsWith("networkChangeEvents_")) {
				
				String delimiter1 = "_";
				String delimiter2 = ".";
				String dateString = StringUtils.explode(StringUtils.explode(f.getName(), delimiter1.charAt(0))[1], delimiter2.charAt(0))[0];
				log.info(">>>> Day: " + dateString);
				
				log.info("Loading scenario...");
				
				Config config = ConfigUtils.createConfig();
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(f.toString());
				config.network().setInputFile(networkFile);
				Scenario scenario = ScenarioUtils.loadScenario(config);
								
				log.info("Loading scenario... Done.");
				
				for (int time = 0; time < 24 * 3600; time = time + TIMESTEP) {
					int currentTimeBin = (dayCounter * 24 * 3600 + time) / TIMESTEP;
					if (currentTimeBin >= timebins) {
						log.error("current: " + currentTimeBin + " size " + timebins);
						break;
					}
					log.info("bin no: " + currentTimeBin + " - day: " + dayCounter + " - time: " + Time.writeTime(time));
					
					for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
						Link link = (Link) scenario.getNetwork().getLinks().get(linkId);
//						TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(linkId);
						if (analyzeFreespeed) this.freespeeds.get(linkId)[currentTimeBin] = link.getFreespeed(time);
						if (analyzeCapacity) this.capacities.get(linkId)[currentTimeBin] = link.getFlowCapacityPerSec(time) * 3600.;
						if (analyzeLanes) this.lanes.get(linkId)[currentTimeBin] = link.getNumberOfLanes(time);
					}
				}
				dayCounter++;
			}
		}
		
		calculateStatistics(scenario0);
		
		if (writeDetailedOutput) {
			
			if (analyzeFreespeed) {
				writeDetailedInformation(scenario0, this.freespeeds, "freespeed_all");
				writeDetailedInformation(scenario0, this.freespeeds, "freespeed_" + morningTimeInterval_from, morningTimeInterval_from);
				writeDetailedInformation(scenario0, this.freespeeds, "freespeed_" + nightTimeInterval_from, nightTimeInterval_from);
				
				writeFreeSpeedActualSpeedRatio(scenario0, this.freespeeds, "freespeedActSpeedRatio_" + morningTimeInterval_from, morningTimeInterval_from, "freespeed");
				writeFreeSpeedActualSpeedRatio(scenario0, this.freespeeds, "freespeedActSpeedRatio_" + nightTimeInterval_from, nightTimeInterval_from, "freespeed");
			}

			if (analyzeCapacity) {
				writeDetailedInformation(scenario0, this.capacities, "capacity_all");
				writeDetailedInformation(scenario0, this.capacities, "capacity_" + morningTimeInterval_from, morningTimeInterval_from);
				writeDetailedInformation(scenario0, this.capacities, "capacity_" + nightTimeInterval_from, nightTimeInterval_from);
				
				writeFreeSpeedActualSpeedRatio(scenario0, this.capacities, "capacityRatio_" + morningTimeInterval_from, morningTimeInterval_from, "capacity");
				writeFreeSpeedActualSpeedRatio(scenario0, this.capacities, "capacityRatio_" + nightTimeInterval_from, nightTimeInterval_from, "capacity");
			}
			
			if (analyzeLanes) {
				writeDetailedInformation(scenario0, this.lanes, "lanes_all");
				writeDetailedInformation(scenario0, this.lanes, "lanes_" + morningTimeInterval_from, morningTimeInterval_from);
				writeDetailedInformation(scenario0, this.lanes, "lanes_" + nightTimeInterval_from, nightTimeInterval_from);
				
				writeFreeSpeedActualSpeedRatio(scenario0, this.lanes, "laneRatio_" + morningTimeInterval_from, morningTimeInterval_from, "lanes");
				writeFreeSpeedActualSpeedRatio(scenario0, this.lanes, "laneRatio_" + nightTimeInterval_from, nightTimeInterval_from, "lanes");
			}
		}
		
		if (analyzeFreespeed) { 
			writeAnalysisValues(scenario0, this.averageFreespeed, "freespeed_avg");
			writeAnalysisValues(scenario0, this.standardDeviationFreespeed, "freespeed_std");
		}
		
		if (analyzeCapacity)  {
			writeAnalysisValues(scenario0, this.averageCapacity, "capacity_avg");
			writeAnalysisValues(scenario0, this.standardDeviationCapacity, "capacity_std");
		}
		
		if (analyzeLanes) {
			writeAnalysisValues(scenario0, this.averageLanes, "lanes_avg");
			writeAnalysisValues(scenario0, this.standardDeviationLanes, "lanes_std");
		}
	}

	private void writeDetailedInformation(Scenario scenario, Map<Id<Link>, double[]> values, String filename) {
		writeDetailedInformation(scenario, values, filename, -1);
	}
	
	private void writeDetailedInformation(Scenario scenario, Map<Id<Link>, double[]> values, String filename, int analysisTime) {
		BufferedWriter bw = IOUtils.getBufferedWriter(nceDirectory + filename + ".csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(nceDirectory + filename + ".csvt");
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		try {
			String l1 = "LinkID;Original Freespeed [m/s];Original Capacity [veh/hour];Original Number of Lanes;Original Length [m]";
			bw.append(l1);
			bwt.append("\"String\";\"Real\";\"Real\";\"Real\";\"Real\"");
			for (int day = 0; day < days; day++) {
				for (int i = 0; i<24*3600;i=i+TIMESTEP ){
					if (analysisTime == -1) {
						bw.append(";Day " + day + " Time: "+Time.writeTime(i));
						bwt.append(";\"Real\"");
					} else {
						if (i == analysisTime) {
							bw.append(";Day " + day + " Time: "+Time.writeTime(i));
							bwt.append(";\"Real\"");
						}
					}
				}	
			}
			
			for (Entry<Id<Link>, double[]> e :  values.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString());
				double freespeed = scenario.getNetwork().getLinks().get(e.getKey()).getFreespeed();
				double capacity = scenario.getNetwork().getLinks().get(e.getKey()).getCapacity();
				double length = scenario.getNetwork().getLinks().get(e.getKey()).getLength();
				double lanes = scenario.getNetwork().getLinks().get(e.getKey()).getNumberOfLanes();
				bw.append(";" + df.format(freespeed));
				bw.append(";" + df.format(capacity));
				bw.append(";" + df.format(lanes));
				bw.append(";" + df.format(length));
				
				int time = 0;
				for (int i=0;i<e.getValue().length;i++){
					if (analysisTime == -1) {
						bw.append(";" + df.format(e.getValue()[i]));
					} else {
						
						if (time == analysisTime) {
							bw.append(";" + df.format(e.getValue()[i]));
						}
						
						time = time + TIMESTEP;
						if (time >= 24 * 3600) {
							time = 0;
						}
					}
				}
			}
			bw.flush();
			bw.close();
			bwt.flush();
			bwt.close();
	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeFreeSpeedActualSpeedRatio(Scenario scenario, Map<Id<Link>, double[]> values, String filename, int analysisTime, String analysisParameter) {
		BufferedWriter bw = IOUtils.getBufferedWriter(nceDirectory + filename + ".csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(nceDirectory + filename + ".csvt");
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		try {
			String l1 = "LinkID;Original Freespeed [m/s];Original Capacity [veh/hour];Original Number of Lanes;Original Length [m]";
			bw.append(l1);
			bwt.append("\"String\";\"Real\";\"Real\";\"Real\";\"Real\"");
			for (int day = 0; day < days; day++) {
				for (int i = 0; i<24*3600;i=i+TIMESTEP ){
					if (analysisTime == -1) {
						bw.append(";Day " + day + " Time: "+Time.writeTime(i));
						bwt.append(";\"Real\"");
					} else {
						if (i == analysisTime) {
							bw.append(";Day " + day + " Time: "+Time.writeTime(i));
							bwt.append(";\"Real\"");
						}
					}
				}	
			}
			
			for (Entry<Id<Link>, double[]> e :  values.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString());
				double freespeed = scenario.getNetwork().getLinks().get(e.getKey()).getFreespeed();
				double capacity = scenario.getNetwork().getLinks().get(e.getKey()).getCapacity();
				double length = scenario.getNetwork().getLinks().get(e.getKey()).getLength();
				double lanes = scenario.getNetwork().getLinks().get(e.getKey()).getNumberOfLanes();
				bw.append(";" + df.format(freespeed));
				bw.append(";" + df.format(capacity));
				bw.append(";" + df.format(lanes));
				bw.append(";" + df.format(length));
				
				int time = 0;
				for (int i=0;i<e.getValue().length;i++){
					if (analysisTime == -1) {
						bw.append(";" + df.format(e.getValue()[i]));
					} else {
						
						if (time == analysisTime) {
							if (analysisParameter.equals("freespeed")) {
								bw.append(";" + df.format(e.getValue()[i] / freespeed));
							
							} else if (analysisParameter.equals("capacity")) {
								bw.append(";" + df.format(e.getValue()[i] / capacity));
								
							} else if (analysisParameter.equals("lanes")) {
								bw.append(";" + df.format(e.getValue()[i] / lanes));
							
							} else {
								throw new RuntimeException("Unknown analysis parameter. Aborting...");
							}
						}
						
						time = time + TIMESTEP;
						if (time >= 24 * 3600) {
							time = 0;
						}
					}
				}
			}
			bw.flush();
			bw.close();
			bwt.flush();
			bwt.close();
	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeAnalysisValues(Scenario scenario, Map<Id<Link>, double[]> linkId2analysisValue, String filename) {
		
		log.info("Writing analysis output:" + filename);
		
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		BufferedWriter bw = IOUtils.getBufferedWriter(nceDirectory+filename+".csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(nceDirectory+filename+".csvt");
		try {
			String l1 = "LinkID;Original Freespeed [m/s];Original Capacity [veh/hour];Original Number of Lanes;Original Length [m]";
			bw.append(l1);
			bwt.append("\"String\";\"Real\";\"Real\";\"Real\";\"Real\"");
			
			for (int i = 0; i<24*3600;i=i+TIMESTEP ){
				double time = i;
				bw.append(";"+Time.writeTime(time));
				bwt.append(";\"Real\"");
			}	
			
			for (Entry<Id<Link>, double[]> e :  linkId2analysisValue.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString());
				double freespeed = scenario.getNetwork().getLinks().get(e.getKey()).getFreespeed();
				double capacity = scenario.getNetwork().getLinks().get(e.getKey()).getCapacity();
				double length = scenario.getNetwork().getLinks().get(e.getKey()).getLength();
				double lanes = scenario.getNetwork().getLinks().get(e.getKey()).getNumberOfLanes();
				bw.append(";" + df.format(freespeed));
				bw.append(";" + df.format(capacity));
				bw.append(";" + df.format(lanes));
				bw.append(";" + df.format(length));
				for (int i=0;i<e.getValue().length;i++){
					bw.append(";" + df.format(e.getValue()[i]));
				}
			}
			bw.flush();
			bw.close();
			bwt.flush();
			bwt.close();
	
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done.");
	}
	
	private void calculateStatistics(Scenario scenario){
		
		log.info("Calculating statistics...");
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
				DescriptiveStatistics capacityStats = new DescriptiveStatistics();
				DescriptiveStatistics freeSpeedStats = new DescriptiveStatistics();
				DescriptiveStatistics laneStats = new DescriptiveStatistics();
	
				for (int currentDay = 0; currentDay < days; currentDay++) {
					int currentBin = i + currentDay * binsPerDay;
					if (analyzeFreespeed) freeSpeedStats.addValue(this.freespeeds.get(linkId)[currentBin]);
					if (analyzeCapacity) capacityStats.addValue(this.capacities.get(linkId)[currentBin]);
					if (analyzeLanes) laneStats.addValue(this.lanes.get(linkId)[currentBin]);
				}
				
				if (analyzeFreespeed) {
					this.standardDeviationFreespeed.get(linkId)[i] = freeSpeedStats.getStandardDeviation();
					this.averageFreespeed.get(linkId)[i] = freeSpeedStats.getMean();	
				}
				
				if (analyzeCapacity) {
					this.standardDeviationCapacity.get(linkId)[i] = capacityStats.getStandardDeviation();
					this.averageCapacity.get(linkId)[i] = capacityStats.getMean();
				}
				
				if (analyzeLanes) {
					this.standardDeviationLanes.get(linkId)[i] = laneStats.getStandardDeviation();
					this.averageLanes.get(linkId)[i] = laneStats.getMean();	
				}
			}
		}
		log.info("Calculating statistics... Done.");
	}

}
