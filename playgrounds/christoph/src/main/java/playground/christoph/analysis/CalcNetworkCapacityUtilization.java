/* *********************************************************************** *
 * project: org.matsim.*
 * CalcNetworkCapacityUtilization.java
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

package playground.christoph.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class CalcNetworkCapacityUtilization {

	private static final Logger log = Logger.getLogger(CalcNetworkCapacityUtilization.class);
	
	private final int hours = 24;
	private final double sampleSize = 0.1;	// a 10% population sample was used for the runs
	
	private final Scenario scenario;
	private final boolean analyzeAllLinks;
	private final Set<Id<Link>> analyzedLinks;
	private VolumesAnalyzer volumesAnalyzer;
	
	public static void main(String[] args) throws IOException {

		String path = "/data/matsim/cdobler/sandbox01/CostNavigationRouter/";
		
		// runs I
//		String[] pcts = new String[]{"0", "10", "30", "50", "70", "90", "100"};
//		for (String pct : pcts) {
//			String fullPath;
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#01_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_32.5_tau_05/";
//			runForPath(fullPath);
//						
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#02_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_32.5_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#03_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_32.5_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#04_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_25.5_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#05_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_40.0_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#06_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_32.5_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#07_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_32.5_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#08_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_25.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#09_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_25.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#10_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_32.5_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#11_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_32.5_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#12_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_40.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#13_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_40.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#14_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_25.0_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#15_" + pct + "pct_gamma0_0.50_tauplus_10.0_tauminus_40.0_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#16_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_25.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#17_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_25.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#18_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_40.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#19_" + pct + "pct_gamma0_0.50_tauplus_17.5_tauminus_40.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#20_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_25.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#21_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_25.0_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#22_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_25.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#23_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_40.0_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#24_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_40.0_tau_10/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#25_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_40.0_tau_05/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#26_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_32.5_tau_00/";
//			runForPath(fullPath);
//			
//			fullPath = path + "Runs_I/" + pct + "pct/output_zh_#27_" + pct + "pct_gamma0_0.50_tauplus_25.0_tauminus_32.5_tau_10/";		
//			runForPath(fullPath);
//		}
				
		// runs III
//		String[] pcts2 = new String[]{"0.00", "0.25", "0.50", "0.75", "1.00"};
//		for (String pct2 : pcts2) {
//			String[] pcts = new String[]{"0", "10", "30", "50", "70", "90", "100"};
//			for (String pct : pcts) {
//				String fullPath;
//				
//				fullPath = path + "Runs_III/dyn-dyn-gamma0_" + pct2 + "/output_zh_#09_" + pct + "pct_gamma0_" + pct2 + "_tauplus_10.0_tauminus_25.0_tau_10/";
//				runForPath(fullPath);
//				
//				fullPath = path + "Runs_III/dyn-dyn-gamma0_" + pct2 + "/output_zh_#23_" + pct + "pct_gamma0_" + pct2 + "_tauplus_25.0_tauminus_40.0_tau_00/";
//				runForPath(fullPath);
//			}
//		}
		
		// runs IV
		String[] pcts2 = new String[]{"0.00", "0.25", "0.50", "0.75", "0.80", "0.85", "0.90", "0.95", "1.00"};
		for (String pct2 : pcts2) {
			String[] pcts = new String[]{"0", "10", "30", "50", "70", "90", "100"};
			for (String pct : pcts) {
				String fullPath = path + "Runs_IV/gamma0_" + pct2 + "/output_zh_#23_" + pct + "pct_gamma0_" + pct2 + "/";
				runForPath(fullPath);
			}
		}
	}
	
	private static void runForPath(String path) throws IOException {
		
		Config config = ConfigUtils.loadConfig("/data/matsim/cdobler/sandbox01/CostNavigationRouter/input_zh/config.xml");
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		double radius = 15000.0;
		Coord center = new Coord(683518.0, 246836.0);
		Set<Id<Link>> linkIds = new TreeSet<Id<Link>>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double distance = CoordUtils.calcDistance(center, link.getCoord());
			if (distance <= radius) linkIds.add(link.getId());
		}
		log.info("Links to analyze: " + linkIds.size());
		
		CalcNetworkCapacityUtilization calcNetworkCapacityUtilization = new CalcNetworkCapacityUtilization(scenario, linkIds);
		calcNetworkCapacityUtilization.parseEventsFile(path + "/ITERS/it.0/1.0.events.xml.gz", path + "/NetworkCapacityUtilization.txt");	
	}
	
	public CalcNetworkCapacityUtilization(Scenario scenario, Set<Id<Link>> analyzedLinks) {
		
		this.scenario = scenario;
		this.analyzedLinks = analyzedLinks;
		this.analyzeAllLinks = false;
		
		init(scenario);
	}
	
	public CalcNetworkCapacityUtilization(Scenario scenario) {
		
		this.scenario = scenario;
		this.analyzedLinks = null;
		this.analyzeAllLinks = true;
		
		init(scenario);
	}
	
	private void init(Scenario scenario) {
		
		int timeSlice = 900;
		int maxTime = hours * 3600;
		this.volumesAnalyzer = new VolumesAnalyzer(timeSlice, maxTime, scenario.getNetwork());
	}
	
	public void parseEventsFile(String eventsFile, String outputFile) throws IOException {
		
		double capacityPeriod = scenario.getNetwork().getCapacityPeriod();
		double capacityPeriodFactor = capacityPeriod / 3600.0;
		
		BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(outputFile);
		
		volumesAnalyzer.reset(0);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(volumesAnalyzer);
		
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		Set<Id<Link>> linkIds = null;
		if (analyzeAllLinks) {
			linkIds = scenario.getNetwork().getLinks().keySet();
		} else linkIds = this.analyzedLinks;
		
		double sumAvgCarUtilization = 0.0;
		for (Id linkId : linkIds) {
			double[] carVolumes = this.volumesAnalyzer.getVolumesPerHourForLink(linkId, TransportMode.car);
			
			Link link = scenario.getNetwork().getLinks().get(linkId);
			double capacity = link.getCapacity();
			
			double totalCarVolume = 0.0;
			if (carVolumes != null) {
				for (double carVolume : carVolumes) totalCarVolume += carVolume;				
			}
			
			// scale volumes to a 100% population sample
			totalCarVolume *= 1 / sampleSize;
			
			// calculate capacity per hour
			double hourlyCapacity = capacity / capacityPeriodFactor;
			
			double avgCarUtilization = totalCarVolume / (hourlyCapacity * hours);
			sumAvgCarUtilization += avgCarUtilization;
			
			String string = "Average utilization of link " + linkId.toString() + ":\t" + String.format("%.10f", avgCarUtilization); 
			log.info(string);
			bufferedWriter.write(string);
			bufferedWriter.newLine();
		}
		String string = "Average utilization of network:\t" + String.format("%.10f", sumAvgCarUtilization/linkIds.size()); 
		log.info(string);
		bufferedWriter.write(string);
		bufferedWriter.newLine();
		
		bufferedWriter.flush();
		bufferedWriter.close();
	}
}
