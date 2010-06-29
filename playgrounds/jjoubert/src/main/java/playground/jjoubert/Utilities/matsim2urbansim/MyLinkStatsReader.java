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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

public class MyLinkStatsReader {
	private final Logger log = Logger.getLogger(MyLinkStatsReader.class);
	private File file;
	private String hours;
	private Map<Id, Double> travelTimes;
	
	public MyLinkStatsReader(String filename, String hours) {
		File f = new File(filename);
		if(!f.exists()){
			throw new RuntimeException("The link stats file " + f.getAbsolutePath() + " does not exist");
		}
		this.file = f;
		this.hours = hours;
		travelTimes = new TreeMap<Id, Double>();
	}
	
	public void readLinkStatsTravelTime(){
		log.info("Reading link statistics from " + this.file.getAbsolutePath());
		Integer index = null;
		BufferedReader input;
		int linkCounter = 0;
		int linkMultiplier = 1;
		try {
			input = IOUtils.getBufferedReader(file.getAbsolutePath());
//			input = new Scanner(new BufferedReader(new FileReader(this.file)));
			try{
				String[] header = input.readLine().split("\t");
				/*
				 * First find the column index in which the appropriate travel time 
				 * occurs.
				 */
				boolean found = false;
				int i = 0;
				String s = "TRAVELTIME" + this.hours + "avg";
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
	}
	
	public Map<Id, Double> getTravelTimeMap(){
		return this.travelTimes;
	}
	
	public String toString(){
		return "MyLinkStatsReader for hours " + hours + " from " + file.getAbsolutePath();
	}

}

