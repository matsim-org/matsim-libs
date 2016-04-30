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
import org.matsim.core.network.TimeVariantLinkImpl;
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

	Logger log = Logger.getLogger(getClass());
	private Map<Id<Link>, double[]> capacities = new HashMap<>();
	private Map<Id<Link>, double[]> averageCapacity = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationCapacity = new HashMap<>();
	
	private Map<Id<Link>, double[]> freespeeds = new HashMap<>();
	private Map<Id<Link>, double[]> averageFreespeed = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationFreespeed = new HashMap<>();
	
	private Map<Id<Link>, double[]> lanes = new HashMap<>();
	private Map<Id<Link>, double[]> averageLanes = new HashMap<>();
	private Map<Id<Link>, double[]> standardDeviationLanes = new HashMap<>();

	private int TIMESTEP = 15 * 60;
	
	private int days;
	private int timebins;
	private int binsPerDay;
	
	private final String nceDirectory = "../../../shared-svn/studies/ihab/incidents/analysis/berlin_2016-04-27/networkChangeEvents_test/";
	private final String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";

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
			
			capacities.put(linkId, new double[timebins]);
			averageCapacity.put(linkId, new double[binsPerDay]);
			standardDeviationCapacity.put(linkId, new double[binsPerDay]);
			
			freespeeds.put(linkId, new double[timebins]);
			averageFreespeed.put(linkId, new double[binsPerDay]);
			standardDeviationFreespeed.put(linkId, new double[binsPerDay]);
			
			lanes.put(linkId, new double[timebins]);
			averageLanes.put(linkId, new double[binsPerDay]);
			standardDeviationLanes.put(linkId, new double[binsPerDay]);
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
						TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(linkId);
						this.capacities.get(linkId)[currentTimeBin] = link.getFlowCapacityPerSec(time) * 3600.;
						this.freespeeds.get(linkId)[currentTimeBin] = link.getFreespeed(time) * 3600.;
						this.lanes.get(linkId)[currentTimeBin] = link.getNumberOfLanes(time) * 3600.;
					}

				}
				dayCounter++;
			}
		}
		
		calculateStatistics(scenario0);

		writeCapacities(scenario0, this.freespeeds, "freespeed_all");
		writeCapacities(scenario0, this.capacities, "capacity_all");
		writeCapacities(scenario0, this.lanes, "lanes_all");
		
		writeAnalysisValues(scenario0, this.averageFreespeed, "freespeed_avg");
		writeAnalysisValues(scenario0, this.standardDeviationFreespeed, "freespeed_std");
		
		writeAnalysisValues(scenario0, this.averageCapacity, "capacity_avg");
		writeAnalysisValues(scenario0, this.standardDeviationCapacity, "capacity_std");
		
		writeAnalysisValues(scenario0, this.averageLanes, "lanes_avg");
		writeAnalysisValues(scenario0, this.standardDeviationLanes, "lanes_std");
	}

	private void writeCapacities(Scenario scenario, Map<Id<Link>, double[]> values, String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(nceDirectory + filename + ".csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(nceDirectory + filename + ".csvt");
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		try {
			String l1 = "LinkID;Original Freespeed [m/s];Original Capacity [veh/hour];Original Number of Lanes;Original Length [m]";
			bw.append(l1);
			bwt.append("\"String\";\"Real\";\"Real\";\"Real\";\"Real\"");
			for (int day = 0; day <= days; day++) {
				for (int i = 0; i<24*3600;i=i+TIMESTEP ){
					double time = day*24*3600+i;
					bw.append(";"+Time.writeTime(time));
					bwt.append(";\"Real\"");
				}	
			}
			
			for (Entry<Id<Link>, double[]> e :  values.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString()+";");
				double capacity = scenario.getNetwork().getLinks().get(e.getKey()).getCapacity();
				bw.append(df.format(capacity));
				for (int i=0;i<e.getValue().length;i++){
					bw.append(";"+df.format(e.getValue()[i]));
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
				bw.append(e.getKey().toString()+";");
				double capacity = scenario.getNetwork().getLinks().get(e.getKey()).getCapacity();
				double freespeed = scenario.getNetwork().getLinks().get(e.getKey()).getFreespeed();
				double length = scenario.getNetwork().getLinks().get(e.getKey()).getLength();
				double lanes = scenario.getNetwork().getLinks().get(e.getKey()).getNumberOfLanes();
				bw.append(df.format(freespeed));
				bw.append(df.format(capacity));
				bw.append(df.format(lanes));
				bw.append(df.format(length));
				for (int i=0;i<e.getValue().length;i++){
					bw.append(";"+df.format(e.getValue()[i]));
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
	
	private void calculateStatistics(Scenario scenario){
		
		log.info("Calculating statistics...");
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
				DescriptiveStatistics capacityStats = new DescriptiveStatistics();
				DescriptiveStatistics freeSpeedStats = new DescriptiveStatistics();
				DescriptiveStatistics laneStats = new DescriptiveStatistics();
	
				for (int currentDay = 0; currentDay < days; currentDay++) {
					int currentBin = i + currentDay * binsPerDay;
					capacityStats.addValue(this.capacities.get(linkId)[currentBin]);
					freeSpeedStats.addValue(this.freespeeds.get(linkId)[currentBin]);
					laneStats.addValue(this.lanes.get(linkId)[currentBin]);
				}
				
				this.standardDeviationFreespeed.get(linkId)[i] = freeSpeedStats.getStandardDeviation();
				this.averageFreespeed.get(linkId)[i] = freeSpeedStats.getMean();
				
				this.standardDeviationCapacity.get(linkId)[i] = capacityStats.getStandardDeviation();
				this.averageCapacity.get(linkId)[i] = capacityStats.getMean();
				
				this.standardDeviationLanes.get(linkId)[i] = laneStats.getStandardDeviation();
				this.averageLanes.get(linkId)[i] = laneStats.getMean();
			}
		}
		log.info("Calculating statistics... Done.");
	}

}
