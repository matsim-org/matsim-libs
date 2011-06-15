/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusFootballSubPopAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.cottbus.DgCottbusSubPopAverageTravelTimeHandler;

/**
 * @author dgrether
 *
 */
public class DgCottbusFootballSubPopAnalysis {
	
	private static final Logger log = Logger.getLogger(DgCottbusFootballSubPopAnalysis.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		String runId = "1222";
		String runDirName = DgPaths.REPOS + "runs-svn/run" + runId;
		
		Map<Integer, Map<Integer, Double>> fbAvgTTMap = new HashMap<Integer, Map<Integer, Double>>();
		Map<Integer, Map<Integer, Double>> commuterAvgTTMap = new HashMap<Integer, Map<Integer, Double>>();
		
		File runDir = new File(runDirName);
		for (File fbdir : runDir.listFiles()){
			if (fbdir.isDirectory() && fbdir.getName().contains("football_fans")){
				int scale = Integer.parseInt(fbdir.getName().split("_")[0]);
				log.debug("found fbdir " + fbdir.getName());
				File itersDir = new File(fbdir.getPath() + File.separator + "ITERS");
				log.debug("itersDir " + itersDir);
				for (File itDir : itersDir.listFiles()){
					if (itDir.isDirectory() && itDir.getName().contains("it")){
						log.debug("itDir " + itDir.getName());
						int iteration = Integer.parseInt(itDir.getName().split("\\.")[1]);
						for (File itFile : itDir.listFiles()){
							if (itFile.getName().startsWith(runId + "_" + scale + "_football_fans." + iteration + ".events.xml")){
								log.info("found events file " + itFile.getName());
								
								DgCottbusSubPopAverageTravelTimeHandler avgtthandler = new DgCottbusSubPopAverageTravelTimeHandler();
								EventsManager events = EventsUtils.createEventsManager();
								events.addHandler(avgtthandler);
								
								MatsimEventsReader eventsReader = new MatsimEventsReader(events);
								eventsReader.readFile(itFile.getAbsolutePath());
								if (! fbAvgTTMap.containsKey(scale)){
									fbAvgTTMap.put(scale, new HashMap<Integer, Double>());
								}
								double avgtt = avgtthandler.getFootballAvgTT();
								log.debug("    avg tt for football is " + avgtt);
								fbAvgTTMap.get(scale).put(iteration, avgtt);
								
								if (! commuterAvgTTMap.containsKey(scale)){
									commuterAvgTTMap.put(scale, new HashMap<Integer, Double>());
								}
								avgtt = avgtthandler.getCommuterAvgTT();
								log.debug("    avg tt for commuter is " + avgtt);
								commuterAvgTTMap.get(scale).put(iteration, avgtt);
							}
						}
					}
				}
			}
		}
		
		writeTTMap(fbAvgTTMap, runDirName + "/" + runId + ".average_travel_times_football.txt");
		writeTTMap(commuterAvgTTMap, runDirName + "/" + runId + ".average_travel_times_commuter.txt");
		
		
	}

	private static void writeTTMap(Map<Integer, Map<Integer, Double>> map, String outfile) throws Exception{
		List<Integer> scaleList = new ArrayList<Integer>();
		scaleList.addAll(map.keySet());
		Collections.sort(scaleList);
		List<Integer> itList = new ArrayList<Integer>();
		Set<Integer> its = map.get(scaleList.get(0)).keySet();
		itList.addAll(its);
		Collections.sort(itList);
		String header = "scale";
		for (Integer i : itList){
			header = header + "\t iteration " + Integer.toString(i);
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(outfile);
		writer.write(header);
		writer.newLine();
		for (Integer scale : scaleList){
			writer.write(Integer.toString(scale));
			for (Integer i : itList){
				double tt = map.get(scale).get(i);
				if (Double.isNaN(tt)){
					writer.write("\t");
				}
				writer.write("\t " + Double.toString(tt));
			}
			writer.newLine();
		}
		writer.close();
	}
	
	
	
	
	
	
	
	
	
	
	
}
