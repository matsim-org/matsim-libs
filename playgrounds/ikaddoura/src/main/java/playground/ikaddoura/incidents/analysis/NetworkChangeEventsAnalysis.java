/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Analyzes multiple network change events files and writes somes statistics into a csv file.
 * TODO: add Min, Max, Std
 * TODO: exclude weekend days
 * 
 * @author ikaddoura
 */

public class NetworkChangeEventsAnalysis {
	private final Logger log = Logger.getLogger(NetworkChangeEventsAnalysis.class);
	
	private String nceDirectory = "../../../shared-svn/studies/ihab/incidents/analysis/berlin_2016-04-27/networkChangeEvents/";
	private String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";

	private Map<Id<Link>, List<DayInfo>> linkId2dayInfo = new HashMap<>();
	
	private final double timeBinSize = 900.;
	
	public static void main(String[] args) throws IOException {
		
		NetworkChangeEventsAnalysis nceAnalysis = new NetworkChangeEventsAnalysis();
		nceAnalysis.run();		
	}
	
	private void run() throws IOException {

		analyzeNCEFiles();
		statistics();
	}
	
	private void write(String outputFile, Map<Id<Link>, Map<Integer, DescriptiveStatistics>> linkId2timeBin2capacityStatistics) throws IOException {
		
		log.info("Writing statistics to " + outputFile + "...");
		
		log.info("Loading scenario...");
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		log.info("Loading scenario... Done.");
		
		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile)) ) {
			
			bw.write("Link ID;Original Capacity [veh/hour]");
			for (double time = timeBinSize; time <= 24 * 3600; time = time + timeBinSize) {
				bw.write("; Capacity [veh/hour] Time: " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS));
			}
			bw.newLine();
			
			for (Id<Link> id : linkId2timeBin2capacityStatistics.keySet()) {
				bw.write(id.toString());
				bw.write("; " + scenario.getNetwork().getLinks().get(id).getCapacity());
				
				for (Integer time : linkId2timeBin2capacityStatistics.get(id).keySet()) {
					bw.write(";" + linkId2timeBin2capacityStatistics.get(id).get(time).getMean());
				}
				bw.newLine();
			}
			log.info("Statistics written to " + outputFile);
			bw.close();
		}
		
	}

	private void statistics() throws IOException {
		
		log.info("+++++++ Running statistical analysis +++++++ ");
		
		Map<Id<Link>, Map<Integer, DescriptiveStatistics>> linkId2timeBin2capacityStatistics = new HashMap<>();
		
		int counter = 0;
		
		for (Id<Link> id : this.linkId2dayInfo.keySet()) {
			Map<Integer, DescriptiveStatistics> time2capacityStatistics = new HashMap<>();

			for (DayInfo dayInfo : this.linkId2dayInfo.get(id)) {
				
				for (Integer time : dayInfo.getTimeBin2info().keySet()) {
					
					if (time2capacityStatistics.containsKey(time)) {
						time2capacityStatistics.get(time).addValue(dayInfo.getTimeBin2info().get(time).getCapacity());
					
					} else {
						DescriptiveStatistics capacityStatistics = new DescriptiveStatistics();
						capacityStatistics.addValue(dayInfo.getTimeBin2info().get(time).getCapacity());
						time2capacityStatistics.put(time, capacityStatistics);
					}
				}				
			}	
			linkId2timeBin2capacityStatistics.put(id, time2capacityStatistics);
			
			if (counter % 1000 == 0) {
				log.info("link #" + counter);
			}
			counter++;
		}
		
		write(nceDirectory + "averageCapacity.csv", linkId2timeBin2capacityStatistics);
	}

	private void analyzeNCEFiles() {
		
		log.info("#### Preprocessing all network change event files in directory " + this.nceDirectory + "...");
		
		File[] fileList = new File(nceDirectory).listFiles();
		
		if (fileList.length == 0) {
			throw new RuntimeException("No file in " + this.nceDirectory + ". Aborting...");
		}
				
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
				int counter = 0;
				for (Id<Link> id : scenario.getNetwork().getLinks().keySet()) {
										
					Map<Integer, TimeVariantLinkInfo> timeBin2info = new HashMap<>();
					
					for (double time = timeBinSize; time <= 24 * 3600; time = time + timeBinSize) {
						TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(id);
						TimeVariantLinkInfo info = new TimeVariantLinkInfo(link.getFlowCapacityPerSec(time) * 3600., link.getNumberOfLanes(time), link.getFreespeed(time));
						timeBin2info.put((int) time, info);
					}					
					
					DayInfo dayInfo = new DayInfo(dateString, timeBin2info);
					
					if (this.linkId2dayInfo.containsKey(id)) {
						this.linkId2dayInfo.get(id).add(dayInfo);
					} else {
						List<DayInfo> dayInfos = new ArrayList<>();
						dayInfos.add(dayInfo);
						this.linkId2dayInfo.put(id, dayInfos);
					}
					if (counter % 1000 == 0) {
						log.info("link #" + counter);
					}
					counter++;
				}
			}
		}
		log.info("#### Preprocessing all network change event files in directory " + this.nceDirectory + "... Done.");
	}
}

