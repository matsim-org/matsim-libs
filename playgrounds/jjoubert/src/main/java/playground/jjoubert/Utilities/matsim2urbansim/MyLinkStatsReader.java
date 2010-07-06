/* *********************************************************************** *
 * project: org.matsim.*
 * MyLinkStatsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.trafficmonitoring.TravelTimeData;
import org.matsim.core.trafficmonitoring.TravelTimeDataArray;
import org.matsim.core.utils.io.IOUtils;

public class MyLinkStatsReader {
	private final Logger log = Logger.getLogger(MyLinkStatsReader.class);
	private File file;
	
	public MyLinkStatsReader(String filename) {
		File f = new File(filename);
		if(!f.exists()){
			throw new RuntimeException("The link stats file " + f.getAbsolutePath() + " does not exist");
		}
		this.file = f;
	}
	
	/**
	 * Reads the average travel time from the LinkStats file.
	 * @param hour for which the travel time is read.
	 * @return
	 */
	public Map<Id, Double> readSingleHour(final String hour){
		log.info("Reading link statistics for hour " + hour + " from " + this.file.getAbsolutePath());
		
		Map<Id,Double> travelTimes = new TreeMap<Id, Double>();
		Integer index = null;
		BufferedReader input;
		int linkCounter = 0;
		int linkMultiplier = 1;
		try {
			input = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				String[] header = input.readLine().split("\t");
				/*
				 * First find the column index in which the appropriate travel time 
				 * occurs.
				 */
				boolean found = false;
				int i = 0;
				String s = "TRAVELTIME" + hour + "avg";
				while(!found && i < header.length){
					if(header[i].equalsIgnoreCase(s)){
						index = i;
						found = true;
					} else{
						i++;
					}					
				} 
				if(index == null){
					throw new RuntimeException("Could not find " + s + " in " + this.file.getAbsolutePath());
				}
				
				/* 
				 * Now process the links, adding each to a map.
				 */
				String theLine= null; 
				while( (theLine = input.readLine()) != null ){
					String line[] = theLine.split("\t");
					if(line.length == header.length){
						travelTimes.put(new IdImpl(line[0]), Double.parseDouble(line[index]));
					}
					
					// Report progress.
					if(++linkCounter == linkMultiplier){
						log.info("   Links processed: " + linkCounter);
						linkMultiplier *= 2;
					}
				}
			} finally{
				input.close();
			}
			log.info("   Links processed: " + linkCounter + " (Done)");		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return travelTimes;
	}
	
	
	/**
	 * Reads and processes the entire linkstats file and create new 
	 * TravelTimeData objects, one for each link.
	 * @param s the scenario
	 * @param stat the summary statistic to use. Acceptable values are "min", 
	 * 		"avg" and "max".
	 */
	public Map<Id, TravelTimeData> buildTravelTimeDataObject(Scenario s, String stat) {
		log.info("Creating TravelTimeData map using " + stat + " travel times from " + file.getAbsolutePath());
		
		Map<Id, TravelTimeData> ttm = new TreeMap<Id, TravelTimeData>();
		Integer index = null;
		int linkCounter = 0;
		int linkMultiplier = 1;
		try {
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				String [] header = br.readLine().split("\t");
				boolean found = false;
				int i = 0;
				String string = "TRAVELTIME0-1" + stat;
				do {
					if(header[i].equalsIgnoreCase(string)){
						found = true;
						index = i;
					} else{
						i++;					
					}
				} while (found == false && i < header.length);
				if(!found){
					throw new RuntimeException("Could not find and index for " + s + " in " + file.getAbsolutePath());
				}
				String line = null;
				while((line = br.readLine()) != null){
					String[] values = line.split("\t");
					Id linkId = new IdImpl(values[0]);
					Link l = s.getNetwork().getLinks().get(linkId);
					TravelTimeData ttd = new TravelTimeDataArray(l, 24);
					try{
						for(i = 0; i < 24; i++){
							ttd.addTravelTime(i, Double.parseDouble(values[index+(i*3)]));
						}	
						ttm.put(l.getId(), ttd);
					} catch (IndexOutOfBoundsException e){
						log.warn("Could not get the travel time for link " + l.getId() + " for hour " + i);
						e.printStackTrace();
					}
					
					// Report progress.
					if(++linkCounter == linkMultiplier){
						log.info("   Links processed: " + linkCounter);
						linkMultiplier *= 2;
					}
				}
			} finally{
					br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ttm;
	}
	
	public File getFile(){
		return this.file;
	}
}

